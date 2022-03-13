package com.heng.redis.test;


import com.heng.redis.lock.DistributedLock;
import com.heng.redis.lock.RedisDistributedLockClient;

import java.util.concurrent.TimeUnit;

public class Test {

    private static RedisDistributedLockClient distributedLockClient = new RedisDistributedLockClient("127.0.0.1",6379);

    public static void main(String[] args) throws InterruptedException {
        DistributedLock myLock = distributedLockClient.getLock("myLock");
        myLock.lock();
        System.out.println("hello");
        say();
//        synchronized (Test.class){
//            Test.class.wait();
//        }
    }

    public static void say() throws InterruptedException {
        DistributedLock myLock = distributedLockClient.getLock("myLock");
        myLock.lock();
        System.out.println("world");
        TimeUnit.SECONDS.sleep(3);
        myLock.unlock();
    }
}
