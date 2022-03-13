package com.heng.redis.lock;

import com.heng.redis.timewheel.WatchDog;
import redis.clients.jedis.Jedis;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;

public class RedisLock  implements DistributedLock {

    private Jedis jedis;

    private final String lockName;

    private String threadKey;

    private AtomicBoolean continueLock = new AtomicBoolean(false);

    private final WatchDog watchDog;

    private static final ThreadLocal<String> threadKeyThreadLocal = new ThreadLocal<>();

    public RedisLock(Jedis jedis, String lockName, WatchDog watchDog){
        this.jedis = jedis;
        threadKey = threadKeyThreadLocal.get();
        if (threadKey == null){
            threadKey = UUID.randomUUID().toString() + ":" + Thread.currentThread().getId();
            threadKeyThreadLocal.set(threadKey);
        }
        this.lockName = lockName;
        this.watchDog = watchDog;
    }


    @Override
    public void lock() {
        String lockLua =
                "if (redis.call('exists',KEYS[1]) == 1) then\n" +
                        "    if(redis.call('hexists',KEYS[1],ARGV[1]) == 1) then\n" +
                        "        redis.call('hincrby',KEYS[1],ARGV[1],1);\n" +
                        "        redis.call('pexpire', KEYS[1],10000);\n" +
                        "        return nil;\n" +
                        "    else\n" +
                        "        return redis.call('pttl',KEYS[1]);\n" +
                        "    end\n" +
                        "else\n" +
                        "    redis.call('hset',KEYS[1],ARGV[1],ARGV[2]);\n" +
                        "    redis.call('pexpire', KEYS[1],10000);\n" +
                        "    return nil;\n" +
                        "end\n";
        Object ttl = jedis.eval(lockLua, Arrays.asList(lockName), Arrays.asList(threadKey, "1"));
        try {
            if (ttl != null){
                Integer sleepTime = Integer.valueOf(ttl.toString()) / 1000;
                TimeUnit.SECONDS.sleep(sleepTime);
            }

            continueLock.set(true);
            String contuneLockLua = "" +
                    "if (redis.call('hexists',KEYS[1],ARGV[1]) == 1) then\n" +
                    "    redis.call('pexpire',KEYS[1],ARGV[2]);\n" +
                    "    return 1;\n" +
                    "end;\n" +
                    "    return -1;";
            new Thread(() -> {
                while (true){
                    watchDog.addEvent(3, new Callable() {
                        @Override
                        public Object call() throws Exception {
                            int result = Integer.parseInt(jedis.eval(contuneLockLua, Arrays.asList(lockName), Arrays.asList(threadKey, "10000")).toString());
                            return result;
                        }
                    });
                    try {
                        TimeUnit.SECONDS.sleep(3);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }).start();


        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }

    @Override
    public boolean tryLock() {
        return false;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public void unlock() {
        String unLockLua = "" +
                "if (redis.call('hget',KEYS[1],ARGV[1]) == 1) then\n" +
                "    redis.call('del',KEYS[1]);\n" +
                "else\n" +
                "    redis.call('hincrby',KEYS[1],ARGV[1],-1);\n" +
                "    redis.call('pexpire', KEYS[1],30000);\n" +
                "end";
        Object result = jedis.eval(unLockLua, Arrays.asList(lockName), Arrays.asList(threadKey));
    }

    @Override
    public Condition newCondition() {
        return null;
    }


    public void hset(String key, String field, String value){
        String lua = "redis.call('hset',KEYS[1],ARGV[1],ARGV[2])";
        jedis.eval(lua, Arrays.asList(key),Arrays.asList(field,value));
    }
}
