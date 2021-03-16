package com.dev.redis.demo.lock;

import com.dev.redis.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractDistributedLock implements DistributedLock {

    @Autowired
    RedisUtil redisUtil;

    @Override
    public boolean lock(String key) {
        return lock(key, TIMEOUT_MILLIS, RETRY_TIMES, SLEEP_MILLIS);
    }

}