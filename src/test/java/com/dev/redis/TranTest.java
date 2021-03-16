package com.dev.redis;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.*;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TranTest {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 原子性
     * watch
     * 乐观锁ms
     *
     * @throws Exception
     */
    @Test
    public void testTran() throws Exception{
        /*
            JedisConnectionFactory factory =
            (JedisConnectionFactory) redisTemplate.getConnectionFactory();
            factory.setDatabase(dbIndex);
            redisTemplate.setConnectionFactory(factory);
         */

        String key = "TRAN";
        //redisTemplate.opsForValue().set(key,10);

        Object k = redisTemplate.opsForValue().get(key);
        if(k==null){
            redisTemplate.opsForValue().set(key, "0", 60, TimeUnit.SECONDS);
        }

        redisTemplate.execute(new SessionCallback<Object>() {
                @Override
                public Object execute(RedisOperations operations) {
                    List<Object> result = null;
                    do {
                        int count = 0;
                        operations.watch(key);  // watch某个key,当该key被其它客户端改变时,则会中断当前的操作

                        count = new Integer( operations.opsForValue().get(key).toString() );
                        count = count + 10;

                        operations.multi(); //开始事务
                        operations.opsForValue().set(key, count);

                        try {
                            Thread.sleep(8000);
                            int a = 1/0;
                            result = operations.exec(); //提交事务

                        } catch (Exception e) {

                        }
                    } while (result == null); //如果失败则重试
                    return null;
                }
        });

    }

}