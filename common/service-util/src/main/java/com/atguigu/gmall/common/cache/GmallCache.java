package com.atguigu.gmall.common.cache;

import java.lang.annotation.*;

/**
 * @author atguigu-mqx
 */
@Target({ElementType.METHOD}) // 当前注解的使用范围 在方法上使用
@Retention(RetentionPolicy.RUNTIME)  //表示当前注解生命周期  当前注解 在Java.class  在jvm虚拟机中都可以使用
@Inherited  // 表示一种继承关系
@Documented //文档注解
public @interface GmallCache {

    //  定义一个数据 sku:skuId
    //  目的用这个前缀要想组成 缓存的key！
    String prefix() default "cache:";

    String suffix() default ":info";
}
