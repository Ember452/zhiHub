package com.solis.search.index;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solis.counter.service.CounterService;
import com.solis.knowpost.mapper.KnowPostMapper;
import com.solis.knowpost.model.KnowPostDetailRow;
import com.solis.knowpost.model.KnowPostFeedRow;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 搜索索引写入服务：负责 upsert/软删 以及首次启动的索引回灌。
 */
@Service
@RequiredArgsConstructor
public class SearchIndexService {
    //创建一个日志打印类，归属的索引是SearchIndexService
    private static final Logger log = LoggerFactory.getLogger(SearchIndexService.class);
    private static final String INDEX = "zhihub_content_index";

    private final ElasticsearchClient es;
    private final KnowPostMapper knowPostMapper;
    private final CounterService counterService;
    private final ObjectMapper objectMapper;
    //创建一个http请求工具，可以调用别人的接口，get，post都行，发送网络请求
    private final RestTemplate http = new RestTemplate();

    /**
     * 启动时若索引为空，进行历史数据回灌（分页）。
     */
    @PostConstruct
    public void ensureBackfill() {
        try {
            long cnt = es.count(c -> c.index(INDEX)).count();
            if (cnt > 0) return;
            int limit = 500;
            int offset = 0;
            while (true) {
                //KnowPostFeedRow:数据库层面的原始数据，没有点赞等数据
                List<KnowPostFeedRow> rows = knowPostMapper.listFeedPublic(limit, offset);
                if (rows == null || rows.isEmpty()) {
                    // 没有更多数据，结束回灌
                    break;
                }
                for (KnowPostFeedRow r : rows) {
                    upsertKnowPost(r.getId());
                }
                offset += rows.size();
            }
            log.info("Search index backfill completed: {} documents", es.count(c -> c.index(INDEX)).count());
        } catch (Exception e) {
            log.warn("Search index backfill skipped: {}", e.getMessage());
        }
    }

    /**
     * upsert 内容文档：写入基础字段、计数与补全。使用 wait_for 刷新以保障“立即可搜”。
     */
    public void upsertKnowPost(long id) {
        try {
            KnowPostDetailRow row = knowPostMapper.findDetailById(id);
            if (row == null) {
                log.warn("Index upsert skipped: post {} not found", id);
                return;
            }
            Map<String, Object> doc = new HashMap<>();
            doc.put("content_id", row.getId());
            doc.put("content_type", row.getType());
            doc.put("title", row.getTitle());
            doc.put("description", row.getDescription());
            doc.put("author_id", row.getCreatorId());
            doc.put("author_avatar", row.getAuthorAvatar());
            doc.put("author_nickname", row.getAuthorNickname());
            doc.put("author_tag_json", row.getAuthorTagJson());
            if (row.getPublishTime() != null) {
                doc.put("publish_time", row.getPublishTime().toEpochMilli());
            }
            doc.put("status", row.getStatus());
            doc.put("tags", parseStringArray(row.getTags()));
            doc.put("img_urls", parseStringArray(row.getImgUrls()));
            if (row.getIsTop() != null) {
                doc.put("is_top", row.getIsTop());
            }

            // 正文优先拉取 contentUrl，失败则使用描述
            String body = fetchContentSafe(row.getContentUrl());
            if (body == null || body.isBlank()) {
                body = row.getDescription();
            }
            if (body != null) {
                doc.put("body", truncate(body, 4000));
            }

            Map<String, Long> counts = counterService.getCounts("knowpost", String.valueOf(id), List.of("like","fav"));
            doc.put("like_count", counts.getOrDefault("like", 0L));
            doc.put("favorite_count", counts.getOrDefault("fav", 0L));
            doc.put("view_count", 0L);

            if (row.getTitle() != null && !row.getTitle().isBlank()) {
                doc.put("title_suggest", row.getTitle());
            }

            // 刷新策略：wait_for，保证写入后即刻可检索
            IndexRequest<Map<String, Object>> req = IndexRequest.of(b -> b
                    .index(INDEX)
                    .id(String.valueOf(id))
                    .document(doc)
                    .refresh(Refresh.WaitFor)
            );
            IndexResponse resp = es.index(req);
            log.info("Indexed post {} result={} version={}", id, resp.result(), resp.version());
        } catch (Exception e) {
            log.error("Index upsert failed for post {}: {}", id, e.getMessage());
        }
    }

    /**
     * 软删内容：仅更新 status=deleted，同一文档 ID 覆盖写入。
     */
    public void softDeleteKnowPost(long id) {
        try {
            Map<String, Object> doc = new HashMap<>();
            doc.put("content_id", id);
            doc.put("status", "deleted");
            IndexRequest<Map<String, Object>> req = IndexRequest.of(b -> b
                    .index(INDEX)
                    .id(String.valueOf(id))
                    .document(doc)
                    .refresh(Refresh.WaitFor)
            );
            es.index(req);
        } catch (Exception e) {
            log.error("Index soft delete failed for post {}: {}", id, e.getMessage());
        }
    }

