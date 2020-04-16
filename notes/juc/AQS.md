## AQS概述

> AQS，AbstractQueuedSynchronizer，即队列同步器。它是构建锁或者其他同步组件的基础框架（如ReentrantLock、ReentrantReadWriteLock、Semaphore等），JUC并发包的作者（**Doug Lea**）期望它能够成为实现大部分同步需求的基础。它是JUC并发包中的核心基础组件。
>
> AQS解决了子实现同步器时涉及当的大量细节问题，例如获取同步状态、FIFO同步队列。基于AQS来构建同步器可以带来很多好处。它不仅能够极大地减少实现工作，而且也不必处理在多个位置上发生的竞争问题。
>
> 在基于AQS构建的同步器中，只能在一个时刻发生阻塞，从而降低上下文切换的开销，提高了吞吐量。同时在设计AQS时充分考虑了可伸缩行，因此J.U.C中所有基于AQS构建的同步器均可以获得这个优势。
>
> AQS的主要使用方式是继承，子类通过继承同步器并实现它的抽象方法来管理同步状态。

AQS支持**独占锁（Exclusive）和共享锁(Share**)两种模式：

-  独占锁：只能被一个线程获取到(`ReentrantLock`)；
-  共享锁：可以被多个线程同时获取(`CountDownLatch`、`ReadWriteLock`的读锁)。

不管是独占锁还是共享锁，本质上都是对AQS内部的一个变量state的获取，state是一个原子性的int变量，可用来表示锁状态、资源数等，如下图。

![aqs](/picture/aqs_gs.webp)

~~~ java
/**
     * The synchronization state.
     */
    private volatile int state;
~~~

AQS使用一个int类型的成员变量state来表示同步状态，当state>0时表示已经获取了锁，当state = 0时表示释放了锁。它提供了三个方法（getState()、setState(int newState)、compareAndSetState(int expect,int update)）来对同步状态state进行操作，当然AQS可以确保对state的操作是安全的。变量使用volatile修饰，表示每一次更新都会及时的刷到主存中。

## 数据结构与结构参数

**AQS的内部实现了两个队列，一个同步队列和一个条件队列**

-  **条件队列**是为Lock实现的一个基础同步器，并且一个线程可能会有多个条件队列，只有在使用了Condition才会存在条件队列。
-  **同步队列**的作用是，在线程获取资源失败后，进入同步队列队尾保持自旋等待状态， 在同步队列中的线程在自旋时会判断其前节点是否为head节点，如果为head节点则不断尝试获取资源/锁，获取成功则退出同步队列。当线程执行完逻辑后，会释放资源/锁，释放后唤醒其后继节点。

###  同步队列与条件队列的关系

首先展示同步队列与条件队列的结构图，如下：

![aqs](/picture/aqs00.png)

***同步队列节点来源：***

1. 同步队列依赖一个双向链表来完成同步状态的管理，当前线程获取同步状态失败 后，同步器会将线程构建成一个节点，并将其加入同步队列中。

2. 通过`signal`或`signalAll`将条件队列中的节点转移到同步队列。（由条件队列转化为同步队列）

***条件队列节点来源：***

1. 调用`await`方法阻塞线程； 
2. 当前线程存在于同步队列的头结点，调用`await`方法进行阻塞（从同步队列转化到条件队列）

**可总结为：** 

1. 同步队列与条件队列节点可相互转化 
2. 一个线程只能存在于两个队列中的一个

### 实例说明

1. 假设初始状态如下，节点A、节点B在同步队列中。

   ![aqs](/picture/aqs01.jpg)

2. 节点A的线程获取锁权限，此时调用`await`方法。节点A从同步队列移除， 并加入条件队列中。

   ![aqs](/picture/aqs02.jpg)

3. 调用 `signal`方法，从条件队列中取出第一个节点，并加入同步队列中，等待获取资源

![aqs](/picture/aqs03.jpg)

以上三个说明实例用图片形式解释了同步队列与条件队列节点可相互转化 。需要注意的是同步队列中，头结点`head`与尾节点`tail`被放在一个同步器中，也就是Node节点。不管是同步队列还是条件队列，**其内部都是由节点Node组成**，首先介绍下AQS的内部类Node，主要源码如下：

