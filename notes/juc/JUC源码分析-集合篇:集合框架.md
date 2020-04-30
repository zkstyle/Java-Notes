> 从JDK1.5开始，Java针对集合类提供了线程安全版本的实现，接下来我们将逐个分析JUC集合类的具体实现，本篇首先介绍一下整个JUC集合类的框架。

## 1. List和Set

![img](https:////upload-images.jianshu.io/upload_images/6050820-eb04e136dc4f1541.png?imageMogr2/auto-orient/strip|imageView2/2/w/589/format/webp)

java.util.concurrent：List和Set

- **CopyOnWriteArrayList**：相当于线程安全的ArrayList，通过显式锁 ReentrantLock 实现线程安全。允许存储null值。
- **CopyOnWriteArraySet**：相当于线程安全的HashSet，内部使用 CopyOnWriteArrayList 实现。允许存储null值。
- ConcurrentSkipListSet在Map中说明

## 2. Map

![img](https:////upload-images.jianshu.io/upload_images/6050820-381a4dce355a08d8.png?imageMogr2/auto-orient/strip|imageView2/2/w/638/format/webp)

java.util.concurrent：Map

- **ConcurrentHashMap**：线程安全的HashMap（但不允许空key或value），ConcurrentHashMap在JDK1.7之前是通过Lock和segment（分段锁）实现，1.8之后改为CAS+synchronized来保证并发安全。
- **ConcurrentSkipListMap**：跳表结构的并发有序哈希表。不允许存储null值。
- **ConcurrentSkipListSet**：跳表结构的并发有序集合。内部使用 ConcurrentSkipListMap 实现。不允许存储null值。

## 3. Queue

![img](https:////upload-images.jianshu.io/upload_images/6050820-d52f71c9ff9fe1e4.png?imageMogr2/auto-orient/strip|imageView2/2/w/1200/format/webp)

java.util.concurrent：Queue

- **ArrayBlockingQueue**：**数组实现的线程安全的有界的阻塞队列**，使用Lock机制实现并发访问，队列元素使用 FIFO（先进先出）方式。
- **LinkedBlockingQueue**：**单向链表实现的（指定大小）阻塞队列**，使用Lock机制实现并发访问，队列元素使用 FIFO（先进先出）方式。
- **LinkedBlockingDeque**：**双向链表实现的（指定大小）双向并发阻塞队列**，使用Lock机制实现并发访问，该阻塞队列同时支持FIFO和FILO两种操作方式。
- **ConcurrentLinkedQueue**：**单向链表实现的无界并发队列**，通过CAS实现并发访问，队列元素使用 FIFO（先进先出）方式。
- **ConcurrentLinkedDeque**：**双向链表实现的无界并发队列**，通过CAS实现并发访问，该队列同时支持FIFO和FILO两种操作方式。
- **DelayQueue**：**延时无界阻塞队列**，使用Lock机制实现并发访问。队列里只允许放可以“延期”的元素，队列中的head是最先“到期”的元素。如果队里中没有元素到“到期”，那么就算队列中有元素也不能获取到。
- **PriorityBlockingQueue**：**无界优先级阻塞队列**，使用Lock机制实现并发访问。priorityQueue的线程安全版，不允许存放null值，依赖于comparable的排序，不允许存放不可比较的对象类型。
- **SynchronousQueue**：**没有容量的同步队列**，通过CAS实现并发访问，支持FIFO和FILO。
- **LinkedTransferQueue**：1.7新增，单向链表实现的无界阻塞队列，通过CAS实现并发访问，队列元素使用 FIFO（先进先出）方式。LinkedTransferQueue可以说是ConcurrentLinkedQueue、SynchronousQueue（公平模式）和LinkedBlockingQueue的超集, 它不仅仅综合了这几个类的功能，同时也提供了更高效的实现。

