package com.example.zklock.core;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ZkLock {

    @AliasFor("lockName")
    String value() default "lock";

    @AliasFor("value")
    String lockName() default "lock";

    String key() default "";
}


