package com.solis.storage.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 预签名直传请求。
 */
public record StoragePresignRequest(
        @NotBlank String scene, // knowpost_content | knowpost_image
        @NotBlank String postId, // 文章ID，字符串避免前端精度丢失
        @NotBlank String contentType, // 文件类型
        String ext  //文件后缀
) {}