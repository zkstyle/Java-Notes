package codedemo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.Semaphore;

/**
 * @BelongsProject: Java-Notes
 * @BelongsPackage: PACKAGE_NAME
 * @Author: elvis
 * @CreateTime: 2020-05-06 16:16
 * @Description:
 */

public class TestSemaphore {


    static class Worker extends Thread{
        private int num;
        private Semaphore semaphore;
        public Worker(int num,Semaphore semaphore){
            this.num = num;
            this.semaphore = semaphore;
        }
        @Override
        public void run() {
            try {
                // 抢许可
                semaphore.acquire();
                Thread.sleep(2000);
                // 释放许可
                semaphore.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    /*public static void main(String[] args) {
        // 机器数目，即5个许可
        Semaphore semaphore = new Semaphore(5);
        // 8个线程去抢许可
        for (int i = 0; i < 8; i++){
            new Worker(i,semaphore).start();
        }
        //返回Java虚拟机中的内存总量
        long totalMemory = Runtime.getRuntime().totalMemory();
//返回Java虚拟机中试图使用的最大内存总量
        long maxMemory = Runtime.getRuntime().maxMemory();
        System.out.println("Total_Memory(-Xms)="+totalMemory+"(字节)、"+(totalMemory/(double)1024/1024)+"MB");
        System.out.println("Max_Memory(-Xmx) = "+maxMemory+" (字节)、"+(maxMemory/(double)1024/1024)+"MB");

    }*/

    public static void main(String[] args) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            list.add((int) (Math.random() * 100));
        }

        PriorityQueue<Integer> queue = new PriorityQueue<>();
        System.out.println("开始往PriorityQueue添加元素");
        for (int i = 0; i < list.size(); i++) {
            System.out.print("添加元素：" + list.get(i) + "--");
            queue.add(list.get(i));

            System.out.print("-- PriorityQueue中数组queue = ");
            Iterator<Integer> temp = queue.iterator();
            while (temp.hasNext()) {
                System.out.print(temp.next() + "--");
            }
            System.out.println();
        }

        System.out.println();

        System.out.println("依次poll() PriorityQueue中的元素");
        int size = queue.size();
        for (int i = 0; i < size; i++) {
            System.out.print("queue.poll() = " + queue.poll() + "-- PriorityQueue中数组queue = ");

            Iterator<Integer> temp = queue.iterator();
            while (temp.hasNext()) {
                System.out.print(temp.next() + "--");
            }
            System.out.println();
        }
    }

}