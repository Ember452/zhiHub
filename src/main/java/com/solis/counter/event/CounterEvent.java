package com.solis.counter.event;

import lombok.Data;

/**
 * 计数事件模型。
 *
 * <p>用于描述一次状态变化导致的计数增量（如点赞 +1 / 取消点赞 -1），
 * 由生产者发送到 Kafka，消费者聚合后折叠到汇总计数。</p>
 */
@Data
public class CounterEvent {
    private String entityType; //在统计什么东西
    private String entityId;
    private String metric; // like | fav（指标名称）
    private int idx; // schema index（见 CounterSchema.NAME_TO_IDX）
    private long userId;
    private int delta; // +1 / -1

    public CounterEvent(String entityType, String entityId, String metric, int idx, long userId, int delta) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.metric = metric;
        this.idx = idx;
        this.userId = userId;
        this.delta = delta;
    }

    /**
     * 静态方法，简化对象创建，代替new关键字
     * @param entityType 实体类型
     * @param entityId 实体ID
     * @param metric 指标名称
     * @param idx 指标索引，做快速映射，提升性能（避免了字符串查找）
     * @param userId 用户ID
     * @param delta 计数增量
     * @return CounterEvent对象
     */
    public static CounterEvent of(String entityType, String entityId, String metric, int idx, long userId, int delta) {
        return new CounterEvent(entityType, entityId, metric, idx, userId, delta);
    }
}