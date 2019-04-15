package com.example.zklock.core;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

@Aspect
public class LockAspect {

    private static final Logger LOG = LoggerFactory.getLogger(LockAspect.class);
    private static final String PATH_SPLIT = "/";

    @Value("${zk.lock.prefix:zk}")
    private String prefix;
    @Autowired
    private CuratorFramework zkClient;

    @Pointcut("@annotation(ZkLock)")
    public void zkLock() {
    }

    @Around("zkLock() && @annotation(lock)")
    public Object lockAround(ProceedingJoinPoint pjp, ZkLock lock) throws Throwable {
        String lockPath = buildLockPath(pjp, lock);
        Object result = null;
        InterProcessMutex mutex = new InterProcessMutex(zkClient, lockPath);
        LOG.info("Acquire lock: " + lockPath);
        if (mutex.acquire(5, TimeUnit.MINUTES)) {
            try {
                LOG.info("Executing...");
                result = pjp.proceed();
            } finally {
                mutex.release();
                LOG.info("Lock release");
            }
        }
        return result;
    }

    private String buildLockPath(ProceedingJoinPoint pjp, ZkLock lock) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(PATH_SPLIT);
            sb.append(prefix);
            sb.append(PATH_SPLIT);
            sb.append(lock.value());
            Object[] args = pjp.getArgs();
            if (args.length > 0) {
                String subName;
                Object arg = args[0];
                String key = lock.key();
                if ("".equals(key)) {
                    subName = arg.toString();
                } else {
                    Class<?> aClass = arg.getClass();
                    Field field = aClass.getDeclaredField(key);
                    field.setAccessible(true);
                    subName = field.get(arg).toString();
                }
                sb.append(PATH_SPLIT).append(subName);
            }
            return sb.toString();
        } catch (Exception ignored) {
            return PATH_SPLIT + prefix + PATH_SPLIT + lock.lockName();
        }
    }
}
