package com.solis.storage.api.dto;

import java.util.Map;

/**
 * 预签名直传响应。
 */
public record StoragePresignResponse(
        String objectKey, //存储文件的唯一路径
        String putUrl, //上传文件的预签名URL
        Map<String, String> headers, //上传需要的请求头
        int expiresIn //过期时间
) {}