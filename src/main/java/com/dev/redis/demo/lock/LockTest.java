package com.dev.redis.demo.lock;

import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

@RestController
@RequestMapping("/lock")
public class LockTest {
    @Autowired
    RedisDistributedLock redisDistributedLock;
    /**
     *
     * @param param
     * @param key
     * @return
     */
    @GetMapping("{key}")
    public Object lockTest(ModelMap param, @PathVariable("key") String key) {

        return doWithLocker(key);
    }

    /**
     *
     * @param key
     * @return
     */
    private Object doWithLocker(String key) {
        StringBuilder builder = new StringBuilder("{");

        boolean lockOK = redisDistributedLock.lock(key);

        if(lockOK){
            builder.append("\"msg_"+Calendar.getInstance().getTime()+"\" : \"拿到锁, 开始运行\", ");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {

            } finally {
                //TODO:模拟多客户端
                //redisDistributedLock.release(key);
            }
        }else{
            int fail = 1;
            while(fail <= RedisDistributedLock.RETRY_TIMES){

                try {
                    Thread.sleep(RedisDistributedLock.SLEEP_MILLIS);
                    System.out.println("sleep.................");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (redisDistributedLock.lock(key)){
                    builder.append("\"msg_"+Calendar.getInstance().getTime()+"\" : \"终于拿到了锁,做一些事情\", ");
                    try { Thread.sleep(3000); } catch (InterruptedException e) {}
                    return JSON.parseObject( builder.subSequence(0, builder.length()-1)+"}" );
                }else{
                    builder.append("\"msg_"+Calendar.getInstance().getTime()+"\" : \"没有拿到锁, 当前第"+fail+"次重试\", ");
                    fail++;
                }
            }
            //throw new RuntimeException("现在请求的人太多了, 请稍后再试");
            builder.append("\"msg_"+Calendar.getInstance().getTime()+"\" : \"现在请求的人太多了, 请稍后再试\", ");
        }

        return JSON.parseObject( builder.subSequence(0, builder.length()-1)+"}" );
    }


    @GetMapping("del/{key}")
    public Object lockDel(ModelMap param, @PathVariable("key") String key) {
        return redisDistributedLock.release(key);
    }


}
