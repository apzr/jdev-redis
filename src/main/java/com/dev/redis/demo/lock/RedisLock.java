package com.dev.redis.demo.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 分布式锁工具类
 * 实现方式为执行Lua脚本
 */
@Component
public class RedisLock {

    private final static Logger log = LoggerFactory.getLogger(RedisLock.class);

    @Autowired
    RedisTemplate redisTemplate;

    private StringRedisSerializer argsStringSerializer = new StringRedisSerializer();
    private StringRedisSerializer resultStringSerializer = new StringRedisSerializer();
    private final String EXEC_RESULT = "1";

    //定义获取锁的lua脚本
    private final static DefaultRedisScript<String> LOCK_LUA_SCRIPT = new DefaultRedisScript<>(
            " if redis.call('set',KEYS[1],ARGV[1],'NX','PX',ARGV[2]) then "
                    + "return '1' "
                    + "else "
                    + "return '0' "
                    + "end"
            , String.class
    );

    //定义释放锁的lua脚本
    private final static DefaultRedisScript<String> UNLOCK_LUA_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get',KEYS[1]) == ARGV[1] then "
                    + "return tostring(redis.call('del',KEYS[1])) "
                    + "else "
                    + "return '0' "
                    + "end"
            , String.class
    );

    /**
     * 加锁操作
     *
     * @param key        Redis 锁的 key 值
     * @param requestId  请求id，防止解了不该由自己解的锁 (随机生成)
     * @param expireTime 锁的超时时间(毫秒)
     * @param retryTimes 获取锁的重试次数
     * @return true or false
     */
    @SuppressWarnings("unchecked")
    public boolean lock(String key, String requestId, String expireTime, int retryTimes) {
        if (retryTimes <= 0) {
            retryTimes = 1;
        }
        try {
            int count = 0;
            while (true) {
                String result = (String) redisTemplate.execute(LOCK_LUA_SCRIPT, argsStringSerializer, resultStringSerializer,
                        Collections.singletonList(key), requestId, expireTime);
                log.debug("result:{},type:{}", result, result.getClass().getName());
                if (EXEC_RESULT.equals(result)) {
                    log.info("lock---->result:{},requestId:{}", "加锁成功", requestId);
                    return true;
                } else {
                    count++;
                    if (retryTimes == count) {
                        log.warn("has tried {} times , failed to acquire lock for key:{},requestId:{}", count, key, requestId);
                        return false;
                    } else {
                        log.warn("try to acquire lock {} times for key:{},requestId:{}", count, key, requestId);
                        Thread.sleep(100);
                        continue;
                    }
                }
            }
        } catch (InterruptedException e) {
            log.error("lock---->result:{},requestId:{}", "加锁异常", requestId);
        }
        return false;
    }


    /**
     * watch
     *
     * @throws Exception
     */
    public void lock(String requestId, int retryLimit) throws Exception {
        if (retryLimit <= 0) {
            retryLimit = 1;
        }

        String key = "sale_count";// 商品数量
        String clientName = requestId;//+"_order_"+UUID.randomUUID().toString().replace("-", "");


        try {

            while (true) {
                try {
                    int tryTimes = 0;
                    redisTemplate.setEnableTransactionSupport(true);
                    redisTemplate.watch(key);//锁


                    Object count = redisTemplate.opsForValue().get(key);
                    log.warn("用户:" + clientName + "开始抢商品:"+count);
                    //log.warn("当前商品的个数：" + count.toString());

                    if((Integer)count==0){
                        log.warn("没库存了, "+requestId+"不抢了");
                        return;
                    }

                    int prdNum = Integer.parseInt(count.toString());;// 当前商品个数
                    if (prdNum > 0) {

                        redisTemplate.multi();// 事务开始

                        redisTemplate.opsForValue().decrement(key);
                        List<Object> result = redisTemplate.exec();// 原子性提交事物

                        if (result == null || result.isEmpty()) {//提交失败,可能是watch-key被外部修改，或者是数据操作被驳回
                            tryTimes++;
                            if (retryLimit == tryTimes) {
                                log.warn("用户:" + clientName + "第"+tryTimes+"次没有抢到商品, 也不抢了");
                                break;
                            } else {
                                log.warn("用户:" + clientName + "第"+tryTimes+"次没有抢到商品, 再来一次");
                            }
                        } else {//提交成功
                            log.warn("用户:" + clientName + "抢到第"+count.toString()+"个商品");
                            redisTemplate.opsForSet().add(requestId, clientName);// 将抢到的用户存起来

//                            log.warn("用户:" + clientName + "抢到第"+count.toString()+"个商品,高兴一会");
//                            Thread.sleep(1000);
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.warn("用户:" + clientName + "操作异常");
                    e.printStackTrace();
                } finally {
                    redisTemplate.unwatch();// exec，discard，unwatch命令都会清除连接中的所有监视
                }
            } // while
        } catch (Exception e) {
            log.error("redis bug:" + e.getMessage());
        } finally {
            /**
             * 事务完成
             *
             * RedisTemplate配置enableTransactionSupport为true（开启事务）时，连接不会自动释放，解决：
             * 1.enableTransactionSupport设置为false（关闭事务）
             * 2.RedisTemplate操作后加入手动释放代码
             * RedisConnectionUtils.unbindConnection(redisTemplate.getConnectionFactory());
             */
            RedisConnectionUtils.unbindConnection(redisTemplate.getConnectionFactory());
            redisTemplate.setEnableTransactionSupport(false);
        }
    }

    /**
     * 解锁操作
     *
     * @param key       Redis 锁的 key 值
     * @param requestId 请求 id, 防止解了不该由自己解的锁 (随机生成)
     * @return true or false
     */
    @SuppressWarnings("unchecked")
    public boolean unLock(String key, String requestId) {
        String result = (String) redisTemplate.execute(UNLOCK_LUA_SCRIPT, argsStringSerializer, resultStringSerializer,
                Collections.singletonList(key), requestId);
        if (EXEC_RESULT.equals(result)) {
            log.info("unLock---->result:{},requestId:{}", "解锁成功", requestId);
            return true;
        }
        return false;
    }
}
