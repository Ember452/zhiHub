package com.solis.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RedissonConfig {
    @Value("${counter.rebuild.lock.watchdog-ms:300000}")
    private Long lockWatchdogMs;

    //后续要用分布式锁到要调用redissonClient，注入RedissonClient
    @Bean
    public RedissonClient redisClient(RedisProperties redisProperties){
        Config config = new Config();
        //配置Redisson看门狗超时
        config.setLockWatchdogTimeout(lockWatchdogMs);

        String address = "redis://" + redisProperties.getHost()+ ":" + redisProperties.getPort();
        SingleServerConfig singleServerConfig = config.useSingleServer().setAddress(address);

        //如果配置了密码，则设置密码
        if(redisProperties.getPassword() != null && !redisProperties.getPassword().isEmpty()){
            singleServerConfig.setPassword(redisProperties.getPassword());
        }

        singleServerConfig.setDatabase(redisProperties.getDatabase());
        return Redisson.create(config);
    }
}
