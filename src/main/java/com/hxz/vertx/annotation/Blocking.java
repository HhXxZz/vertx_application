package com.hxz.vertx.annotation;

import java.lang.annotation.*;

/**
 * 阻塞，耗时任务接口声明
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Blocking {

}
