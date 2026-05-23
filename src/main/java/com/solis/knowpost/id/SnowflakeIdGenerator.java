package com.solis.knowpost.id;

import org.springframework.stereotype.Component;

/**
 * 线程安全的雪花算法 ID 生成器。
 * 41 位时间戳 + 5 位数据中心 + 5 位工作节点 + 12 位序列。
 * java中的long是64位的
 * java整数全部用补码存储
 * -1L  Long类型-1,64位全是1
 */
@Component
public class SnowflakeIdGenerator {
    private static final long EPOCH = 1704067200000L; // 2024-01-01 00:00:00 UTC
    //定义每段占多少位，机器ID，数据中心，序列号
    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;
    //计算机器ID和数据中心ID的最大值31，31
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;
    //序列化掩码，保证序列号在0-4095之间循环
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private final long datacenterId;
    private final long workerId;

    private long lastTimestamp = -1L;
    private long sequence = 0L; //序列号

    public SnowflakeIdGenerator() {
        this(1, 1);
    }

    public SnowflakeIdGenerator(long datacenterId, long workerId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("workerId out of range");
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId out of range");
        }
        this.datacenterId = datacenterId;
        this.workerId = workerId;
    }

    //保证多线程下，sequence 和 lastTimestamp 不被同时修改，确保 ID 不重复、线程安全！
    //同一个时间，只允许一个线程进入方法
    public synchronized long nextId() {
        long timestamp = currentTime();

//        if (timestamp < lastTimestamp) {
//            throw new IllegalStateException("Clock moved backwards. Refusing to generate id");
//        }
        // 等待时钟追回的方案（时钟回拨）
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;

            // 1. 小幅度回拨（比如 NTP 校时导致的 1~5ms 间抖动）：等待一会儿再试
            if (offset <= 5) {
                try {
                    // 睡 offset 毫秒，给系统时钟一点时间“追上来”
                    Thread.sleep(offset);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Thread interrupted while waiting for clock to catch up", e);
                }

                timestamp = currentTime();
                if (timestamp < lastTimestamp) {
                    // 等完还是没追上，说明问题较严重，直接拒绝
                    throw new IllegalStateException(
                            "Clock is still behind after waiting. last=" + lastTimestamp + ", now=" + timestamp);
                }
            } else {
                // 2. 回拨幅度太大，直接拒绝，避免线程长时间阻塞
                throw new IllegalStateException(
                        "Clock moved backwards too much. Refusing to generate id. offset=" + offset + "ms");
            }
        }

        // 处理同一毫秒内的并发请求：序列号逻辑
        if (lastTimestamp == timestamp) {
            //按位于：只有二进制两个都是1，才是1，可以保证在0-4095之间
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                // 这一毫秒的 4096 个名额用完了
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        // 组装 64 位 ID
        return ((timestamp - EPOCH) << TIMESTAMP_LEFT_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    private long waitNextMillis(long lastTimestamp) {
        long timestamp = currentTime();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTime();
        }
        return timestamp;
    }

    private long currentTime() {
        return System.currentTimeMillis();
    }
}