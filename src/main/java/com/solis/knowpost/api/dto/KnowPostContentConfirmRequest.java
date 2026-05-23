package com.solis.knowpost.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 内容上传确认请求。
 */
public record KnowPostContentConfirmRequest(
        @NotBlank String objectKey,//文件在存储服务里的唯一标识
        @NotBlank String etag,//云存储（OSS/COS/S3）生成的文件标识
        @NotNull Long size,//文件大小（字节）
        @NotBlank String sha256//文件的 SHA256 哈希值用来校验文件内容是否被篡改、是否完整
) {}