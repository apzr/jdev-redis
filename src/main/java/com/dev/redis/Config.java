package com.dev.redis;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scripting.support.ResourceScriptSource;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * REDIS 配置
 */
@Configuration
@EnableCaching
public class Config extends CachingConfigurerSupport  {
    /**
     * 从JSON的KV与OBJECT属性映射
     * @return
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        om.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        return om;
    }

//    /**
//     * GenericJackson
//     * 效率低，内存高。
//     * 反序列化带泛型的数组类会报转换异常，解决办法存储以JSON字符串存储。
//     *
//     * @param om
//     * @return
//     */
//    @Bean
//    public GenericJackson2JsonRedisSerializer jackson2JsonRedisSerializer(ObjectMapper om) {
//        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(om);
//        return serializer;
//    }

    /**
     * Jackson2
     * 存入redis时的序列化方式
     * 需要指明序列化的类Class，可以使用Obejct.class
     *
     * @param om
     * @return
     */
    @Bean
    public Jackson2JsonRedisSerializer jackson2JsonRedisSerializer(ObjectMapper om) {
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        jackson2JsonRedisSerializer.setObjectMapper(om);
        return jackson2JsonRedisSerializer;
    }

    /**
     * 缓存管理器
     *
     * @param redisConnectionFactory
     * @return
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        return new RedisCacheManager(
                RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory),
                this.getRedisCacheConfigurationWithTtl(10*60), // 默认策略，未配置的 key 会使用这个
                this.getRedisCacheConfigurationMap() // 指定 key 策略
        );
    }

    /**
     * 单独配置指定缓存名称的过期时间
     *
     * @return
     */
    private Map<String, RedisCacheConfiguration> getRedisCacheConfigurationMap() {
        Map<String, RedisCacheConfiguration> redisCacheConfigurationMap = new HashMap<>();

        redisCacheConfigurationMap.put("dict", this.getRedisCacheConfigurationWithTtl(60));

        return redisCacheConfigurationMap;
    }

    /**
     * 配置所有缓存的默认过期时间
     *
     * @param expireTime
     * @return
     */
    private RedisCacheConfiguration getRedisCacheConfigurationWithTtl(Integer expireTime) {
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);

        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig();
        redisCacheConfiguration = redisCacheConfiguration.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer)
        ).entryTtl(Duration.ofSeconds(expireTime))//ttl
        .computePrefixWith(cacheName -> "dev-redis".concat("::").concat(cacheName).concat(":"));//前缀;

        return redisCacheConfiguration;
    }

    /**
     * 两者的数据是不共通
     * StringRedisTemplate只能管理StringRedisTemplate里面的数据
     * RedisTemplate只能管理RedisTemplate中的数据。
     *
     * 使用的序列化类不同:
     * RedisTemplate使用的是JdkSerializationRedisSerializer    存入数据会将数据先序列化成字节数组然后在存入Redis数据库。
     * StringRedisTemplate使用的是StringRedisSerializer
     *
     * 当redis数据库里面本来存的是字符串数据或者你要存取的数据就是字符串类型数据的时候，那么你就使用StringRedisTemplate即可。
     * 若数据是复杂的对象类型，而取出的时候又不想做任何的数据转换，直接从Redis里面取出一个对象，那么使用RedisTemplate是更好的选择。
     *
     * redisTemplate 中存取数据都是字节数组。
     * 当redis中存入的数据是可读形式而非字节数组时，使用redisTemplate取值的时候会无法获取导出数据，获得的值为null。
     * 可以使用 StringRedisTemplate 。
     *
     * @param redisConnectionFactory
     * @param jackson2JsonRedisSerializer
     * @return
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory,
                                                       RedisSerializer jackson2JsonRedisSerializer) {
        StringRedisTemplate stringRedisTemplate = new StringRedisTemplate(redisConnectionFactory);

        RedisSerializer stringSerializer = new StringRedisSerializer();

        stringRedisTemplate.setKeySerializer(stringSerializer);//key序列化
        stringRedisTemplate.setValueSerializer(jackson2JsonRedisSerializer);//value序列化
        stringRedisTemplate.setHashKeySerializer(stringSerializer);//Hash key序列化
        stringRedisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer);//Hash value序列化
        stringRedisTemplate.afterPropertiesSet();

        return stringRedisTemplate;
    }

    /**
     * 同上
     *
     * @param redisConnectionFactory
     * @param jackson2JsonRedisSerializer
     * @return
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory,
                                                       RedisSerializer jackson2JsonRedisSerializer) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        RedisSerializer stringSerializer = new StringRedisSerializer();

        redisTemplate.setKeySerializer(stringSerializer);//key序列化
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);//value序列化
        redisTemplate.setHashKeySerializer(stringSerializer);//Hash key序列化
        redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer);//Hash value序列化
        redisTemplate.afterPropertiesSet();
        //redisTemplate.setEnableTransactionSupport(true);
        return redisTemplate;
    }

//    /**
//     * 配置<1X>
//     * spring boot 2.x中，RedisCacheManager已经没有了单参数的构造方法
//     * 1.x中通过参数redisTemplate配置的方式不可行
//     *
//     * @param redisTemplate
//     * @return
//     */
//    @Bean
//    public CacheManager cacheManager(RedisTemplate redisTemplate) {
//        RedisCacheManager rcm = new RedisCacheManager(redisTemplate);
//        return rcm;
//    }