    /**
     * 安全拉取正文内容：失败返回 null，不中断索引流程。
     */
    private String fetchContentSafe(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            //告诉编译器，我能接收文本，HTML，JSON
            headers.setAccept(List.of(MediaType.TEXT_HTML, MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON));
            //发送get请求把数据通过字节数组获取到,整个http响应
            ResponseEntity<byte[]> resp = http.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
            //从ResponseEntity中获取响应的字节数据
            byte[] bytes = resp.getBody();
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            //有些接口只在响应头带编码，有些只在 HTML 里写 charset，
            //从响应头中获取编码格式
            MediaType contentType = resp.getHeaders().getContentType();
            Charset headerCharset = (contentType != null) ? contentType.getCharset() : null;
            //读取 HTML 里的 <meta charset="UTF-8"> 标签。
            //因为 HTTP 响应头里的编码，经常不准、甚至没有！所以必须去 HTML 内容里读 <meta charset="UTF-8"> 才最靠谱
            Charset metaCharset = sniffHtmlCharset(bytes);
            //自动选择最正确的编码格式。
            Charset charset = pickCharset(bytes, headerCharset, metaCharset);
            //把字节数组 → 用正确编码 → 转成字符串（正文内容）。
            return new String(bytes, charset);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 选择正确的编码
     * @param bytes 字节数组
     * @param headerCharset  头
     * @param metaCharset  HTML
     * @return 正确的编码
     */
    private Charset pickCharset(byte[] bytes, Charset headerCharset, Charset metaCharset) {
        if (metaCharset != null) {
            return metaCharset;
        }
        //处理缺少header
        if (headerCharset == null) {
            Charset utf8 = StandardCharsets.UTF_8;
            Charset gb18030 = Charset.forName("GB18030");
            return countReplacementChars(new String(bytes, utf8)) <= countReplacementChars(new String(bytes, gb18030)) ? utf8 : gb18030;
        }
        //纠正错误的编码，比如老旧头里携带的错误编码
        if (isLikelyWrongCharsetHeader(headerCharset)) {
            Charset utf8 = StandardCharsets.UTF_8;
            Charset gb18030 = Charset.forName("GB18030");
            int repUtf8 = countReplacementChars(new String(bytes, utf8));
            int repGb = countReplacementChars(new String(bytes, gb18030));
            int repHeader = countReplacementChars(new String(bytes, headerCharset));
            if (repUtf8 <= repGb && repUtf8 <= repHeader) return utf8;
            if (repGb <= repHeader) return gb18030;
        }
        return headerCharset;
    }

    /**
     * 判断编码是否错误
     * @param charset   编码
     * @return 是否错误
     */
    private boolean isLikelyWrongCharsetHeader(Charset charset) {
        return StandardCharsets.ISO_8859_1.equals(charset) || StandardCharsets.US_ASCII.equals(charset);
    }

    /**
     * 从 HTML 网页的字节数据里，智能嗅探它声明的编码格式（UTF-8/GBK 等），用来解决中文乱码问题
     * @param bytes 字节数据
     * @return 编码
     */
    private Charset sniffHtmlCharset(byte[] bytes) {
        //只读取前面一小段
        int limit = Math.min(bytes.length, 8192);
        // 用 ISO-8859-1 把字节转成字符串（最关键一步）,不会破坏字节的编码，它把每个字节直接映射成一个字符。
        String head = new String(bytes, 0, limit, StandardCharsets.ISO_8859_1);

        Matcher m = Pattern.compile("charset\\s*=\\s*['\\\"]?([a-zA-Z0-9_\\-]+)", Pattern.CASE_INSENSITIVE).matcher(head);
        if (!m.find()) {
            return null;
        }
        String cs = m.group(1);
        if (cs == null || cs.isBlank()) {
            return null;
        }

        //去掉字符串首尾所有空白，strip()更安全
        cs = cs.trim();
        if ("utf8".equalsIgnoreCase(cs)) {
            return StandardCharsets.UTF_8;
        }
        if ("gbk".equalsIgnoreCase(cs) || "gb2312".equalsIgnoreCase(cs) || "gb18030".equalsIgnoreCase(cs)) {
            return Charset.forName("GB18030");
        }
        try {
            return Charset.forName(cs);
        } catch (Exception e) {
            return null;
        }
    }

    //统计不同的字符串编码中出现的乱码的个数。乱码：\uFFFD
    private int countReplacementChars(String s) {
        if (s == null || s.isEmpty()) return 0;
        int cnt = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\uFFFD') cnt++;
        }
        return cnt;
    }

    /**
     * 截断字符串到最大长度。
     */
    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }

        return s.length() <= max ? s : s.substring(0, max);
    }

    /**
     * 将 JSON 数组字符串解析为 List<String>；异常返回空列表。
     */
    private List<String> parseStringArray(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
