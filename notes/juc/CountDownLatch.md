# JUC源码分析-JUC锁（三）：CountDownLatch

## 1. 概述
- 闭锁（Latch）
闭锁（Latch）：一种同步方法，可以延迟线程的进度直到线程到达某个终点状态。通俗的讲就是，一个闭锁相当于一扇大门，在大门打开之前所有线程都被阻断，一旦大门打开所有线程都将通过，但是一旦大门打开，所有线程都通过了，那么这个闭锁的状态就失效了，门的状态也就不能变了，只能是打开状态。也就是说闭锁的状态是一次性的，它确保在闭锁打开之前所有特定的活动都需要在闭锁打开之后才能完成。

应用场景：
- 保某个计算在其需要的所有资源都被初始化之后才继续执行。二元闭锁（包括两个状态）可以用来表示“资源R已经被初始化”，而所有需要R的操作都必须先在这个闭锁上等待。
- 确保某个服务在其依赖的所有其他服务都已经启动之后才启动。
- 等待直到某个操作的所有参与者都就绪在继续执行。（例如：多人游戏中需要所有玩家准备才能开始）　

> CountDownLatch是一个同步辅助类，通过AQS实现的一个闭锁。在其他线程完成它们的操作之前，允许一个多个线程等待。简单来说，CountDownLatch中有一个锁计数，在计数到达0之前，线程会一直等待。

![img](https://upload-images.jianshu.io/upload_images/6050820-31525b28d22df2b5.png?imageMogr2/auto-orient/strip|imageView2/2/w/560/format/webp)

CountDownLatch运行机制

## 2. 数据结构和核心参数

![img](https://upload-images.jianshu.io/upload_images/6050820-b93909094116df2f.png?imageMogr2/auto-orient/strip|imageView2/2/w/524/format/webp)

CountDownLatch继承关系

从锁类别来说，CountDownLatch是一个“共享锁”，内部定义了自己的同步器Sync，Sync继承自AQS，实现了`tryAcquireShared`和`tryReleaseShared`两个方法。需要注意的是，**CountDownLatch中的锁是响应中断的，如果线程在对锁进行操作期间发生中断，会直接抛出`InterruptedException`。**

## 3. 源码解析
```java
//构造函数
public CountDownLatch(int count) {
    if (count < 0) throw new IllegalArgumentException("count < 0");
    this.sync = new Sync(count);
}
//CountDownLatch中的计数其实就是AQS的state
Sync(int count) {
    setState(count);
}
```

说明：从构造函数中可以看出，CountDownLatch的“锁计数”本质上就是AQS的资源数`state`。下面我们将通过`await()`和`countDown()`两个方法来分析CountDownLatch的“latch”实现。

### 3.1 await()

```java
public void await() throws InterruptedException {
    sync.acquireSharedInterruptibly(1);
}

//AQS中acquireSharedInterruptibly(1)的实现
public final void acquireSharedInterruptibly(int arg)
        throws InterruptedException {
    if (Thread.interrupted())
        throw new InterruptedException();
    if (tryAcquireShared(arg) < 0)
        doAcquireSharedInterruptibly(arg);
}

//tryAcquireShared在CountDownLatch中的实现
protected int tryAcquireShared(int acquires) {
    return (getState() == 0) ? 1 : -1;
}
```

**说明**:`await()`的实现非常简单，就是通过对资源`state`剩余量（`state==0 ? 1 : -1`）来判断是否获取到锁。在《AQS》篇中我们讲到过，`tryAcquireShared`函数规定了它的返回值类型：**成功获取并且还有可用资源返回正数；成功获取但是没有可用资源时返回0；获取资源失败返回一个负数。** 也就是说，只要`state!=0`，线程就进入等待队列阻塞。

### 3.2 countDown()

```java
public void countDown() {
    sync.releaseShared(1);
}

//AQS中releaseShared(1)的实现
public final boolean releaseShared(int arg) {
    if (tryReleaseShared(arg)) {
        doReleaseShared();//唤醒后续节点
        return true;
    }
    return false;
}

//tryReleaseShared在CountDownLatch中的实现
protected boolean tryReleaseShared(int releases) {
    // Decrement count; signal when transition to zero
    for (;;) {
        int c = getState();
        if (c == 0)
            return false;
        int nextc = c-1;
        if (compareAndSetState(c, nextc))
            return nextc == 0;
    }
}
```

**说明：**如果释放资源后`state==0`,说明已经到达latch，此时就可以调用`doReleaseShared`唤醒等待的线程。

# 小结

相对其他同步类来说，CountDownLatch可以说是最简单的同步类实现了。它完全依赖了AQS，只要理解了AQS，那么理解它就不成问题了。