//    /**
//     * template相关配置<1X>
//     * @param factory
//     * @return
//     */
//    @Bean
//    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
//
//        RedisTemplate<String, Object> template = new RedisTemplate<>();
//        // 配置连接工厂
//        template.setConnectionFactory(factory);
//
//        //使用Jackson2JsonRedisSerializer来序列化和反序列化redis的value值（默认使用JDK的序列化方式）
//        Jackson2JsonRedisSerializer jacksonSeial = new Jackson2JsonRedisSerializer(Object.class);
//
//        ObjectMapper om = new ObjectMapper();
//        // 指定要序列化的域，field,get和set,以及修饰符范围，ANY是都有包括private和public
//        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
//        // 指定序列化输入的类型，类必须是非final修饰的，final修饰的类，比如String,Integer等会跑出异常
//        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
//        jacksonSeial.setObjectMapper(om);
//
//        // 值采用json序列化
//        template.setValueSerializer(jacksonSeial);
//        //使用StringRedisSerializer来序列化和反序列化redis的key值
//        template.setKeySerializer(new StringRedisSerializer());
//
//        // 设置hash key 和value序列化模式
//        template.setHashKeySerializer(new StringRedisSerializer());
//        template.setHashValueSerializer(jacksonSeial);
//        template.afterPropertiesSet();
//
//        return template;
//    }

    /**
     * 对hash类型的数据操作
     *
     * @param redisTemplate
     * @return HashOperations
     */
    @Bean
    public HashOperations<String, String, Object> hashOperations(RedisTemplate<String, Object> redisTemplate) {
        return redisTemplate.opsForHash();
    }

    /**
     * 对redis字符串类型数据操作
     *
     * @param redisTemplate
     * @return ValueOperations
     */
    @Bean
    public ValueOperations<String, Object> valueOperations(RedisTemplate<String, Object> redisTemplate) {
        return redisTemplate.opsForValue();
    }

    /**
     * 对链表类型的数据操作
     *
     * @param redisTemplate
     * @return ListOperations
     */
    @Bean
    public ListOperations<String, Object> listOperations(RedisTemplate<String, Object> redisTemplate) {
        return redisTemplate.opsForList();
    }

    /**
     * 对无序集合类型的数据操作
     *
     * @param redisTemplate
     * @return SetOperations
     */
    @Bean
    public SetOperations<String, Object> setOperations(RedisTemplate<String, Object> redisTemplate) {
        return redisTemplate.opsForSet();
    }

    /**
     * 对有序集合类型的数据操作
     *
     * @param redisTemplate
     * @return ZSetOperations
     */
    @Bean
    public ZSetOperations<String, Object> zSetOperations(RedisTemplate<String, Object> redisTemplate) {
        return redisTemplate.opsForZSet();
    }

    /**
     * 自定义key生成策略
     *
     * @return
     */
    @Bean
    public KeyGenerator keyGen() {

        return (target, method, params) -> {
            return target.getClass().getName().concat(":ID:").concat(params[1].toString());
        };

    }

    /**
     * 运行lua脚本
     * @return
     */
    @Bean
    public DefaultRedisScript<Boolean> defaultRedisScript() {
        DefaultRedisScript<Boolean> defaultRedisScript = new DefaultRedisScript<>();
        defaultRedisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/lockThenAdd.lua")));
        defaultRedisScript.setResultType(Boolean.class);
        return defaultRedisScript;
    }

}

