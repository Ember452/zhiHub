package com.solis.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration //告诉Spring这是一个配置类，Spring会扫描并加载其中的Bean方法
@EnableWebSecurity //启用Spring Security的Web安全功能，允许我们自定义安全配置
@EnableMethodSecurity //启用方法级安全，允许我们在方法上使用注解来控制访问权限@PreAuthorize("hasRole('ADMIN')")
public class SecurityConfig {
    /**
     * 配置 Spring Security 过滤链。
     *
     * <p>主要包含：</p>
     * - 关闭 CSRF；
     * - 启用 CORS；
     * - 使用无状态会话策略；
     * - 公开认证接口与健康检查，其余接口需鉴权；
     * - 启用资源服务器的 JWT 校验。
     *
     * @param http Spring 的 {@link HttpSecurity} 构建器。
     * @return 构建完成的 {@link SecurityFilterChain}。
     * @throws Exception 构建过滤链过程中可能抛出的异常。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                //AbstractHttpConfigurer 是 Spring Security 提供的一个 “配置器父类”
                //spring security各种功能的通用配置容器
                //csrf跨站请求伪造，后端不用Session的话要关闭，用JWT
                .csrf(AbstractHttpConfigurer::disable)
                //启用跨域资源共享，允许前端应用访问后端API
                .cors(Customizer.withDefaults())
                //jwt每次自带身份，不用服务端会话，适合分布式，微服务，集群
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                //给接口设置谁能访问
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // 公开内容：首页 Feed 不需要登录
                        .requestMatchers("/api/v1/knowposts/feed").permitAll()
                        // 知文详情（公开已发布内容，非公开由服务层校验）
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/knowposts/detail/*").permitAll()
                        // 知文详情页 RAG 问答（SSE 流式输出）允许匿名访问
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/knowposts/*/qa/stream").permitAll()
                        .requestMatchers(
                                "/api/v1/auth/send-code",
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/token/refresh",
                                "/api/v1/auth/logout",
                                "/api/v1/auth/password/reset"

                ).permitAll()
                .anyRequest().authenticated()
                )
                //OAuth2 资源服务器 = 专门负责「校验 JWT 令牌、保护接口」的后端服务
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
        return http.build();
    }

    /**
     * 定义并提供 CORS 配置源。
     * 允许前端跨域访问后端接口的配置
     * <p>当前允许所有来源（后续建议替换为产品白名单），允许常见方法与请求头，且不携带凭证。</p>
     *
     * @return {@link CorsConfigurationSource}，用于为所有路径注册 CORS 规则。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(){
        CorsConfiguration config = new CorsConfiguration();
        //允许所有来源，生产环境建议替换为前端应用的白名单
        config.setAllowedHeaders(List.of("Authorization","Cache-Control","Content-Type"));
        config.setAllowedOrigins(List.of("*")); //TODO 生产环境替换为前端应用的白名单
        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        // 使用JWT不需要携带凭证（如Cookie），且允许所有来源时必须设置为false
        config.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        //为所有路径注册 CORS 配置
        source.registerCorsConfiguration("/**",config);
        return source;
    }
}