~~~java
static final class Node {
    /**
     * Marker to indicate a node is waiting in shared mode
     */
    static final Node SHARED = new Node();
    /**
     * Marker to indicate a node is waiting in exclusive mode
     */
    static final Node EXCLUSIVE = null;
    //取消
    static final int CANCELLED = 1;
    //等待触发
    static final int SIGNAL = -1;
    //等待条件
    static final int CONDITION = -2;
    //状态需要向后传播
    static final int PROPAGATE = -3;

/** 等待状态 */
    volatile int waitStatus;
    /** 前驱节点 */
    volatile Node prev;
    /** 后继节点 */
    volatile Node next;
   /** 获取同步状态的线程 */
    volatile Thread thread;
    Node nextWaiter;
}
~~~

说明：Node的实现很简单，就是一个普通双向链表的实现，这里主要说明一下内部的几个等待状态：

-  `CANCELLED`：值为1，当前节点由于超时或中断被取消。
-  `SIGNAL`：值为-1，表示当前节点的前节点被阻塞，当前节点在release或cancel时需要执行unpark来唤醒后继节点。
-  `CONDITION`：值为-2，当前节点正在等待Condition，这个状态在同步队列里不会被用到。
-  `PROPAGATE`：值为-3，(针对共享锁) `releaseShared()`操作需要被传递到其他节点，这个状态在`doReleaseShared`中被设置，用来保证后续节点可以获取共享资源。
-  **0**：初始状态，当前节点在sync queue中，等待获取锁。

### AQS主要提供了如下一些方法：

- `getState()`：返回同步状态的当前值；
- `setState(int newState)`：设置当前同步状态；
- `compareAndSetState(int expect, int update)`：使用CAS设置当前状态，该方法能够保证状态设置的原子性；
- `tryAcquire(int arg)`：独占式获取同步状态，获取同步状态成功后，其他线程需要等待该线程释放同步状态才能获取同步状态；
- `tryRelease(int arg)`：独占式释放同步状态；
- `tryAcquireShared(int arg)`：共享式获取同步状态，返回值大于等于0则表示获取成功，否则获取失败；
- `tryReleaseShared(int arg)`：共享式释放同步状态；
- `isHeldExclusively()`：当前同步器是否在独占式模式下被线程占用，一般该方法表示是否被当前线程所独占；
- `acquire(int arg)`：独占式获取同步状态，如果当前线程获取同步状态成功，则由该方法返回，否则，将会进入同步队列等待，该方法将会调用可重写的tryAcquire(int arg)方法；
- `acquireInterruptibly(int arg)`：与acquire(int arg)相同，但是该方法响应中断，当前线程为获取到同步状态而进入到同步队列中，如果当前线程被中断，则该方法会抛出InterruptedException异常并返回；
- `tryAcquireNanos(int arg,long nanos)`：超时获取同步状态，如果当前线程在nanos时间内没有获取到同步状态，那么将会返回false，已经获取则返回true；
- `acquireShared(int arg)`：共享式获取同步状态，如果当前线程未获取到同步状态，将会进入同步队列等待，与独占式的主要区别是在同一时刻可以有多个线程获取到同步状态；
- `acquireSharedInterruptibly(int arg)`：共享式获取同步状态，响应中断；
- `tryAcquireSharedNanos(int arg, long nanosTimeout)`：共享式获取同步状态，增加超时限制；
- `release(int arg)`：独占式释放同步状态，该方法会在释放同步状态之后，将同步队列中第一个节点包含的线程唤醒；
- `releaseShared(int arg)`：共享式释放同步状态；

## 源码分析

###  acquire(int)

