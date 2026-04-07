package com.qixiaopi.point.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimiter {
    String key() default "";
    int permitsPerMinute() default 10;
    int permitsPerSecond() default 0;
}
