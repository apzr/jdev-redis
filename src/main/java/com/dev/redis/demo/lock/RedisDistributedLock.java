package com.dev.redis.demo.lock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 */
@Component
public class RedisDistributedLock extends AbstractDistributedLock {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    //@Autowired
    private DefaultRedisScript<String> defaultRedisScript;

    @Override
//    public boolean lock(String key){
//        try {
//            redisTemplate.opsForValue().set(key, "lockFlag", TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
//            return true;
//        } catch (Exception e) {
//
//        }
//        return false;
//    }

    public boolean lock(String key) {
        return lock(key, TIMEOUT_MILLIS, RETRY_TIMES, SLEEP_MILLIS);
    }



    //@Scheduled(cron="*/5 * * * * *")
    public boolean lock(String key, long timeout, int retry, long sleep){

        String lock = LOCK_PREFIX + key;

        return (Boolean) redisTemplate.execute( (RedisCallback)connection -> {

            long expireAt = System.currentTimeMillis() + timeout + 1;
            Boolean acquire = connection.setNX(lock.getBytes(), String.valueOf(expireAt).getBytes());

            if (acquire) {
                return Boolean.TRUE;//拿到锁
            } else {
                byte[] value = connection.get(lock.getBytes());
                if (Objects.nonNull(value) && value.length > 0) {

                    long expireTime = Long.parseLong(new String(value));
                    // 如果锁已经过期
                    if (expireTime < System.currentTimeMillis()) {
                        // 重新加锁，防止死锁
                        byte[] oldValue = connection.getSet(lock.getBytes(), String.valueOf(System.currentTimeMillis() + timeout + 1).getBytes());
                        return Long.parseLong(new String(oldValue)) < System.currentTimeMillis();
                    }
                }
            }

            return Boolean.FALSE;
        });
    }

    public boolean release(String key) {
        try {
            redisTemplate.delete(LOCK_PREFIX + key);
            return true;
        } catch (Exception e) {

        }
        return false;
//        String lockValue = (String) service.get(key);
//        if (lockValue.equals(lockValue)){
//            service.del(key);
//        }

//        // 使用lua脚本进行原子删除操作
//        String checkAndDelScript =  "   if redis.call('get', KEYS[1]) == ARGV[1] then   " +
//                                    "   return redis.call('del', KEYS[1])   " +
//                                    "   else        " +
//                                    "   return 0    " +
//                                    "   end         ";
//        jedis.eval(checkAndDelScript, 1, lockKey, lockValue);


    }


    public Boolean exexLua(String key, String value, String expireTime) {
        try {
            List<String> keyList = new ArrayList<String>();
            keyList.add(key);

            List<Object> argList = new ArrayList<Object>();
            keyList.add(expireTime);
            keyList.add(value);

            redisTemplate.execute(defaultRedisScript, keyList, argList.toArray());

            return true;
        } catch (Exception e) {

            e.printStackTrace();
            return false;
        }
    }

    public boolean lockWithScript(){
        return false;
    }

}
