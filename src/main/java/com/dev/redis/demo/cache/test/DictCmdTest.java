package com.dev.redis.demo.cache.test;

import com.dev.redis.demo.cache.dict.Dict;
import com.dev.redis.demo.cache.dict.DictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


/**
 *
 * PostConstruct是指在实例化bean的时候预先执行的方法
 * CommandLineRunner是指在项目启动后执行的方法
 *
 */
@Component
public class DictCmdTest implements CommandLineRunner {

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private DictService dictService;

    @Override
    public void run(String... args) throws Exception {
        List<Dict> ds = dictService.top(5);
        redisTemplate.opsForList().leftPushAll("dev-redis::dict", ds);
        if(ds!=null)
            ds.forEach(d -> {
                redisTemplate.opsForValue().set("dev-redis::dict:id:"+d.getId(), d);
            });

        System.out.println(this.getClass().toString()+Instant.now().plusMillis(TimeUnit.HOURS.toMillis(8)));
    }
}
