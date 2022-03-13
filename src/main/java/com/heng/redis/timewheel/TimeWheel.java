package com.heng.redis.timewheel;

import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

public interface TimeWheel {

    LinkedBlockingQueue<Callable> getTaskTask();

    TimeWheel getCurrentTimeWheel();

    TimeWheel getNextTimeWheel();

    Integer getCurrentTimePoint();

    boolean isFinal();
}
