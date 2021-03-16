package com.dev.redis.demo.lock;

import org.springframework.data.redis.core.HashOperations;

import java.util.concurrent.CountDownLatch;

/**
 * buy
 */
public class Website{
    private RedisLock r;

    public Website(RedisLock redislock){
        r=redislock;
    }

    /**
     * 乐
     * @return
     */
    public Runnable doMiaosha(){

        return new Runnable() {

            private int iCounter;

            @Override
            public void run() {
                //for(int i = 0; i < 10; i++) {
                    try {
                        r.lock(Thread.currentThread().getName(), 5);
                        iCounter++;
                        //System.out.println(System.nanoTime() + " [" + Thread.currentThread().getName() + "] iCounter = " + iCounter);
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                //}
            }
        };
    }

    /**
     *
     * @return
     */
    public Runnable doEditOrder(String orderId, String orderAttr, String attrValue, HashOperations hashOperations){

        return new Runnable() {
            @Override
            public void run() {
                try {
                    boolean tryLock = r.lock(orderId + "_lock", Thread.currentThread().getName(), "60000", 10);
                    if( tryLock ){
                        hashOperations.put(orderId, orderAttr, attrValue);
                        //hashOperations.get("orderId", "state" )
                        r.unLock(orderId + "_lock", Thread.currentThread().getName());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    r.unLock(orderId + "_lock", Thread.currentThread().getName());
                }
            }
        };
    }

    /**
     * 同时执行
     *
     * @param threadNums
     * @param task
     * @return
     * @throws InterruptedException
     */
    public long startTaskAllInOnce(int threadNums, final Runnable task) throws InterruptedException {
        final CountDownLatch startGate = new CountDownLatch(1);
        final CountDownLatch endGate = new CountDownLatch(threadNums);
        for(int i = 0; i < threadNums; i++) {
            Thread t = new Thread() {
                public void run() {
                    try {
                        // 使线程在此等待，当开始门打开时，一起涌入门中
                        startGate.await();
                        try {
                            task.run();
                        } finally {
                            // 将结束门减1，减到0时，就可以开启结束门了
                            endGate.countDown();
                        }
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
            };
            t.start();
        }
        long startTime = System.nanoTime();
        System.out.println(startTime + " [" + Thread.currentThread() + "] All thread is ready, concurrent going...");
        // 因开启门只需一个开关，所以立马就开启开始门
        startGate.countDown();
        // 等等结束门开启
        endGate.await();
        long endTime = System.nanoTime();
        System.out.println(endTime + " [" + Thread.currentThread() + "] All thread is completed.");
        return endTime - startTime;
    }

    /**
     * 同时执行
     *
     * @param tasks
     * @return
     * @throws InterruptedException
     */
    public long startTaskAllInOnce(final Runnable[] tasks) throws InterruptedException {
        final CountDownLatch startGate = new CountDownLatch(1);
        final CountDownLatch endGate = new CountDownLatch(tasks.length);
        for(Runnable task : tasks) {
            Thread t = new Thread() {
                public void run() {
                    try {
                        // 使线程在此等待，当开始门打开时，一起涌入门中
                        startGate.await();
                        try {
                            task.run();
                        } finally {
                            // 将结束门减1，减到0时，就可以开启结束门了
                            endGate.countDown();
                        }
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
            };
            t.start();
        }
        long startTime = System.nanoTime();
        System.out.println(startTime + " [" + Thread.currentThread() + "] All thread is ready, concurrent going...");
        // 因开启门只需一个开关，所以立马就开启开始门
        startGate.countDown();
        // 等等结束门开启
        endGate.await();
        long endTime = System.nanoTime();
        System.out.println(endTime + " [" + Thread.currentThread() + "] All thread is completed.");
        return endTime - startTime;
    }
}
