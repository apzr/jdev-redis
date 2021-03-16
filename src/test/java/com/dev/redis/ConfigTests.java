package com.dev.redis;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.*;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ConfigTests {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    @Resource
    private ValueOperations<String,Object> valueOperations;

    @Autowired
    private HashOperations<String, String, Object> hashOperations;

    @Autowired
    private ListOperations<String, Object> listOperations;

    @Autowired
    private SetOperations<String, Object> setOperations;

    @Autowired
    private ZSetOperations<String, Object> zSetOperations;

    @Resource
    private RedisUtil redisUtil;

    @Test
    public void testAutowired() throws Exception{
        Assert.assertNotNull(stringRedisTemplate);
        Assert.assertNotNull(redisTemplate);
        Assert.assertNotNull(valueOperations);
        Assert.assertNotNull(hashOperations);
        Assert.assertNotNull(listOperations);
        Assert.assertNotNull(setOperations);
        Assert.assertNotNull(zSetOperations);
        Assert.assertNotNull(redisUtil);
    }

    @Test
    public void testObj() throws Exception{
        UserVo userVo = new UserVo();
        userVo.setAddress("shanghai");
        userVo.setName("userName0");
        userVo.setAge(123);
        ValueOperations<String,Object> operations = redisTemplate.opsForValue();
        redisUtil.expire("name",20);
        String key = UserVo.Table+"_name_"+userVo.getName();
        UserVo vo = (UserVo) operations.get(key);
        System.out.println(vo);
    }

    @Test
    public void testValueOption( )throws  Exception{
        UserVo userVo = new UserVo();
        userVo.setAddress("shanghai");
        userVo.setName("userName1");
        userVo.setAge(23);
        valueOperations.set("test",userVo);

        System.out.println(valueOperations.get("test"));
    }

    @Test
    public void testSetOperation() throws Exception{
        UserVo userVo = new UserVo();
        userVo.setAddress("peking");
        userVo.setName("userName2");
        userVo.setAge(23);
        UserVo auserVo = new UserVo();
        auserVo.setAddress("中文");
        auserVo.setName("userName3");
        auserVo.setAge(23);
        setOperations.add("user:test",userVo,auserVo);
        setOperations.members("user:test");
        Set<Object> result = setOperations.members("user:test");
        System.out.println(result);
    }

    @Test
    public void HashOperations() throws Exception{
        UserVo userVo = new UserVo();
        userVo.setAddress("beijing");
        userVo.setName("userName4");
        userVo.setAge(23);
        hashOperations.put("hash:user",userVo.hashCode()+"",userVo);
        System.out.println(hashOperations.get("hash:user",userVo.hashCode()+""));
    }

    @Test
    public void  ListOperations() throws Exception{
        UserVo userVo = new UserVo();
        userVo.setAddress("beijing");
        userVo.setName("userName5");
        userVo.setAge(23);
        listOperations.leftPush("list:user",userVo);
        System.out.println(listOperations.leftPop("list:user"));
        System.out.println(listOperations.leftPop("list:user"));
    }

    @Test
    public void stringRedisTemplateOpr () throws Exception{
        redisTemplate.opsForValue().set("age", 10, 20, TimeUnit.SECONDS);
        Assert.assertTrue(20000l>redisTemplate.getExpire("age",TimeUnit.MILLISECONDS) );

        redisTemplate.boundValueOps("age").increment(-1);
        Assert.assertTrue("9".equals( redisTemplate.opsForValue().get("age").toString() ));

        redisTemplate.boundValueOps("age").increment(2);
        Assert.assertTrue("11".equals( redisTemplate.opsForValue().get("age").toString() ));

        redisTemplate.delete("age");
        Assert.assertTrue( !redisTemplate.hasKey("age") );

        redisTemplate.opsForSet().add("red_123", "1","2","3");
        redisTemplate.expire("red_123",5 , TimeUnit.SECONDS);
        Assert.assertTrue(  redisTemplate.opsForSet().isMember("red_123", "1") );

        Assert.assertTrue( redisTemplate.opsForSet().members("red_123").size()==3 );
    }

    @Test
    public void hitRateCount() throws Exception{
        RedisConnection conn = redisTemplate.getConnectionFactory().getConnection();

        Properties redisInfo = conn.info("stats");

        String hits = redisInfo.getProperty("keyspace_hits","0");
        String misses = redisInfo.getProperty("keyspace_misses","1");

        Integer allCount = (new Integer(hits)+new Integer(misses));
        Integer allHits = new Integer(hits);

        double hitsRate = allHits.doubleValue()/allCount.doubleValue();

        Assert.assertTrue(hitsRate>=0 && hitsRate<=1);

        conn.close();
    }

    @Test
    public void counter() throws Exception {
        valueOperations.set("count", 0, 3, TimeUnit.SECONDS);
        org.springframework.util.Assert.isTrue("0".equals(valueOperations.get("count").toString()));

        for (int i = 0; i < 10; i++) {
            valueOperations.increment("count", 1);
            Thread.sleep(200);
        }
        org.springframework.util.Assert.isTrue("10".equals(valueOperations.get("count").toString()));

        Thread.sleep(3300);
        org.springframework.util.Assert.isNull(valueOperations.get("count"));
    }

    @Test
    public void setWithSet() throws Exception {
        setOperations.add("set1", "1","2","3");
        setOperations.getOperations().expire("set1", 20, TimeUnit.SECONDS);
        setOperations.add("set2", "2","3","4");
        setOperations.getOperations().expire("set2", 20, TimeUnit.SECONDS);

        Set<Object> s1 = setOperations.intersect("set1", "set2");
        org.springframework.util.Assert.isTrue(2==s1.size() );

        setOperations.intersectAndStore("set1", "set2", "set3");
        Set<Object> s2 = setOperations.members("set3");
        org.springframework.util.Assert.isTrue(2==s2.size() );
    }

    @Test
    public void rankWithZSet() throws Exception {
        zSetOperations.add("fruit", "apple", 1d);
        zSetOperations.add("fruit", "cherry", 2d);
        zSetOperations.add("fruit", "orange", 3d);
        zSetOperations.add("fruit", "peach", 4d);
        zSetOperations.add("fruit", "pear", 5d);

        Set<Object> fruits = zSetOperations.rangeByScore("fruit", 0, 3);
        org.springframework.util.Assert.hasText(fruits.toString(), "apple");
        org.springframework.util.Assert.hasText(fruits.toString(), "cherry");
        org.springframework.util.Assert.hasText(fruits.toString(), "orange");

        zSetOperations.add("fruit", "cherry", 6d);
        fruits = zSetOperations.rangeByScore("fruit", 0, 3);
        org.springframework.util.Assert.doesNotContain(fruits.toString(), "cherry");
    }

    @Test
    public void pubAndSub(){
        redisTemplate.convertAndSend("chan0", "msg from channel_0");
        redisTemplate.convertAndSend("chan1", "msg from channel_1");
    }
}