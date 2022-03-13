package com.heng.redis.timewheel;

import java.util.concurrent.Callable;

public interface AddTimeWheel extends TimeWheel,Router{

    void add(Integer secounds, Callable task);

}
