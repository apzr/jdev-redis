package com.dev.redis.demo.message.expired;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class ConfigForKeyExpiredListener {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private RedisConnectionFactory connectionFactory;

    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }

    @Bean
    public KeyExpiredListener registerListener() {
        RedisMessageListenerContainer redisMessageListenerContainer = applicationContext.
                getBean(RedisMessageListenerContainer.class);
        redisMessageListenerContainer.setConnectionFactory(connectionFactory);
        KeyExpiredListener listener = new KeyExpiredListener();
        redisMessageListenerContainer.addMessageListener(listener,new PatternTopic("__keyevent@*__:expired"));
        return listener;
    }

}
