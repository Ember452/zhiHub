package com.solis.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "spring.elasticsearch")
public class EsProperties {
    //支持多个ES节点
    private List<String> uris;

    //ES用户名
    private String username;
    //ES密码
    private String password;

    //配置RAG索引名，来自Spring AI的配置
    @Value("${spring.ai.vectorstore.elasticsearch.index-name:}")
    private String index;

    //返回第一个URI作为host
    public String getHost() {
        return(uris == null || uris.isEmpty()) ? null:uris.getFirst();
    }

}
