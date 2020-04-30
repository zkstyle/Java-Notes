# 1.概述

> CyclicBarrier是一个同步辅助类，允许一组线程互相等待，直到到达某个公共屏障点 (common barrier point)。如果一个程序中有固定的线程数，并且线程之间需要相互等待，这时候CyclicBarrier是一个很好的选择。之所以叫它cyclic，是因为在释放等待线程之后，它可以被重用。

![img](https:////upload-images.jianshu.io/upload_images/6050820-c89ef6dc60470065.png?imageMogr2/auto-orient/strip|imageView2/2/w/527/format/webp)

CyclicBarrier运行机制

**CountDownLatch和CyclicBarrier的区别：**

1. CountDownLatch的作用是允许1或N个线程等待其他线程完成执行；而CyclicBarrier则是允许N个线程相互等待。
2. CountDownLatch的计数器无法被重置；CyclicBarrier的计数器可以被重置后使用，因此它被称为是循环的barrier。

# 2. 函数列表和核心参数

```java
//-------------------------核心参数------------------------------
// 内部类
private static class Generation {
    boolean broken = false;
}
/** 守护barrier入口的锁 */
private final ReentrantLock lock = new ReentrantLock();
/** 等待条件，直到所有线程到达barrier */
private final Condition trip = lock.newCondition();
/** 要屏障的线程数 */
private final int parties;
/* 当线程都到达barrier，运行的 barrierCommand*/
private final Runnable barrierCommand;
/** The current generation */
private Generation generation = new Generation();
//等待到达barrier的参与线程数量，count=0 -> tripped
private int count;

//-------------------------函数列表------------------------------
//构造函数，指定参与线程数
public CyclicBarrier(int parties)
//构造函数，指定参与线程数，并在所有线程到达barrier之后执行给定的barrierAction逻辑
public CyclicBarrier(int parties, Runnable barrierAction);
//等待所有的参与者到达barrier
public int await();
//等待所有的参与者到达barrier，或等待给定的时间
public int await(long timeout, TimeUnit unit);
//获取参与等待到达barrier的线程数
public int getParties();
//查询barrier是否处于broken状态
public boolean isBroken();
//重置barrier为初始状态
public void reset();
//返回等待barrier的线程数量
public int getNumberWaiting();
```

1. **Generation：**每个使用中的barrier都表示为一个`generation`实例。当barrier触发trip条件或重置时`generation`随之改变。使用barrier时有很多`generation`与线程关联，由于不确定性的方式，锁可能分配给等待的线程。但是在同一时间只有一个是活跃的`generation`(通过`count`变量确定)，并且其余的要么被销毁，要么被trip条件等待。如果有一个中断，但没有随后的重置，就不需要有活跃的`generation`。**`CyclicBarrier`的可重用特性就是通过`Generation`来实现，每一次触发tripped都会new一个新的Generation**。
2. **barrierCommand：**`CyclicBarrier`的另一个特性是在所有参与线程到达barrier触发一个自定义函数，这个函数就是`barrierCommand`，在`CyclicBarrier`的构造函数中初始化。

# 3. 源码解析

## 3.1 构造方法

```java
public CyclicBarrier(int parties, Runnable barrierAction) {
    if (parties <= 0) throw new IllegalArgumentException();
    this.parties = parties;
    this.count = parties;
    this.barrierCommand = barrierAction;
}

public CyclicBarrier(int parties) {
    this(parties, null);
}
```

1. 默认的构造方法是CyclicBarrier(int parties)，其参数表示屏障拦截的线程数量，每个线程调用await方法告诉CyclicBarrier已经到达屏障位置，线程被阻塞。
2. 另外一个构造方法CyclicBarrier(int parties, Runnable barrierAction)，其中barrierAction任务会在所有线程到达屏障后执行。

## 3.2 await()

最主要的方法就是await()方法，调用await()的线程会等待直到有足够数量的线程调用await——也就是开闸状态。

```java
public int await() throws InterruptedException, BrokenBarrierException {
    try {
        return dowait(false, 0L);
    } catch (TimeoutException toe) {
        throw new Error(toe); // cannot happen
    }
}

public int await(long timeout, TimeUnit unit)
        throws InterruptedException,
        BrokenBarrierException,
        TimeoutException {
    return dowait(true, unit.toNanos(timeout));
}
```

await()和await(long, TimeUnit)都是调用dowait方法，区别就是参数不同，我们来看看dowait方法。

```java
private int dowait(boolean timed, long nanos)
        throws InterruptedException, BrokenBarrierException,
        TimeoutException {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        final Generation g = generation;

        if (g.broken)   // 如果当前Generation是处于打破状态则传播这个BrokenBarrierExcption
            throw new BrokenBarrierException();

        if (Thread.interrupted()) {
            // 如果当前线程被中断则使得当前generation处于打破状态，重置剩余count。
            // 并且唤醒状态变量。这时候其他线程会传播BrokenBarrierException。
            breakBarrier();
            throw new InterruptedException();
        }

        int index = --count;    // 尝试降低当前count
        /**
         * 如果当前状态将为0，则Generation处于开闸状态。运行可能存在的command，
         * 设置下一个Generation。相当于每次开闸之后都进行了一次reset。
         */
        if (index == 0) {  // tripped
            boolean ranAction = false;
            try {
                final Runnable command = barrierCommand;
                if (command != null)
                    command.run();
                ranAction = true;
                nextGeneration();
                return 0;
            } finally {
                if (!ranAction) // 如果运行command失败也会导致当前屏障被打破。
                    breakBarrier();
            }
        }

        // loop until tripped, broken, interrupted, or timed out
        for (;;) {
            try {
                if (!timed) // 阻塞在当前的状态变量。
                    trip.await();
                else if (nanos > 0L)
                    nanos = trip.awaitNanos(nanos);
            } catch (InterruptedException ie) {
                if (g == generation && ! g.broken) {    // 如果当前线程被中断了则使得屏障被打破。并抛出异常。
                    breakBarrier();
                    throw ie;
                } else {
                    Thread.currentThread().interrupt();
                }
            }

            // 从阻塞恢复之后，需要重新判断当前的状态。
            if (g.broken)
                throw new BrokenBarrierException();

            if (g != generation)
                return index;

            if (timed && nanos <= 0L) {
                breakBarrier();
                throw new TimeoutException();
            }
        }
    } finally {
        lock.unlock();
    }
}
```

**说明：**`dowait()`是`await()`的实现函数，它的作用就是让当前线程阻塞，直到“有parties个线程到达barrier” 或 “当前线程被中断” 或 “超时”这3者之一发生，当前线程才继续执行。当所有parties到达barrier（`count=0`），如果`barrierCommand`不为空，则执行`barrierCommand`。然后调用`nextGeneration()`进行换代操作。
 在`for(;;)`自旋中。`timed`是用来表示当前是不是“超时等待”线程。如果不是，则通过`trip.await()`进行等待；否则，调用`awaitNanos()`进行超时等待。



此外再看下两个小过程：

这两个小过程当然是需要锁的，但是由于这两个方法只是通过其他方法调用，所以依然是在持有锁的范围内运行的。这两个方法都是对域进行操作。

nextGeneration实际上在屏障开闸之后重置状态。以待下一次调用。
 breakBarrier实际上是在屏障打破之后设定打破状态，以唤醒其他线程并通知。

```java
private void nextGeneration() {
    trip.signalAll();
    count = parties;
    generation = new Generation();
}

private void breakBarrier() {
    generation.broken = true;
    count = parties;
    trip.signalAll();
}
```

## 3.4、reset

reset方法比较简单。但是这里还是要注意一下要先打破当前屏蔽，然后再重建一个新的屏蔽。否则的话可能会导致信号丢失。

```java
public void reset() {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        breakBarrier();   // break the current generation
        nextGeneration(); // start a new generation
    } finally {
        lock.unlock();
    }
}
```

# 小结

`CyclicBarrier`主要通过独占锁`ReentrantLock`和`Condition`配合实现。类本身实现很简单，重点是分清`CyclicBarrier`和`CountDownLatch`的用法及区别，还有在jdk1.7新增的另外一个与它们相似的同步锁`Phaser`，在后面文章中会详细讲解。