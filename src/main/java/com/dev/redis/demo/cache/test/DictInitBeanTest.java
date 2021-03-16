package com.dev.redis.demo.cache.test;

import com.dev.redis.demo.cache.dict.Dict;
import com.dev.redis.demo.cache.dict.DictService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class DictInitBeanTest implements InitializingBean{

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private DictService dictService;

    /**
     * 加载bean完成之后执行
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception{
//        List<Dict> ds = dictService.top(5);
//        redisTemplate.opsForList().leftPushAll(ds);
//        if(ds!=null)
//            ds.forEach(d -> {
//                redisTemplate.opsForValue().set("dev-redis::dict:id:"+d.getId(), d);
//            });
//
//        System.out.println(this.getClass().toString()+Instant.now().plusMillis(TimeUnit.HOURS.toMillis(8)));
    };

}
