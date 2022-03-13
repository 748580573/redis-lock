package com.heng.redis.timewheel;

import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class HourTimeWheel implements AddTimeWheel,Router{

    private MinuteTimeWheel[] minuteTimeWheels = new MinuteTimeWheel[60];

    private AtomicInteger currentMinuteTimeWheelsPoint = new AtomicInteger(0);

    private AtomicInteger nextMinuteTimeWheelsPoint = new AtomicInteger(0);

    @Override
    public TimeWheel rout(int secound) {
        TimeWheel timeWheel;
        MinuteTimeWheel currentMinuteTimeWheel = (MinuteTimeWheel) getCurrentTimeWheel();
        Integer minute = (currentMinuteTimeWheel.getCurrentTimePoint() + secound) / 60;
        if (minute < 1){
            timeWheel = currentMinuteTimeWheel;
        }else {
            timeWheel = getTimeWheel(currentMinuteTimeWheelsPoint.get() + minute);
        }

        return timeWheel;
    }

    @Override
    public LinkedBlockingQueue<Callable> getTaskTask() {
        return null;
    }

    @Override
    public TimeWheel getCurrentTimeWheel() {
        if (minuteTimeWheels[currentMinuteTimeWheelsPoint.get()] == null){
            minuteTimeWheels[currentMinuteTimeWheelsPoint.get()] = new MinuteTimeWheel();
        }
        return minuteTimeWheels[currentMinuteTimeWheelsPoint.get()];
    }

    @Override
    public void add(Integer secounds, Callable task) {
        MinuteTimeWheel currentMinuteTimeWheel = (MinuteTimeWheel) getCurrentTimeWheel();
        AddTimeWheel rout = (AddTimeWheel) rout(secounds);
        Integer secoundSlot = (currentMinuteTimeWheel.getCurrentTimePoint() + secounds) % 60;
        rout.add(secoundSlot,task);
    }

    @Override
    public TimeWheel getNextTimeWheel() {

        if (getCurrentTimeWheel().isFinal()){
            currentMinuteTimeWheelsPoint.getAndIncrement();
            return minuteTimeWheels[nextMinuteTimeWheelsPoint.getAndIncrement()];
        }
        return getCurrentTimeWheel();
    }

    @Override
    public Integer getCurrentTimePoint() {
        return currentMinuteTimeWheelsPoint.get();
    }

    @Override
    public boolean isFinal() {
        return currentMinuteTimeWheelsPoint.get() >= 60;
    }

    private TimeWheel getTimeWheel(int minute){
        if (minuteTimeWheels[minute] == null){
            minuteTimeWheels[minute] = new MinuteTimeWheel();
        }
        return minuteTimeWheels[minute];
    }
}
