package com.solis.storage.api;

import com.solis.storage.config.OssProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/api/proxy")
@RequiredArgsConstructor
public class ImageProxyController {

    private final OssProperties ossProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final Set<String> ALLOWED_PROTOCOLS = new HashSet<>(Arrays.asList("http", "https"));

    @GetMapping("/image")
    public ResponseEntity<byte[]> proxyImage(@RequestParam String url) {
        try {
            // 1. 校验 URL 格式
            if (url == null || url.isBlank()) {
                return ResponseEntity.badRequest().build();
            }

            URL parsedUrl = new URL(url);
            
            // 2. 校验协议
            if (!ALLOWED_PROTOCOLS.contains(parsedUrl.getProtocol().toLowerCase())) {
                return ResponseEntity.badRequest().build();
            }

            // 3. 校验是否为你的 OSS 域名
            String ossDomain = getOssDomain();
            if (!parsedUrl.getHost().equals(ossDomain)) {
                return ResponseEntity.badRequest().build();
            }

            // 4. 后端请求 OSS 图片
            ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);

            // 5. 设置响应头，返回图片
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(response.getHeaders().getContentType());
            headers.setCacheControl("public, max-age=86400"); // 缓存一天
            
            return new ResponseEntity<>(response.getBody(), headers, HttpStatus.OK);
            
        } catch (Exception e) {
            // 记录日志并返回错误
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String getOssDomain() {
        // 优先使用自定义域名，否则使用默认 OSS 域名
        if (ossProperties.getPublicDomain() != null && !ossProperties.getPublicDomain().isBlank()) {
            try {
                return new URL(ossProperties.getPublicDomain()).getHost();
            } catch (Exception e) {
                // 如果解析失败，尝试直接使用域名
                return ossProperties.getPublicDomain();
            }
        }
        
        // 默认 OSS 域名格式: bucket.endpoint
        return ossProperties.getBucket() + "." + ossProperties.getEndpoint();
    }
}
