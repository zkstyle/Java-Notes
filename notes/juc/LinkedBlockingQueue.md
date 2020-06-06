# JUC源码分析-集合篇(九):LinkedBlockingQueue

> LinkedBlockingQueue 是**单向链表结构的自定义容量的阻塞队列**，元素操作按照**FIFO(first-in-first-out 先入先出)**的顺序，使用显式锁 ReentrantLock 和 Condition 来保证线程安全。链表结构的队列通常比基于数组的队列（ArrayBlockingQueue）有更高的吞吐量，但是在并发环境下性能却不如数组队列。因为比较简单，本章本来是不在笔者的写作范围内的，但是在后面的线程池源码中用到了LinkedBlockingQueue，我们我们就来简单看一下，加深一下印象。

本章应该是队列篇的终章了，还有LinkedBlockingDeque、ArrayBlockingQueue这些比较简单的队列就不再讲解了，后面我们会开始线程池相关源码分析。

------

# 概述

> LinkedBlockingQueue（后称LBQ）队列容量可通过参数来自定义，并且内部是不会自动扩容的。如果未指定容量，将取最大容量`Integer.MAX_VALUE`。 如果你理解了前几篇我们所讲的队列，那么你会发现 LBQ 非常容易理解，内部没有太多复杂的算法，数据结构也是使用了简单的链表结构。

------

# 数据结构

![img](https://upload-images.jianshu.io/upload_images/6050820-1abdfd11c930afd5.png?imageMogr2/auto-orient/strip|imageView2/2/w/581/format/webp)

LinkedBlockingQueue 继承关系

标准的队列继承关系，不多赘述。

##### 重要属性

```java
//容量
private final int capacity;

//元素个数
private final AtomicInteger count = new AtomicInteger();

//链表头
transient Node<E> head;

//链表尾
private transient Node<E> last;

//出列锁
private final ReentrantLock takeLock = new ReentrantLock();

//等待获取(出队)条件
private final Condition notEmpty = takeLock.newCondition();

//入列锁
private final ReentrantLock putLock = new ReentrantLock();

//等待插入(入列)条件
private final Condition notFull = putLock.newCondition();
```

LBQ 在实现多线程对竞争资源的互斥访问时，对于入列和出列操作分别使用了不同的锁。对于入列操作，通过`putLock`进行同步；对于出列操作，通过`takeLock`进行同步。
此外，插入锁`putLock`和出列条件`notFull`相关联，出列锁`takeLock`和出列条件`notEmpty`相关联。通过`notFull`和`notEmpty`更细腻的控制锁。

- 若某线程(线程A)要取出数据时，队列正好为空，则该线程会执行`notEmpty.await()`进行等待；当其它某个线程(线程B)向队列中插入了数据之后，会调用`notEmpty.signal()`唤醒`notEmpty`上的等待线程。此时，线程A会被唤醒从而得以继续运行。 此外，线程A在执行取操作前，会获取`takeLock`，在取操作执行完毕再释放`takeLock`。
- 若某线程(线程H)要插入数据时，队列已满，则该线程会它执行`notFull.await()`进行等待；当其它某个线程(线程I)取出数据之后，会调用`notFull.signal()`唤醒`notFull`上的等待线程。此时，线程H就会被唤醒从而得以继续运行。 此外，线程H在执行插入操作前，会获取`putLock`，在插入操作执行完毕才释放`putLock`。

------

# 源码解析

## put(E e)

LBQ 的添加元素的方法有`offer()、put()`，`put`是在队列已满的情况下等待，而`offer`则直接返回结果，它们内部操作都一致。所这里我们只对`put`进行解析



```java
//尾部插入节点,队列满时会一直等待可用,响应中断
public void put(E e) throws InterruptedException {
    if (e == null) throw new NullPointerException();
 
    int c = -1;
    Node<E> node = new Node<E>(e);
    final ReentrantLock putLock = this.putLock;//获取入列锁
    final AtomicInteger count = this.count;//获取元素数
    putLock.lockInterruptibly();//响应中断式加锁
    try {
       
        while (count.get() == capacity) {
            notFull.await();//队列已满，等待
        }
        enqueue(node);//节点添加到队列尾
        c = count.getAndIncrement();
        if (c + 1 < capacity)
            notFull.signal();
    } finally {
        putLock.unlock();
    }
    if (c == 0)
        signalNotEmpty();
}
```

**说明**：看源码吧。

## poll()

LBQ 的获取元素的方法有`poll()、take()、peek()`，`take`在队列为空的情况下会一直等待，`poll`不等待直接返回结果，`peek`是获取但不移除头结点元素，内部操作都差不多。这里我们只对`take`进行解析：



```swift
/**获取并消除头节点,会一直等待队列可用,响应中断*/
public E take() throws InterruptedException {
    E x;
    int c = -1;
    final AtomicInteger count = this.count;
    final ReentrantLock takeLock = this.takeLock;//获取出列锁
    takeLock.lockInterruptibly();//响应中断式加锁
    try {
        while (count.get() == 0) {
            notEmpty.await();//队列为空，等待
        }
        x = dequeue();//首节点出列
        c = count.getAndDecrement();
        if (c > 1)
            notEmpty.signal();
    } finally {
        takeLock.unlock();
    }
    if (c == capacity)
        signalNotFull();
    return x;
}
```

# 小结

本章比较简单，只是为了加深同学们的印象，为之后线程池源码解析做准备，随便看看就行了。