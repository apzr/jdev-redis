package com.dev.redis.demo.message.expired;

import com.dev.redis.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/listener")
public class KeyExpiredListenerTest {


    @Autowired
    private RedisUtil redisUtil;

    @GetMapping("/")
    public Map<String,Object> redisTest(){

        redisUtil.set("msg_key", "msg_value",5);

        Map<String,Object> resultMap = new HashMap<String,Object>();
        resultMap.put("msg_key", redisUtil.get("msg_key") );

        return resultMap;
    }
}