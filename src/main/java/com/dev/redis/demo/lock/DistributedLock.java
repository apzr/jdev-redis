package com.dev.redis.demo.lock;

/**
 * 分布锁要实现三个目标
 *
 * 互斥。在任何给定时刻，只有一个客户端可以持有锁。
 * 无死锁。最终，即使锁定资源的客户端崩溃或被分区，也始终可以获取锁定。
 * 容错。只要大多数Redis节点启动，客户端就能够获取和释放锁。
 */
public abstract interface DistributedLock {

    public static final long TIMEOUT_MILLIS = 10000L;
    public static final int RETRY_TIMES = 10;
    public static final long SLEEP_MILLIS = 1000;
    public static final String LOCK_PREFIX = "redis_lock_";

    public abstract boolean lock(String key);

    public abstract boolean lock(String key, long timeout, int retry, long sleep);

    public abstract boolean release(String paramString);
}