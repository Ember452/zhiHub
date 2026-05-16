package com.solis.auth.model;

/**
 * 登录标识，记录用户是用什么登录方式登录的
 */
public enum IdentifierType {
    PHONE,
    EMAIL;

    public static IdentifierType fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("identifier type required");
        }
        //调用toLowerCase()方法，把字符串转换成小写
        return switch (value.toLowerCase()) {
            case "phone", "mobile" -> PHONE;
            case "email" -> EMAIL;
            default -> throw new IllegalArgumentException("Unsupported identifier type: " + value);
        };
    }
}
