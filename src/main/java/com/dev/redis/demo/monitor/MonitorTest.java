package com.dev.redis.demo.monitor;

import com.alibaba.fastjson.JSON;
import com.dev.redis.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("monitor")
@EnableAutoConfiguration
public class MonitorTest {

    @Autowired
    private RedisTemplate redisTemplate;

    @Resource
    private RedisUtil redisUtil;

    @GetMapping("/")
    public Object monitor(ModelMap param) {
        return monitor(param, null);
    }

    /**
     * 查询相关参数
     *
     * @param param
     * @param option
     * @return
     */
    @GetMapping("{option}")
    public Object monitor(ModelMap param, @PathVariable("option") String option) {
        if("hits".equals(option))
            return getHitsRate(param);

        String jsonStr = redisUtil.getInfo(option);
        return JSON.parseObject(jsonStr);
    }

    /**
     * 统计命中率
     *
     * @param param
     * @return
     */
    private Object getHitsRate(ModelMap param) {
        String jsonStr = redisUtil.getHits();
        return JSON.parseObject(jsonStr);
    }
}
