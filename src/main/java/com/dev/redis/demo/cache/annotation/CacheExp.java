package com.dev.redis.demo.cache.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheExp {

    String key() default "";

    String nextKey() default "";

    int expireTime() default 300;//秒

}