package handwrite;

import java.util.LinkedList;
import java.util.List;

/**
 * @BelongsProject: Java-Notes
 * @BelongsPackage: PACKAGE_NAME
 * @Author: elvis
 * @CreateTime: 2020-05-10 08:17
 * @Description:　生产消费者
 */
//仓库代码 主要处理同步问题
public class Storage {
    private final int MAX_SIZE = 100;//仓库最大容量
    private List list = new LinkedList();//产品存储在这里

    public void produce(int num) {//生产num个产品
        synchronized (list) {
            //一定是while，因为wait被唤醒后需要判断是不是满足生产条件
            while(list.size()+num > MAX_SIZE) {
                System.out.println("暂时不能执行生产任务");
                try{
                    list.wait();
                } catch ( InterruptedException e) {
                    e.printStackTrace();
                }
            }
            //满足生产条件开始生产
            for(int i = 0; i < num; i++) {
                list.add(new Object());
            }
            System.out.println("已生产产品数"+num+" 仓库容量"+list.size());
            list.notifyAll();
        }
    }

    public void consume(int num) {//消费num个产品
        synchronized (list) {
            while(list.size() < num) {
                System.out.println("暂时不能执行消费任务");
                try{
                    list.wait();
                } catch ( InterruptedException e) {
                    e.printStackTrace();
                }
            }
            //满足消费条件开始消费
            for(int i = 0; i < num; i++) {
                list.remove(i);
            }
            System.out.println("已消费产品数"+num+" 仓库容量"+list.size());
            list.notifyAll();
        }
    }

    //生产者
    public static class Producer extends Thread {
        private int num;//生产的数量
        public Storage storage;//仓库
        public Producer(Storage storage) {
            this.storage = storage;
        }
        public void setNum(int num) {
            this.num = num;
        }
        public void run() {
            storage. produce(num);
        }
    }
    //消费者
    public static class Consumer extends Thread {
        private int num;//消费的数量
        public Storage storage;//仓库
        public Consumer(Storage storage) {
            this.storage = storage;
        }
        public void setNum(int num) {
            this.num = num;
        }
        public void run() {
            storage. consume(num);
        }
    }

    public static void main(String[] args) {
        Storage storage = new Storage();
        Producer p1 = new Producer(storage);
        Producer p2 = new Producer(storage);
        Producer p3 = new Producer(storage);
        Producer p4 = new Producer(storage);
        Producer p5 = new Producer(storage);

        Consumer c1 = new Consumer(storage);
        Consumer c2 = new Consumer(storage);
        Consumer c3 = new Consumer(storage);
        p1.setNum(10);
        p2.setNum(20);
        p3.setNum(10);
        p4.setNum(80);
        p5.setNum(10);
        c1.setNum(50);
        c2.setNum(20);
        c3.setNum(20);
        c1.start();
        c2.start();
        c3.start();
        p1.start();
        p2.start();
        p3.start();
        p4.start();
        p5.start();
    }
}
