

package com.dev.redis.demo.lock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RedisLockTest {
    @Resource
    private RedisLock redisLock;

    @Resource
    private RedisTemplate redisTemplate;


    @Before
    public void setUp() {

        Map<String,String> order=new HashMap<String,String>();
        order.put("name","user00");
        order.put("price","15");
        order.put("state","wait_pay");
        order.put("from","app");
        order.put("create","202101010011694");
        redisTemplate.opsForHash().putAll("orderId", order);

        redisTemplate.opsForValue().set("sale_count", 10, 600, TimeUnit.SECONDS);

    }

    /**
     * 测试lua实现的悲观锁
     *
     * @throws Exception
     */
    @Test
    public void test() throws Exception {
        Website msTest = new Website(redisLock);

        Runnable[] tasks = new Runnable[3];
        tasks[0] = msTest.doEditOrder("orderId", "state", "payed", redisTemplate.opsForHash());
        tasks[1] = msTest.doEditOrder("orderId", "create", "222222222222222", redisTemplate.opsForHash());
        tasks[2] = msTest.doEditOrder("orderId", "price", "999", redisTemplate.opsForHash());

        msTest.startTaskAllInOnce(tasks);
    }

    /**
     * 测试watch实现的乐观锁
     *
     * @throws Exception
     */
    @Test
    public void testLock() throws Exception {
        //redisLock.lock(Thread.currentThread().getName(), 5);
        Website msTest = new Website(redisLock);
        msTest.startTaskAllInOnce(20, msTest.doMiaosha());
    }

}