~~~ java
//独占模式获取资源
public final void acquire(int arg) {
    if (!tryAcquire(arg) &&
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        selfInterrupt();
}
~~~

**说明**：独占模式下获取资源/锁，忽略中断的影响。内部主要调用了三个方法，其中tryAcquire需要自定义实现。后面会对各个方法进行详细分析。`acquire`方法流程如下：

1. `tryAcquire()` 尝试直接获取资源，如果成功则直接返回，失败进入第二步；

2. `addWaiter()` 获取资源失败后，将当前线程加入等待队列的尾部，并标记为独占模式；

3. `acquireQueued()` 使线程在等待队列中自旋等待获取资源，一直获取到资源后才返回。如果在等待过程中被中断过，则返回true，否则返回false。

4. 如果线程在等待过程中被中断(interrupt)是不响应的，在获取资源成功之后根据返回的中断状态调用`selfInterrupt()`方法再把中断状态补上。

   #### tryAcquire(int)

   ~~~ java
   protected boolean tryAcquire(int arg) {
       throw new UnsupportedOperationException();
   }
   ~~~

   **说明**：尝试获取资源，成功返回true。**具体资源获取/释放方式交由自定义同步器实现**。`ReentrantLock`中公平锁和非公平锁的实现如下:

   ```java
   //公平锁
   protected final boolean tryAcquire(int acquires) {
       final Thread current = Thread.currentThread();
       int c = getState();
       if (c == 0) {
           if (!hasQueuedPredecessors() &&
                   compareAndSetState(0, acquires)) {
               setExclusiveOwnerThread(current);
               return true;
           }
       }
       else if (current == getExclusiveOwnerThread()) {
           int nextc = c + acquires;
           if (nextc < 0)
               throw new Error("Maximum lock count exceeded");
           setState(nextc);
           return true;
       }
       return false;
   }
   //非公平锁
   final boolean nonfairTryAcquire(int acquires) {
       final Thread current = Thread.currentThread();
       int c = getState();
       if (c == 0) {
           if (compareAndSetState(0, acquires)) {
               setExclusiveOwnerThread(current);
               return true;
           }
       }
       else if (current == getExclusiveOwnerThread()) {
           int nextc = c + acquires;
           if (nextc < 0) // overflow
               throw new Error("Maximum lock count exceeded");
           setState(nextc);
           return true;
       }
       return false;
   }
   ```

   ### 3.1.2 addWaiter(Node)

   ```java
   //添加等待节点到尾部
   private Node addWaiter(Node mode) {
       Node node = new Node(Thread.currentThread(), mode);
       // Try the fast path of enq; backup to full enq on failure
       //尝试快速入队
       Node pred = tail;
       if (pred != null) {
           node.prev = pred;
           if (compareAndSetTail(pred, node)) {
               pred.next = node;
               return node;
           }
       }
       enq(node);
       return node;
   }
   //插入给定节点到队尾
   private Node enq(final Node node) {
       for (;;) {
           Node t = tail;
           if (t == null) { // Must initialize
               if (compareAndSetHead(new Node()))
                   tail = head;
           } else {
               node.prev = t;
               if (compareAndSetTail(t, node)) {
                   t.next = node;
                   return t;
               }
           }
       }
   }
   ```

   **说明**：获取独占锁失败后，将当前线程加入等待队列的尾部，并标记为独占模式。返回插入的等待节点。

   ### 3.1.3 acquireQueued(Node,int)

   ```java
   //自旋等待获取资源
   final boolean acquireQueued(final Node node, int arg) {
       boolean failed = true;
       try {
           boolean interrupted = false;
           for (;;) {
               final Node p = node.predecessor();//获取前继节点
               //前继节点为head，说明可以尝试获取资源
               if (p == head && tryAcquire(arg)) {
                   setHead(node);//获取成功，更新head节点
                   p.next = null; // help GC
                   failed = false;
                   return interrupted;
               }
               if (shouldParkAfterFailedAcquire(p, node) && //检查是否可以park
                   parkAndCheckInterrupt())
                   interrupted = true;
           }
       } finally {
           if (failed)
               cancelAcquire(node);
       }
   }
   
   //获取资源失败后，检查并更新等待状态
   private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
       int ws = pred.waitStatus;
       if (ws == Node.SIGNAL)
           /*
            * This node has already set status asking a release
            * to signal it, so it can safely park.
            */
           return true;
       if (ws > 0) {
           /*
            * Predecessor was cancelled. Skip over predecessors and
            * indicate retry.
            */
           //如果前节点取消了，那就一直往前找到一个等待状态的节点，并排在它的后边
           do {
               node.prev = pred = pred.prev;
           } while (pred.waitStatus > 0);
           pred.next = node;
       } else {
           /*
            * waitStatus must be 0 or PROPAGATE.  Indicate that we
            * need a signal, but don't park yet.  Caller will need to
            * retry to make sure it cannot acquire before parking.
            */
           //此时前节点状态为0或PROPAGATE，表示我们需要一个唤醒信号，但是不立即park,在park前调用者需要重试来确认它不能获取资源。
           compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
       }
       return false;
   }
   //阻塞当前线程，返回中断状态
   private final boolean parkAndCheckInterrupt() {
       LockSupport.park(this);
       return Thread.interrupted();
   }
   ```

   **说明**：线程进入等待队列后，在等待队列中自旋等待获取资源。如果在整个等待过程中被中断过，则返回true，否则返回false。具体流程如下：

   1. 获取当前等待节点的前继节点，如果前继节点为head，说明可以尝试获取锁；
   2. 调用`tryAcquire`获取锁，成功后更新`head`为当前节点；
   3. 获取资源失败，调用`shouldParkAfterFailedAcquire`方法检查并更新等待状态。如果前继节点状态为`SIGNAL`，说明当前节点可以进入waiting状态等待唤醒；被唤醒后，继续自旋重复上述步骤。
   4. 获取资源成功后返回中断状态。

   当前线程通过`parkAndCheckInterrupt()`阻塞之后进入waiting状态，此状态下可以通过下面两种途径唤醒线程：

   1. 前继节点释放资源后，通过`unparkSuccessor()`方法unpark当前线程；
   2. 当前线程被中断。

   ### 3.2 release(int)

   ```java
   /**独占模式释放资源*/
   public final boolean release(int arg) {
       if (tryRelease(arg)) {//尝试释放资源
           Node h = head;//头结点
           if (h != null && h.waitStatus != 0)
               unparkSuccessor(h);//唤醒head的下一个节点
           return true;
       }
       return false;
   }
   ```

   **说明**：独占模式下释放指定量的资源，成功释放后调用`unparkSuccessor`唤醒head的下一个节点。

   ### 3.2.1 tryRelease(int)

   ```java
   protected boolean tryRelease(int arg) {
       throw new UnsupportedOperationException();
   }
   ```

   **说明**：和`tryAcquire()`一样，这个方法也需要自定义同步器去实现。一般来说，释放资源直接拿`state`减去给定的参数`arg`，释放后state==0说明释放成功。在`ReentrantLock`中实现如下：

   ```java
   protected final boolean tryRelease(int releases) {
       int c = getState() - releases;
       if (Thread.currentThread() != getExclusiveOwnerThread())
           throw new IllegalMonitorStateException();
       boolean free = false;
       if (c == 0) {
           free = true;
           setExclusiveOwnerThread(null);//设置独占锁持有线程为null
       }
       setState(c);
       return free;
   }
   ```

   ### 3.2.2 unparkSuccessor(Node)

   ```java
   private void unparkSuccessor(Node node) {
       int ws = node.waitStatus;
       if (ws < 0)//当前节点没有被取消,更新waitStatus为0。
           compareAndSetWaitStatus(node, ws, 0);
   
       Node s = node.next;//找到下一个需要唤醒的结点
       if (s == null || s.waitStatus > 0) {
           s = null;
           //next节点为空，从tail节点开始向前查找有效节点
           for (Node t = tail; t != null && t != node; t = t.prev)
               if (t.waitStatus <= 0)
                   s = t;
       }
       if (s != null)
           LockSupport.unpark(s.thread);
   }
   ```

   **说明**：成功获取到资源后，调用此方法唤醒head的下一个节点。因为当前节点已经释放掉资源，下一个等待的线程可以被唤醒继续获取资源。

   ## 3.3  acquireShared(int)

   ```java
   public final void acquireShared(int arg) {
       if (tryAcquireShared(arg) < 0)
           doAcquireShared(arg);
   }
   ```

   **说明**：共享模式下获取资源/锁，忽略中断的影响。内部主要调用了两个个方法，其中`tryAcquireShared`需要自定义同步器实现。后面会对各个方法进行详细分析。`acquireShared`方法流程如下：

   1.  `tryAcquireShared(arg)` 尝试获取共享资源。**成功获取并且还有可用资源返回正数；成功获取但是没有可用资源时返回0；获取资源失败返回一个负数。** 
   2.  获取资源失败后调用`doAcquireShared`方法进入等待队列，获取资源后返回。

   ### 3.3.1 tryAcquireShared(int arg)

   ```java
   /**共享模式下获取资源*/
   protected int tryAcquireShared(int arg) {
       throw new UnsupportedOperationException();
   }
   ```

   **说明**：尝试获取共享资源，需同步器自定义实现。有三个类型的返回值：

   - 正数：成功获取资源，并且还有剩余可用资源，可以唤醒下一个等待线程；
   - 负数：获取资源失败，准备进入等待队列；
   - 0：获取资源成功，但没有剩余可用资源。

   ### 3.3.2 doAcquireShared(int)

   ```java
   //获取共享锁
   private void doAcquireShared(int arg) {
       final Node node = addWaiter(Node.SHARED);//添加一个共享模式Node到队列尾
       boolean failed = true;
       try {
           boolean interrupted = false;
           for (;;) {
               final Node p = node.predecessor();//获取前节点
               if (p == head) {
                   int r = tryAcquireShared(arg);//前节点为head，尝试获取资源
                   if (r >= 0) {
                       //获取资源成功，设置head为自己，如果有剩余资源可以在唤醒之后的线程
                       setHeadAndPropagate(node, r);
                       p.next = null; // help GC
                       if (interrupted)
                           selfInterrupt();
                       failed = false;
                       return;
                   }
               }
               if (shouldParkAfterFailedAcquire(p, node) &&  //检查获取失败后是否可以阻塞
                   parkAndCheckInterrupt())
                   interrupted = true;
           }
       } finally {
           if (failed)
               cancelAcquire(node);
       }
   }
   ```

   **说明**：在`tryAcquireShared`中获取资源失败后，将当前线程加入等待队列尾部等待唤醒，成功获取资源后返回。在阻塞结束后成功获取到资源时，如果还有剩余资源，就调用`setHeadAndPropagate`方法继续唤醒之后的线程，源码如下：

   ```java
   //设置head，如果有剩余资源可以再唤醒之后的线程
   private void setHeadAndPropagate(Node node, int propagate) {
       Node h = head; // Record old head for check below
       setHead(node);
       /*
        * 如果满足下列条件可以尝试唤醒下一个节点：
        *  调用者指定参数(propagate>0)，并且后继节点正在等待或后继节点为空
        */
       if (propagate > 0 || h == null || h.waitStatus < 0 ||
           (h = head) == null || h.waitStatus < 0) {
           Node s = node.next;
           if (s == null || s.isShared())
               doReleaseShared();
       }
   }
   ```

   ## 3.4  releaseShared(int)

   ```java
   /**共享模式释放资源*/
   public final boolean releaseShared(int arg) {
       if (tryReleaseShared(arg)) {
           doReleaseShared();//释放锁，并唤醒后继节点
           return true;
       }
       return false;
   }
   ```

   **说明**：共享模式下释放给定量的资源，如果成功释放，唤醒等待队列的后继节点。`tryReleaseShared`需要自定义同步器去实现。方法执行流程：`tryReleaseShared(int)`尝试释放给定量的资源，成功释放后调用`doReleaseShared()`唤醒后继线程。

   ### 3.4.1 tryReleaseShared(int)

   ```java
   /**共享模式释放资源*/
   protected boolean tryReleaseShared(int arg) {
       throw new UnsupportedOperationException();
   }
   ```

   **说明：**释放给定量的资源，需自定义同步器实现。释放后如果允许后继等待线程获取资源返回true。

   ### 3.4.2 doReleaseShared(int)

   ```java
   //释放共享资源-唤醒后继线程并保证后继节点的资源传播
   private void doReleaseShared() {
       //自旋，确保释放后唤醒后继节点
       for (;;) {
           Node h = head;
           if (h != null && h != tail) {
               int ws = h.waitStatus;
               if (ws == Node.SIGNAL) {
                   if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                       continue;            // loop to recheck cases
                   unparkSuccessor(h);//唤醒后继节点
               }
               else if (ws == 0 &&
                        !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))  //waitStatus为0，CAS修改为PROPAGATE
                   continue;                // loop on failed CAS
           }
           if (h == head)                   // loop if head changed
               break;
       }
   }
   ```

   **说明**：在`tryReleaseShared`成功释放资源后，调用此方法唤醒后继线程并保证后继节点的release传播（通过设置head节点的`waitStatus`为`PROPAGATE`）。

   ## 小结

   自此，AQS的主要方法就讲完了，有几个没有讲到的方法如`tryAcquireNanos`、`tryAcquireSharedNanos`，都是带等待时间的资源获取方法，还有`acquireInterruptibly` `acquireSharedInterruptibly`,响应中断式资源获取方法。都比较简单，同学们可以参考本篇源码阅读。

   