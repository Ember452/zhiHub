package com.solis.cache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


/**
 * 缓存相关配置项。
 *避免重复查库
 * 查本地caffeine缓存，提高接口响应速度
 * Caffeine 的原理：
 *  Caffeine 是目前 Java 最快的本地缓存，它的淘汰策略是：W-TinyLFU
 *  记录访问频率
 *  记录最近访问时间
 *  优先删除用得少 + 很久没访问的数据
 *  保留高频 + 最近访问的热点数据
 *
 * <p>配置前缀：{@code cache}，用于绑定 {@code application.yml} 中的缓存参数。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "cache")
public class CacheProperties {
    // 进程内缓存（本地二级缓存）配置。
    private L2 l2 = new L2();

    // 热点 Key 识别与扩展策略配置。
    private Hotkey hotkey = new Hotkey();

    @Data
    public static class L2 {
        // 公共信息流缓存配置。
        private PublicCfg publicCfg = new PublicCfg();

        // 个人信息流缓存配置。
        private MineCfg mineCfg = new MineCfg();

        // 知文详情缓存配置
        private DetailCfg detailCfg = new DetailCfg();
    }

    @Data
    public static class PublicCfg {
        // TTL（秒）：写入后在本地缓存中保留的时长。
        private int ttlSeconds = 15;

        // 最大条目数：超过后按 Caffeine 策略逐出。
        private long maxSize = 1000;
    }

    @Data
    public static class MineCfg {
        // TTL（秒）：写入后在本地缓存中保留的时长。
        private int ttlSeconds = 10;

        // 最大条目数：超过后按 Caffeine 策略逐出。
        private long maxSize = 1000;
    }

    @Data
    public static class DetailCfg {
        // TTL（秒）：写入后在本地缓存中保留的时长。
        private int ttlSeconds = 30;

        // 最大条目数：超过后按 Caffeine 策略逐出。
        private long maxSize = 5000;
    }
    /**
     * 业界标准的热点识别算法：滑动时间窗口（Sliding Window）+ 分片计数（Segment）
     *
     */
    @Data
    public static class Hotkey {
        // 热点统计窗口长度（秒）。
        private int windowSeconds = 60;

        // 统计窗口切片大小（秒），用于按段累计访问次数。
        private int segmentSeconds = 10;

        // 低热度阈值：窗口内访问次数达到该值视为低热。
        private int levelLow = 50;

        // 中热度阈值：窗口内访问次数达到该值视为中热。
        private int levelMedium = 200;

        // 高热度阈值：窗口内访问次数达到该值视为高热。
        private int levelHigh = 500;

        // 低热度额外延长 TTL（秒）。
        private int extendLowSeconds = 20;

        //中热度额外延长 TTL（秒）。
        private int extendMediumSeconds = 60;

        // 高热度额外延长 TTL（秒）。
        private int extendHighSeconds = 120;
    }
}
