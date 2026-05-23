package com.solis.relation.processor;

import com.solis.counter.service.UserCounterService;
import com.solis.relation.event.RelationEvent;
import com.solis.relation.mapper.RelationMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 注意Canal外围的type：操作类型
 * payload里的type：表示业务事件类型
 * 关系事件处理器。
 * 职责：对 FollowCreated/FollowCanceled 事件进行去重、防抖与幂等处理，落库更新粉丝表，维护关注/粉丝 ZSet 缓存与 TTL，并原子更新用户维度计数（SDS）。
 */
@Service
public class RelationEventProcessor {
    private final RelationMapper mapper;
    private final StringRedisTemplate redis;
    private final UserCounterService userCounterService;

    //构造器注入，对象不能修改，安全，可以保证依赖一定存在
    public RelationEventProcessor(RelationMapper mapper, StringRedisTemplate redis, UserCounterService userCounterService) {
        this.mapper = mapper;
        this.redis = redis;
        this.userCounterService = userCounterService;
    }

    /**
     * 处理关系事件：入库、更新缓存、刷新计数，并进行幂等去重。
     * kafka可能会把一个消息发送多次---网络抖动，消费者重启，重平衡，重试机制
     * @param evt 关系事件
     * now:给ZSet当分数排序用
     * 每次关注都会刷新过期时间，那为什么还要设置 2 小时过期？为了让不活跃用户缓存自动消失，释放内存
     * 解决用户在10分钟内取关，关注问题：加evt.type()
     */
    public void process(RelationEvent evt) {
        String dk = "dedup:rel:" + evt.type() + ":" + evt.fromUserId() + ":" + evt.toUserId() + ":" + (evt.id() == null ? "0" : String.valueOf(evt.id()));
        Boolean first = redis.opsForValue().setIfAbsent(dk, "1", Duration.ofMinutes(10));

        // 非首次（存在去重键）直接返回，保证消息幂等，防止网络波动，消费者重启，重平衡，重试机制
        if (first == null || !first) {
            return;
        }
        if ("FollowCreated".equals(evt.type())) {
            // 异步插入粉丝表
            mapper.insertFollower(evt.id(), evt.toUserId(), evt.fromUserId(), 1);
            long now = System.currentTimeMillis();

            // 更新关注表与粉丝表缓存：ZSet 按时间分数维护最近项，设置短 TTL 减少陈旧数据
            redis.opsForZSet().add("uf:flws:" + evt.fromUserId(), String.valueOf(evt.toUserId()), now);
            redis.opsForZSet().add("uf:fans:" + evt.toUserId(), String.valueOf(evt.fromUserId()), now);
            //使用redis.expire设置缓存过期时间
            redis.expire("uf:flws:" + evt.fromUserId(), Duration.ofHours(2));
            redis.expire("uf:fans:" + evt.toUserId(), Duration.ofHours(2));

            // 更新关注数与粉丝数
            userCounterService.incrementFollowings(evt.fromUserId(), 1);
            userCounterService.incrementFollowers(evt.toUserId(), 1);
        } else if ("FollowCanceled".equals(evt.type())) {
            mapper.cancelFollower(evt.toUserId(), evt.fromUserId());

            // 更新关注表与粉丝表缓存：移除 ZSet 项并刷新 TTL
            redis.opsForZSet().remove("uf:flws:" + evt.fromUserId(), String.valueOf(evt.toUserId()));
            redis.opsForZSet().remove("uf:fans:" + evt.toUserId(), String.valueOf(evt.fromUserId()));
            redis.expire("uf:flws:" + evt.fromUserId(), Duration.ofHours(2));
            redis.expire("uf:fans:" + evt.toUserId(), Duration.ofHours(2));

            // 更新关注数与粉丝数
            userCounterService.incrementFollowings(evt.fromUserId(), -1);
            userCounterService.incrementFollowers(evt.toUserId(), -1);
        }
    }
}
