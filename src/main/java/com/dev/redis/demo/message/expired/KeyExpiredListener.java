package com.dev.redis.demo.message.expired;

import com.dev.redis.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * 消息过期事件监听
 *
 * 需先redis.conf文件配置
 * notify-keyspace-events Ex
 */
@Component
public class KeyExpiredListener implements MessageListener {

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 当有过期的消息时 触发这里
     * @param message
     * @param pattern
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String( message.getBody() );
        //String msgBody = (String) redisTemplate.getValueSerializer().deserialize(body);
        System.out.println(body);
        System.out.println(redisUtil.get(body));//null

        String channel = new String( message.getChannel() );
        //String msgChannel = (String) redisTemplate.getValueSerializer().deserialize(channel);
        System.out.println(channel);

        String msgPattern = new String(pattern);
        System.out.println(msgPattern);
    }


}