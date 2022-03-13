package com.heng.redis.timewheel;

import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class MinuteTimeWheel implements AddTimeWheel{

    private SecoundTimeWheel[] secoundTimeWheels = new SecoundTimeWheel[60];

    private AtomicInteger currentSecoundTimeWheelPoint = new AtomicInteger(0);

    private AtomicInteger nextSecoundTimeWheelPoint = new AtomicInteger(0);

    @Override
    public void add(Integer secounds, Callable task) {
        SecoundTimeWheel secoundTimeWheel = (SecoundTimeWheel) rout(secounds);
        secoundTimeWheel.add(secounds % 60,task);
    }

    @Override
    public TimeWheel rout(int secound) {
        TimeWheel timeWheel;
        if (secoundTimeWheels[secound] == null){
            secoundTimeWheels[secound] = new SecoundTimeWheel();
        }

        timeWheel = secoundTimeWheels[secound];
        return timeWheel;
    }

    @Override
    public LinkedBlockingQueue<Callable> getTaskTask() {
        return null;
    }

    @Override
    public TimeWheel getCurrentTimeWheel() {
        return secoundTimeWheels[currentSecoundTimeWheelPoint.get()];
    }

    @Override
    public TimeWheel getNextTimeWheel() {
        SecoundTimeWheel secoundTimeWheel = secoundTimeWheels[nextSecoundTimeWheelPoint.get() % 60];
        if (secoundTimeWheel == null){
            secoundTimeWheels[nextSecoundTimeWheelPoint.get() % 60] = new SecoundTimeWheel();
            secoundTimeWheel = secoundTimeWheels[nextSecoundTimeWheelPoint.get() % 60];
        }
        nextSecoundTimeWheelPoint.getAndIncrement();
        currentSecoundTimeWheelPoint.compareAndSet(currentSecoundTimeWheelPoint.get(),nextSecoundTimeWheelPoint.get() - 1);
        return secoundTimeWheel;
    }

    @Override
    public Integer getCurrentTimePoint() {
        return currentSecoundTimeWheelPoint.get();
    }

    @Override
    public boolean isFinal() {
        return nextSecoundTimeWheelPoint.get() > 58 ;
    }
}
