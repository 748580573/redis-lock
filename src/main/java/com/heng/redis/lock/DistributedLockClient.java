package com.heng.redis.lock;

public interface DistributedLockClient {

    DistributedLock getLock(String lockName);
}
