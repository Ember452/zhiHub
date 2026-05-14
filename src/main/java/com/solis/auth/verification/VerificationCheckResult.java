package com.solis.auth.verification;

/**
 * 验证码校验结果。
 * <p>
 * 包含状态（成功/未找到/过期/错误/尝试过多）和次数统计信息，提供便捷成功判断。
 * attempts: 当前尝试次数
 * maxAttempts: 最大尝试次数
 */
public record VerificationCheckResult(
        VerificationCodeStatus status,
        int attempts,
        int maxAttempts
) {
    public boolean isSuccess() {
        return status == VerificationCodeStatus.SUCCESS;
    }
}

