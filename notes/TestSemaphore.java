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
    public static void main(String[] args) {
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
        while (true){

        }
    }
}