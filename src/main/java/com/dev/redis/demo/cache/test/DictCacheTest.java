package com.dev.redis.demo.cache.test;

import com.alibaba.fastjson.JSON;
import com.dev.redis.demo.cache.annotation.CacheExp;
import com.dev.redis.demo.cache.dict.Dict;
import com.dev.redis.demo.cache.dict.DictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/dict")
@EnableAutoConfiguration
@CacheConfig(cacheNames = "dict")
public class DictCacheTest {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private DictService dictService;

    /**
     * 将返回值缓存到redis，
     * 下次同条件调用该方法时直接从缓存中获取
     *
     * @param param
     * @param dict_id
     * @return Object
     */
    @Cacheable(value="dict", key="#root.target.getFormatKey(#p0,#p1)" /* keyGenerator = "keyGen" */, sync = true)
    @GetMapping("cache/{dict_id}")
    public Object cacheCustomTtl(ModelMap param, @PathVariable("dict_id") String dict_id) {
        Dict dict = dictService.getById(Integer.parseInt(dict_id));
        return JSON.toJSON(dict);
    }

    /**
     * 非dict缓存, 使用系统默认配置过期时间
     *
     * @param param
     * @param dict_id
     * @return
     */
    @Cacheable(value = "dict_default_ttl", key="#root.target.getFormatKey(#p0,#p1)")
    @GetMapping("cache_0/{dict_id}")
    public Object cacheDefaultTtl(ModelMap param, @PathVariable("dict_id") String dict_id) {
        Dict dict = dictService.getById(Integer.parseInt(dict_id));
        return JSON.toJSON(dict);
    }

    /**
     * 方法执行完毕之后删除对应的缓存
     *
     * @param param
     * @param dict_id
     * @return
     */
    @CacheEvict( value="dict", key="#root.target.getFormatKey(#p0,#p1)" )
    @GetMapping("evict/{dict_id}")
    public Object evict(ModelMap param, @PathVariable("dict_id") String dict_id) {
        Dict dict = dictService.getById(Integer.parseInt(dict_id));
        return JSON.toJSON(dict);
    }

    /**
     * 执行前不会去检查缓存中是否存在之前执行过的结果，
     * 每次都会执行该方法，并将执行结果存入指定的缓存中。
     *
     * @param param
     * @param dict_id
     * @return
     */
    @CachePut( value="dict", key="#root.target.getFormatKey(#p0,#p1)" )
    @GetMapping("put/{dict_id}")
    public Object put(ModelMap param, @PathVariable("dict_id") String dict_id) {
        Dict dict = dictService.getById(Integer.parseInt(dict_id));
        return JSON.toJSON(dict);
    }

    @CacheExp( key="dev-redis::dict:id:#p1", expireTime = 30)
    @GetMapping("expire/{dict_id}")
    public Object expire(ModelMap param, @PathVariable("dict_id") String dict_id) {
        Dict dict = dictService.getById(Integer.parseInt(dict_id));
        return JSON.toJSON(dict);
    }

    /**
     * 生成k
     *
     * @param param
     * @param id
     * @return
     */
    public String getFormatKey(ModelMap param,String id){//生成key
        return "id:".concat(id);
    }

}
