package com.solis.counter.config;

//import org.apache.kafka.common.serialization.StringSerializer;
//import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.kafka.annotation.EnableKafka;
//import org.springframework.kafka.core.DefaultKafkaProducerFactory;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.kafka.core.ProducerFactory;
//import org.springframework.scheduling.annotation.EnableScheduling;
//
///**
// * 计数模块配置：启用调度与 Kafka，并提供字符串模板。
// */
//@Configuration
//@EnableScheduling // 启用 @Scheduled 定时任务（计数聚合刷写）
//@EnableKafka // 启用 Kafka（计数事件生产与消费）
//public class CounterConfig {
//
//    @Bean
//    public ProducerFactory<String, String> stringProducerFactory(KafkaProperties properties) {
//        var props = properties.buildProducerProperties();
//        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new StringSerializer()); // 统一字符串序列化
//    }
//
//    @Bean
//    public KafkaTemplate<String, String> stringKafkaTemplate(ProducerFactory<String, String> pf) {
//        return new KafkaTemplate<>(pf);
//    }
//}

import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 创建一个kafka字符串的生产模版
 */
@Configuration
@EnableKafka
@EnableScheduling
public class CounterConfig {

    // 注入 SslBundles（Spring Boot 自动配置提供）
    @Bean
    public ProducerFactory<String, String> stringProducerFactory(KafkaProperties properties, SslBundles sslBundles) {
        // 替换废弃方法，使用带 SslBundles 的重载
        var props = properties.buildProducerProperties(sslBundles);
        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * 创建一个kafka字符串的模版
     * 用上面方法返回值创建kafkaTemplate
     * @param pf 生产模版
     * @return kafka模版
     */
    @Bean
    public KafkaTemplate<String, String> stringKafkaTemplate(ProducerFactory<String, String> pf) {
        return new KafkaTemplate<>(pf);
    }
}