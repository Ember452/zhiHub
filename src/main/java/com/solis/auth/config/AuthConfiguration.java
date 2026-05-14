package com.solis.auth.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * 认证相关 Bean 配置。
 * <p>
 * - `PasswordEncoder`：根据配置的 BCrypt 强度创建；
 * - `JwtEncoder/Decoder`：读取配置中的 RSA 私钥/公钥并构造 Nimbus 实现；
 * - JWK 使用 `keyId` 标识，供下游验证与密钥轮换。
 */
@Configuration
@EnableConfigurationProperties(AuthProperties.class)
@RequiredArgsConstructor
public class AuthConfiguration {

    private final AuthProperties properties;

    /**
     * 创建密码编码器（BCrypt）。
     *
     * @return 使用配置的强度构造的 {@link PasswordEncoder}。
     * BCryptPasswordEncoder Spring最安全最常用的加密算法，自动加盐，强度可配置，适合存储用户密码。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // 密码加密工具基于BCrypt算法，获取加密强度
        return new BCryptPasswordEncoder(properties.getPassword().getBcryptStrength());
    }

    /**
     * 创建 JWT 编码器。
     *
     * <p>读取 RSA 私钥/公钥并构造 JWK，使用 Nimbus 实现生成 {@link JwtEncoder}。</p>
     *
     * @return 基于 RSA JWK 的 {@link JwtEncoder}。
     */
    @Bean
    public JwtEncoder jwtEncoder() {
        AuthProperties.Jwt jwtProps = properties.getJwt();
        RSAPrivateKey privateKey = PemUtils.readPrivateKey(jwtProps.getPrivateKey());
        RSAPublicKey publicKey = PemUtils.readPublicKey(jwtProps.getPublicKey());
        //构建jwk，封装秘钥和keyID
        RSAKey jwk = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(jwtProps.getKeyId())
                .build();
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(jwk));
        /*Nimbus jwt encoderSpring默认的JWT库
        * spring security就是靠这个来生成前面，签发验证Token的将公私钥转换jwk格式
        * */
        return new NimbusJwtEncoder(jwkSource);
    }

    /**
     * 创建 JWT 解码器。
     *
     * <p>读取 RSA 公钥并构造基于 Nimbus 的 {@link JwtDecoder}。</p>
     *
     * @return 基于 RSA 公钥的 {@link JwtDecoder}。
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        AuthProperties.Jwt jwtProps = properties.getJwt();
        RSAPublicKey publicKey = PemUtils.readPublicKey(jwtProps.getPublicKey());
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }
}
