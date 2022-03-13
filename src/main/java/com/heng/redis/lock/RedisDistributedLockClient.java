package com.heng.redis.lock;

import com.heng.redis.timewheel.WatchDog;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisDistributedLockClient implements DistributedLockClient{

    private JedisPool jedisPool;

    private WatchDog watchDog;


    public RedisDistributedLockClient(String host,Integer post){
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(1024);
        jedisPoolConfig.setMaxIdle(100);
        jedisPoolConfig.setMaxWaitMillis(100);
        jedisPoolConfig.setTestOnBorrow(false);//jedis 第一次启动时，会报错
        jedisPoolConfig.setTestOnReturn(true);
        this.jedisPool = new JedisPool(jedisPoolConfig,host,post);
        watchDog = new WatchDog();
        watchDog.start();
    }

    @Override
    public DistributedLock getLock(String lockName) {
        RedisLock redisLock = new RedisLock(jedisPool.getResource(),lockName,watchDog);
        return redisLock;
    }
}
