package com.heng.redis.timewheel;

import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

public class SecoundTimeWheel implements AddTimeWheel{

    private LinkedBlockingQueue<Callable> taskQueue = new LinkedBlockingQueue<>();

    public void add(Integer secounds, Callable task) {
        taskQueue.add(task);
    }

    @Override
    public LinkedBlockingQueue<Callable> getTaskTask() {
        return taskQueue;
    }

    @Override
    public TimeWheel getCurrentTimeWheel() {
        return null;
    }

    @Override
    public TimeWheel getNextTimeWheel() {
        return null;
    }

    @Override
    public Integer getCurrentTimePoint() {
        return null;
    }

    @Override
    public boolean isFinal() {
        return true;
    }

    @Override
    public TimeWheel rout(int secound) {
        return null;
    }
}
