package com.heng.redis.timewheel;

import java.util.concurrent.*;

public class WatchDog {

    private ExecutorService executorService = Executors.newCachedThreadPool();

    private HourTimeWheel hourTimeWheel = new HourTimeWheel();

    public void addEvent(Integer secound,Callable runnable){
        executorService.submit(() -> {
            hourTimeWheel.add(secound,runnable);
        });

    }

    public void start(){
        long start = System.currentTimeMillis();
        Future<Object> submit = executorService.submit(() -> {
            for (; ; ) {
                System.out.println("时间轮运行第：" + (System.currentTimeMillis() - start) / 1000 + "秒");
                TimeWheel secoundWheel = hourTimeWheel.getNextTimeWheel();
                TimeWheel currentTimeWheel = secoundWheel.getNextTimeWheel();
                if (currentTimeWheel == null) {
                    TimeUnit.SECONDS.sleep(1);
                    continue;
                }

                LinkedBlockingQueue<Callable> taskTask = currentTimeWheel.getTaskTask();
                Callable point;
                while ((point = taskTask.poll(1, TimeUnit.SECONDS)) != null) {
                    executorService.submit(point);
                }
            }

        });
    }
}
