> ConcurrentHashMap 是一个支持并发检索和并发更新的线程安全的HashMap（但不允许空key或value）。不管是在实际工作或者是面试中，ConcurrentHashMap 都是在整个JUC集合框架里出现频率最高的一个类，所以，对ConcurrentHashMap有一个深入的认识对我们自身还是非常重要的。本章我们来从源码层面详细分析 ConcurrentHashMap 的实现（基于JDK 8），希望对大家有所帮助。

# 1. 概述

> ConcurrentHashMap 在JDK 7之前是通过Lock和Segment（分段锁）实现并发安全，JDK 8之后改为CAS+synchronized来保证并发安全（为了序列化兼容，JDK 8的代码中还是保留了Segment的部分代码）。由于笔者没有过多研究过JDK 7的源码，所以我们后面的分析主要针对JDK 8。

首先来看一下ConcurrentHashMap、HashMap和HashTable的区别：

- HashMap 是非线程安全的哈希表，常用于单线程程序中。
- Hashtable 是线程安全的哈希表，由于是通过内置锁 synchronized 来保证线程安全，在资源争用比较高的环境下，Hashtable 的效率比较低。
- ConcurrentHashMap 是一个支持并发操作的线程安全的HashMap，但是他不允许存储空key或value。使用CAS+synchronized来保证并发安全，在并发访问时不需要阻塞线程，所以效率是比Hashtable 要高的。

# 2. 数据结构

![img](https:////upload-images.jianshu.io/upload_images/6050820-225eef139360d82d.png?imageMogr2/auto-orient/strip|imageView2/2/w/727/format/webp)

ConcurrentHashMap数据结构

ConcurrentHashMap 是通过Node来存储K-V的，从上图可以看出，它的内部有很多Node节点（在内部封装了一个Node数组-`table`），不同的节点有不同的作用。下面我们来详细看一下这几个Node节点类：

![img](https:////upload-images.jianshu.io/upload_images/6050820-0100f88e72b6ae94.png?imageMogr2/auto-orient/strip|imageView2/2/w/1045/format/webp)

Node

1. **Node：** 保存k-v、k的`hash`值和链表的 next 节点引用，其中 V 和 next 用`volatile`修饰，保证多线程环境下的可见性。
2. **TreeNode：** 红黑树节点类,当链表长度>=8且数组长度>=64时，Node 会转为 TreeNode，但它不是直接转为红黑树，而是把这些 TreeNode 节点放入TreeBin 对象中，由 TreeBin 完成红黑树的封装。
3. **TreeBin：** 封装了 TreeNode，红黑树的根节点，也就是说在 ConcurrentHashMap 中红黑树存储的是 TreeBin 对象。
4. **ForwardingNode：** 在节点转移时用于连接两个 table（table和nextTable）的节点类。包含一个 nextTable 指针，用于指向下一个table。而且这个节点的 k-v 和 next 指针全部为 null，hash 值为-1。只有在扩容时发挥作用，作为一个占位节点放在 table 中表示当前节点已经被移动。
5. **ReservationNode：** 在`computeIfAbsent`和`compute`方法计算时当做一个占位节点，表示当前节点已经被占用，在`compute`或`computeIfAbsent`的 function 计算完成后插入元素。hash值为-3。

## 2.1 核心参数



```java
//最大容量
private static final int MAXIMUM_CAPACITY = 1 << 30;
//初始容量
private static final int DEFAULT_CAPACITY = 16;
//数组最大容量
static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
//默认并发度，兼容1.7及之前版本
private static final int DEFAULT_CONCURRENCY_LEVEL = 16;
//加载/扩容因子，实际使用n - (n >>> 2)
private static final float LOAD_FACTOR = 0.75f;
//链表转红黑树的节点数阀值
static final int TREEIFY_THRESHOLD = 8;
//红黑树转链表的节点数阀值
static final int UNTREEIFY_THRESHOLD = 6;
//当数组长度还未超过64,优先数组的扩容,否则将链表转为红黑树
static final int MIN_TREEIFY_CAPACITY = 64;
//扩容时任务的最小转移节点数
private static final int MIN_TRANSFER_STRIDE = 16;
//sizeCtl中记录stamp的位数
private static int RESIZE_STAMP_BITS = 16;
//帮助扩容的最大线程数
private static final int MAX_RESIZERS = (1 << (32 - RESIZE_STAMP_BITS)) - 1;
//size在sizeCtl中的偏移量
private static final int RESIZE_STAMP_SHIFT = 32 - RESIZE_STAMP_BITS;

//存放Node元素的数组,在第一次插入数据时初始化
transient volatile Node<K,V>[] table;
//一个过渡的table表,只有在扩容的时候才会使用
private transient volatile Node<K,V>[] nextTable;
//基础计数器值(size = baseCount + CounterCell[i].value)
private transient volatile long baseCount;
//控制table初始化和扩容操作
private transient volatile int sizeCtl;
//节点转移时下一个需要转移的table索引
private transient volatile int transferIndex;
//元素变化时用于控制自旋
private transient volatile int cellsBusy;
// 保存table中的每个节点的元素个数 2的幂次方
// size = baseCount + CounterCell[i].value
private transient volatile CounterCell[] counterCells;
```

**table**：Node数组，在第一次插入元素的时候初始化，默认初始大小为16，用来存储Node节点数据，扩容时大小总是2的幂次方。

**nextTable**：默认为null，扩容时生成的新的数组，其大小为原数组的两倍。

**sizeCtl**  ：默认为0，用来控制table的初始化和扩容操作，在不同的情况下有不同的涵义：

- -1 代表table正在初始化
- -N 表示有N-1个线程正在进行扩容操作
- 初始化数组或扩容完成后,将`sizeCtl`的值设为0.75*n
- 在扩容操作在进行节点转移前，`sizeCtl`改为`(hash << RESIZE_STAMP_SHIFT) + 2`，这个值为负数，并且每有一个线程参与扩容操作`sizeCtl`就加1

**transferIndex**：扩容时用到，初始时为`table.length`，表示从索引 0 到`transferIndex`的节点还未转移 。

**counterCells**： ConcurrentHashMap的特定计数器，实现方法跟`LongAdder`类似。这个计数器的机制避免了在更新时的资源争用，但是如果并发读取太频繁会导致缓存超负荷，为了避免读取太频繁，只有在添加了两个以上节点时才可以尝试扩容操作。在统一hash分配的前提下，发生这种情况的概率在13%左右，也就是说只有大约1/8的put操作才会检查扩容（并且在扩容后会更少）。

**hash计算公式**：`hash = (key.hashCode ^ (key.hashCode >>> 16)) & HASH_BITS`
 **索引计算公式**：`(table.length-1)&hash`

# 3. 源码解析

## 3.1 put(K, V)



```csharp
public V put(K key, V value) {
    return putVal(key, value, false);
}

/** Implementation for put and putIfAbsent */
final V putVal(K key, V value, boolean onlyIfAbsent) {
    if (key == null || value == null) throw new NullPointerException();
    //计算hash值
    int hash = spread(key.hashCode());
    int binCount = 0;
    for (Node<K,V>[] tab = table;;) {//自旋
        //f:索引节点; n:tab.length; i:新节点索引 (n - 1) & hash; fh:f.hash
        Node<K,V> f; int n, i, fh;
        if (tab == null || (n = tab.length) == 0)
            //初始化
            tab = initTable();
        else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {//索引i节点为空，直接插入
            //cas插入节点,成功则跳出循环
            if (casTabAt(tab, i, null,
                         new Node<K,V>(hash, key, value, null)))
                break;                   // no lock when adding to empty bin
        }
        //当前节点处于移动状态-其他线程正在进行节点转移操作
        else if ((fh = f.hash) == MOVED)
            //帮助转移
            tab = helpTransfer(tab, f);
        else {
            V oldVal = null;
            synchronized (f) {
                if (tabAt(tab, i) == f) {//check stable
                    //f.hash>=0,说明f是链表的头结点
                    if (fh >= 0) {
                        binCount = 1;//记录链表节点数，用于后面是否转换为红黑树做判断
                        for (Node<K,V> e = f;; ++binCount) {
                            K ek;
                            //key相同 修改
                            if (e.hash == hash &&
                                ((ek = e.key) == key ||
                                 (ek != null && key.equals(ek)))) {
                                oldVal = e.val;
                                if (!onlyIfAbsent)
                                    e.val = value;
                                break;
                            }
                            Node<K,V> pred = e;
                            //到这里说明已经是链表尾，把当前值作为新的节点插入到队尾
                            if ((e = e.next) == null) {
                                pred.next = new Node<K,V>(hash, key,
                                                          value, null);
                                break;
                            }
                        }
                    }
                    //红黑树节点操作
                    else if (f instanceof TreeBin) {
                        Node<K,V> p;
                        binCount = 2;
                        if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key,
                                                       value)) != null) {
                            oldVal = p.val;
                            if (!onlyIfAbsent)
                                p.val = value;
                        }
                    }
                }
            }
            if (binCount != 0) {
                //如果链表中节点数binCount >= TREEIFY_THRESHOLD(默认是8)，则把链表转化为红黑树结构
                if (binCount >= TREEIFY_THRESHOLD)
                    treeifyBin(tab, i);
                if (oldVal != null)
                    return oldVal;
                break;
            }
        }
    }
    //更新新元素个数
    addCount(1L, binCount);
    return null;
}
```

**说明：**在 ConcurrentHashMap 中，put 方法几乎涵盖了所有内部的函数操作。所以，我们将从put函数开始逐步向下分析。
 首先说一下put的流程，后面再详细分析每一个流程的具体实现（阅读时请结合源码）：

1. 计算当前key的hash值，根据hash值计算索引 i `（i=(table.length - 1) & hash）`；
2. 如果当前`table`为null，说明是第一次进行put操作，调用`initTable()`初始化`table`；
3. 如果索引 i 位置的节点 f 为空，则直接把当前值作为新的节点直接插入到索引 i 位置；
4. 如果节点 f 的`hash`为-1（`f.hash == MOVED(-1)`），说明当前节点处于移动状态（或者说是其他线程正在对 f 节点进行转移/扩容操作），此时调用`helpTransfer(tab, f)`帮助转移/扩容；
5. 如果不属于上述条件，说明已经有元素存储到索引 i 处，此时需要对索引 i 处的节点 f 进行 put or update 操作，首先使用内置锁 `synchronized` 对节点 f 进行加锁：

- 如果`f.hash>=0`，说明 i 位置是一个链表，并且节点 f 是这个链表的头节点，则对 f 节点进行遍历，此时分两种情况：
   --如果链表中某个节点e的hash与当前key的hash相同，则对这个节点e的value进行修改操作。
   --如果遍历到链表尾都没有找到与当前key的hash相同的节点，则把当前K-V作为一个新的节点插入到这个链表尾部。
- 如果节点 f 是`TreeBin`节点(`f instanceof TreeBin`)，说明索引 i 位置的节点是一个红黑树，则调用`putTreeVal`方法找到一个已存在的节点进行修改，或者是把当前K-V放入一个新的节点（put or update）。

1. 完成插入后，如果索引 i 处是一个链表，并且在插入新的节点后节点数>8，则调用`treeifyBin`把链表转换为红黑树。
2. 最后，调用`addCount`更新元素数量

### 3.1.1 initTable()



```csharp
/**
 * Initializes table, using the size recorded in sizeCtl.
 */
private final Node<K,V>[] initTable() {
    Node<K,V>[] tab; int sc;
    while ((tab = table) == null || tab.length == 0) {
        if ((sc = sizeCtl) < 0)//其他线程正在进行初始化或转移操作，让出CPU执行时间片，继续自旋
            Thread.yield(); // lost initialization race; just spin
        else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {//CAS设置sizectl为-1 表示当前线程正在进行初始化
            try {
                if ((tab = table) == null || tab.length == 0) {
                    int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
                    @SuppressWarnings("unchecked")
                    Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                    table = tab = nt;
                    sc = n - (n >>> 2);//0.75*n 设置扩容阈值
                }
            } finally {
                sizeCtl = sc;//初始化sizeCtl=0.75*n
            }
            break;
        }
    }
    return tab;
}
```

**说明：**初始化操作，ConcurrentHashMap的初始化在第一次插入数据的时候(判断table是否为null)，注意初始化操作为单线程操作（如果有其他线程正在进行初始化，则调用Thread.yield()让出CPU时间片，自旋等待table初始完成）。

### 3.1.2 helpTransfer(Node<K,V>[], Node<K,V>)



```kotlin
//帮助其他线程进行转移操作
final Node<K,V>[] helpTransfer(Node<K,V>[] tab, Node<K,V> f) {
    Node<K,V>[] nextTab; int sc;
    if (tab != null && (f instanceof ForwardingNode) &&
        (nextTab = ((ForwardingNode<K,V>)f).nextTable) != null) {
        //计算操作栈校验码
        int rs = resizeStamp(tab.length);
        while (nextTab == nextTable && table == tab &&
               (sc = sizeCtl) < 0) {
            if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                sc == rs + MAX_RESIZERS || transferIndex <= 0)//不需要帮助转移，跳出
                break;
            if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1)) {//CAS更新帮助转移的线程数
                transfer(tab, nextTab);
                break;
            }
        }
        return nextTab;
    }
    return table;
}
```

**说明：** 如果索引到的节点的 hash 为-1，说明当前节点处于移动状态（或者说是其他线程正在对 f 节点进行转移操作。这里主要是靠 ForwardingNode 节点来检测，在`transfer`方法中，被转移后的节点会改为ForwardingNode，它是一个占位节点，并且hash=MOVED（-1），也就是说，我们可以通过判断hash是否为MOVED来确定当前节点的状态），此时调用`helpTransfer(tab, f)`帮助转移，主要操作就是更新帮助转移的线程数（sizeCtl+1），然后调用`transfer`方法进行转移操作，`transfer`后面我们会详细分析。

### 3.1.3 treeifyBin(Node<K,V>[] , index)



```java
private final void treeifyBin(Node<K,V>[] tab, int index) {
        Node<K,V> b; int n, sc;
        if (tab != null) {
            //当数组长度还未超过64,优先数组的扩容,否则将链表转为红黑树
            if ((n = tab.length) < MIN_TREEIFY_CAPACITY)
                //两倍扩容
                tryPresize(n << 1);
            else if ((b = tabAt(tab, index)) != null && b.hash >= 0) {
                synchronized (b) {
                    if (tabAt(tab, index) == b) {//check stable
                        //hd：节点头
                        TreeNode<K,V> hd = null, tl = null;
                        //遍历转换节点
                        for (Node<K,V> e = b; e != null; e = e.next) {
                            TreeNode<K,V> p =
                                new TreeNode<K,V>(e.hash, e.key, e.val,
                                                  null, null);
                            if ((p.prev = tl) == null)
                                hd = p;
                            else
                                tl.next = p;
                            tl = p;
                        }
                        setTabAt(tab, index, new TreeBin<K,V>(hd));
                    }
                }
            }
        }
    }
```

**说明：** 在put操作完成后，如果当前节点为一个链表，并且链表长度>=TREEIFY_THRESHOLD(8)，此时就需要调用`treeifyBin`方法来把当前链表转为一个红黑树。`treeifyBin`主要进行两步操作：

1. 如果当前table长度还未超过`MIN_TREEIFY_CAPACITY(64)`，则优先对数组进行扩容操作，容量为原来的2倍(n<<1)。
2. 否则就对当前节点进行转换操作（注意这个操作是单线程完成的）。遍历链表节点，把Node转换为TreeNode，然后在通过TreeBin来构造红黑树（红黑树的构造这里就不在详细介绍了）。

### 3.1.4 tryPresize(int size)

```java
private final void tryPresize(int size) {
    int c = (size >= (MAXIMUM_CAPACITY >>> 1)) ? MAXIMUM_CAPACITY :
        tableSizeFor(size + (size >>> 1) + 1);//计算一个近似size的2的幂次方数
    int sc;
    while ((sc = sizeCtl) >= 0) {
        Node<K,V>[] tab = table; int n;
        //未初始化
        if (tab == null || (n = tab.length) == 0) {
            n = (sc > c) ? sc : c;
            if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                try {
                    if (table == tab) {
                        @SuppressWarnings("unchecked")
                        Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                        table = nt;
                        sc = n - (n >>> 2);
                    }
                } finally {
                    sizeCtl = sc;
                }
            }
        }
        //已达到最大容量
        else if (c <= sc || n >= MAXIMUM_CAPACITY)
            break;
        else if (tab == table) {
            int rs = resizeStamp(n);
            //正在进行扩容操作
            if (sc < 0) {
                Node<K,V>[] nt;
                if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                    sc == rs + MAX_RESIZERS || (nt = nextTable) == null ||
                    transferIndex <= 0)
                    break;
                if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1))
                    transfer(tab, nt);
            }
            else if (U.compareAndSwapInt(this, SIZECTL, sc,
                                         (rs << RESIZE_STAMP_SHIFT) + 2))
                transfer(tab, null);
        }
    }
}
```

**说明：** 当table容量不足时，需要对其进行两倍扩容。`tryPresize`方法很简单，主要就是用来检查扩容前的必要条件（比如是否超过最大容量），真正的扩容其实也可以叫**“节点转移”**，主要是通过`transfer`方法完成。

### 3.1.5 transfer(Node<K,V> tab, Node<K,V> nextTab)



```java
//转移或复制节点到新的table
private final void transfer(Node<K,V>[] tab, Node<K,V>[] nextTab) {
    int n = tab.length, stride;
    //转移幅度( tab.length/(NCPU*8) )，最小为16
    if ((stride = (NCPU > 1) ? (n >>> 3) / NCPU : n) < MIN_TRANSFER_STRIDE)
        stride = MIN_TRANSFER_STRIDE; // subdivide range
    if (nextTab == null) {            // initiating
        try {
            //根据当前数组长度,新建一个两倍长度的数组nextTab
            @SuppressWarnings("unchecked")
            Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n << 1];
            nextTab = nt;
        } catch (Throwable ex) {      // try to cope with OOME
            sizeCtl = Integer.MAX_VALUE;
            return;
        }
        nextTable = nextTab;
        transferIndex = n;//初始为table的最后一个索引
    }
    int nextn = nextTab.length;
    //初始化ForwardingNode节点,持有nextTab的引用,在处理完每个节点之后当做占位节点，表示该槽位已经处理过了
    ForwardingNode<K,V> fwd = new ForwardingNode<K,V>(nextTab);
    boolean advance = true;//节点是否已经处理
    boolean finishing = false; // to ensure sweep before committing nextTab
    //自旋移动每个节点，从transferIndex开始移动stride个节点到新的table。
    //i：当前处理的Node索引；bound：需要处理节点的索引边界
    for (int i = 0, bound = 0;;) {
        //f:当前处理i位置的node; fh:f.hash
        Node<K,V> f; int fh;
        //通过while循环获取本次需要移动的节点索引i
        while (advance) {
            //nextIndex:下一个要处理的节点索引; nextBound:下一个需要处理的节点的索引边界
            int nextIndex, nextBound;
            if (--i >= bound || finishing)//通过--i控制下一个需要移动的节点
                advance = false;
            //节点已全部转移
            else if ((nextIndex = transferIndex) <= 0) {
                i = -1;
                advance = false;
            }
            //transferIndex（初值为最后一个节点的索引），表示从transferIndex开始后面所有的节点都已分配，
            //每次线程领取扩容任务后，需要更新transferIndex的值(transferIndex-stride)。
            //CAS修改transferIndex，并更新索引边界
            else if (U.compareAndSwapInt
                     (this, TRANSFERINDEX, nextIndex,
                      nextBound = (nextIndex > stride ?
                                   nextIndex - stride : 0))) {
                bound = nextBound;
                i = nextIndex - 1;
                advance = false;
            }
        }
        if (i < 0 || i >= n || i + n >= nextn) {
            int sc;
            if (finishing) {//已完成转移，更新相关属性
                nextTable = null;
                table = nextTab;
                sizeCtl = (n << 1) - (n >>> 1);//1.5*n 扩容阈值设置为原来容量的1.5倍  依然相当于现在容量的0.75倍
                return;
            }
            //当前线程已经完成转移，但可能还有其他线程正在进行转移操作
            //每个线程完成自己的扩容操作后就对sizeCtl-1
            if (U.compareAndSwapInt(this, SIZECTL, sc = sizeCtl, sc - 1)) {
                //判断是否全部任务已经完成,sizeCtl初始值=(rs << RESIZE_STAMP_SHIFT) + 2)
                //这里判断如果还有其他线程正在操作，直接返回，否则的话重新初始化i对原tab进行一遍检查然后再提交
                if ((sc - 2) != resizeStamp(n) << RESIZE_STAMP_SHIFT)
                    return;
                finishing = advance = true;
                i = n; // recheck before commit
            }
        }
        else if ((f = tabAt(tab, i)) == null)
            advance = casTabAt(tab, i, null, fwd);//i位置节点为空，替换为ForwardingNode节点，用于通知其他线程该位置已经处理
        else if ((fh = f.hash) == MOVED)//节点已经被其他线程处理过，继续处理下一个节点
            advance = true; // already processed
        else {
            synchronized (f) {
                if (tabAt(tab, i) == f) {//check stable
                    //处理当前拿到的节点,构建两个node:ln/hn。ln:原位置; hn:i+n位置
                    Node<K,V> ln, hn;

                    if (fh >= 0) {//当前为链表节点（fh>=0）
                        //使用fn&n把原链表中的元素分成两份（fn&n = n or 0）
                        //在表扩容2倍后，索引i可能发生改变，如果原table长度n=2^x，如果hash的x位为1，此时需要加上x位的值，也就是i+n；
                        //如果x位为0，索引i不变
                        int runBit = fh & n; // n or 0
                        //最后一个与头节点f索引不同的节点
                        Node<K,V> lastRun = f;
                        //从索引i的节点开始向后查找最后一个有效节点
                        for (Node<K,V> p = f.next; p != null; p = p.next) {
                            int b = p.hash & n;//n or 0
                            if (b != runBit) {
                                runBit = b;
                                lastRun = p;
                            }
                        }
                        if (runBit == 0) {
                            ln = lastRun;
                            hn = null;
                        } else {
                            hn = lastRun;
                            ln = null;
                        }
                        //把f链表分解为两个链表
                        for (Node<K,V> p = f; p != lastRun; p = p.next) {
                            int ph = p.hash; K pk = p.key; V pv = p.val;
                            //在原位置
                            if ((ph & n) == 0)
                                ln = new Node<K,V>(ph, pk, pv, ln);
                            //i+n位置
                            else
                                hn = new Node<K,V>(ph, pk, pv, hn);
                        }
                        //nextTab的i位置插入一个链表
                        setTabAt(nextTab, i, ln);
                        //nextTab的i+n位置插入一个链表
                        setTabAt(nextTab, i + n, hn);
                        //在table的i位置上插入forwardNode节点  表示已经处理过该节点
                        setTabAt(tab, i, fwd);
                        advance = true;
                    }
                    /**
                     * 如果该节点是红黑树结构，则构造树节点lo和hi，遍历红黑树中的节点，同样是根据hash&tab.length算法，
                     * 把节点分为两类，分别插入索引i和(i+n)位置。
                     */
                    else if (f instanceof TreeBin) {
                        //转为根结点
                        TreeBin<K,V> t = (TreeBin<K,V>)f;
                        TreeNode<K,V> lo = null, loTail = null;//低位(i)节点和低位尾节点
                        TreeNode<K,V> hi = null, hiTail = null;//高位(i+n)节点和高位尾节点
                        int lc = 0, hc = 0;
                        //从首个节点向后遍历
                        for (Node<K,V> e = t.first; e != null; e = e.next) {
                            int h = e.hash;
                            //构建树节点
                            TreeNode<K,V> p = new TreeNode<K,V>
                                (h, e.key, e.val, null, null);
                            //原位置
                            if ((h & n) == 0) {
                                if ((p.prev = loTail) == null)
                                    lo = p;
                                else
                                    loTail.next = p;
                                loTail = p;
                                ++lc;
                            }
                            //i+n位置
                            else {
                                if ((p.prev = hiTail) == null)
                                    hi = p;
                                else
                                    hiTail.next = p;
                                hiTail = p;
                                ++hc;
                            }
                        }
                        //如果扩容后已经不再需要tree的结构 反向转换为链表结构
                        ln = (lc <= UNTREEIFY_THRESHOLD) ? untreeify(lo) :
                            (hc != 0) ? new TreeBin<K,V>(lo) : t;
                        hn = (hc <= UNTREEIFY_THRESHOLD) ? untreeify(hi) :
                            (lc != 0) ? new TreeBin<K,V>(hi) : t;
                        setTabAt(nextTab, i, ln);
                        setTabAt(nextTab, i + n, hn);
                        setTabAt(tab, i, fwd);
                        advance = true;
                    }
                }
            }
        }
    }
}
```

**说明：** `transfer`方法是table扩容的核心实现。由于 ConcurrentHashMap 的扩容是新建一个table，所以主要问题就是如何把旧table的元素转移到新的table上。所以，扩容问题就演变成了“节点转移”问题。首先总结一下需要转移节点（调用transfer）的几个条件：

1. 对table进行扩容时
2. 在更新元素数目时(addCount方法)，元素总数>=sizeCtl（sizeCtl=0.75n，达到扩容阀值），此时也需要扩容
3. 在put操作时，发现索引节点正在转移(hash==MOVED)，此时需要帮助转移

在进行节点转移之前，首先要做的就是重新初始化`sizeCtl`的值（`sizeCtl = (hash << RESIZE_STAMP_SHIFT) + 2`），这个值是一个负值，用于标识当前table正在进行转移操作，并且每有一个线程参与转移，`sizeCtl`就加1。`transfer`执行步骤如下（请结合源码注释阅读）：

1. 计算转移幅度`stride`（或者说是当前线程需要转移的节点数），最小为16；
2. 创建一个相当于当前 table 两倍容量的 Node 数组，转移完成后用作新的 table；
3. 从`transferIndex`（初始为`table.length`，也就是 table 的最后一个节点）开始，依次向前处理`stride`个节点。前面介绍过，table 的每个节点都可能是一个链表结构，因为在 put 的时候是根据`(table.length-1)&hash`计算出的索引，当插入新值时，如果通过 key 计算出的索引已经存在节点，那么这个新值就放在这个索引位节点的尾部(Node.next)。所以，在进行节点转移后，由于 table.length 变为原来的两倍，所以相应的索引也会改变，这时候就需要对链表进行分割，我们来看一下这个分割算法：

- 假设当前处理的节点 table[i]=f，并且它是一个链表结构，原table容量为 n=2x，索引计算公式为`i=(n - 1)&hash`。在表扩张后，由于容量 n 变为 2x+1 = 2*2x，所以索引计算就变为`i=(2n - 1)&hash`。如果 hash 的 x 位为0，则 hash&(2x-1)=hash，此时 hash&(2x-1) == hash&(2x+1-1)，索引位 i 不变；如果 hash 的 x 位为1，则 hash&2x=2x == n，在扩容后 x 变为 x+1，此时的索引需要加上 x 位的值，即 _i=hash&(2x-1) + hash&2x，也就是 i+n。举个栗子：设 n=100000 (25)，x=5，hash 为100101（x位是1）。n-1=011111，那么i=hash&(n-1)=000101；扩容后容量变为m=1000000(26)，m-1=0111111，那么 i 就变成了 hash&(m-1)=100101，也就是说新的索引位_i = i+n。
- 如果当前节点为红黑树结构，也是利用这个算法进行分割，不同的是，在分割完成之后，如果这两个新的树节点<=6，则调用`untreeify`方法把树结构转为链表结构。

1. 最后把操作过的节点都设为 ForwardingNode 节点（hash= MOVED，这样别的线程就可以检测到）。

**`transfer`操作完成后，table的结构变化如下：**

![img](https:////upload-images.jianshu.io/upload_images/6050820-b17f80ff4712285d.png?imageMogr2/auto-orient/strip|imageView2/2/w/1092/format/webp)

扩容之后的table变化


### 3.1.6 addCount(long,int)

+ 源码分析

```
// 从 putVal 传入的参数是 1， binCount，binCount 默认是0，只有 hash 冲突了才会大于 1.且他的大小是链表的长度（如果不是红黑数结构的话）。
private final void addCount(long x, int check) {
    CounterCell[] as; long b, s;
    // 如果计数盒子不是空 或者
    // 如果修改 baseCount 失败
    if ((as = counterCells) != null ||
        !U.compareAndSwapLong(this, BASECOUNT, b = baseCount, s = b + x)) {
        CounterCell a; long v; int m;
        boolean uncontended = true;
        // 如果计数盒子是空（尚未出现并发）
        // 如果随机取余一个数组位置为空 或者
        // 修改这个槽位的变量失败（出现并发了）
        // 执行 fullAddCount 方法。并结束
        if (as == null || (m = as.length - 1) < 0 ||
            (a = as[ThreadLocalRandom.getProbe() & m]) == null ||
            !(uncontended =
              U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))) {
            fullAddCount(x, uncontended);
            return;
        }
        if (check <= 1)
            return;
        s = sumCount();
    }
    // 如果需要检查,检查是否需要扩容，在 putVal 方法调用时，默认就是要检查的。
    if (check >= 0) {
        Node<K,V>[] tab, nt; int n, sc;
        // 如果map.size() 大于 sizeCtl（达到扩容阈值需要扩容） 且
        // table 不是空；且 table 的长度小于 1 << 30。（可以扩容）
        while (s >= (long)(sc = sizeCtl) && (tab = table) != null &&
               (n = tab.length) < MAXIMUM_CAPACITY) {
            // 根据 length 得到一个标识
            int rs = resizeStamp(n);
            // 如果正在扩容
            if (sc < 0) {
                // 如果 sc 的低 16 位不等于 标识符（校验异常 sizeCtl 变化了）
                // 如果 sc == 标识符 + 1 （扩容结束了，不再有线程进行扩容）（默认第一个线程设置 sc ==rs 左移 16 位 + 2，当第一个线程结束扩容了，就会将 sc 减一。这个时候，sc 就等于 rs + 1）
                // 如果 sc == 标识符 + 65535（帮助线程数已经达到最大）
                // 如果 nextTable == null（结束扩容了）
                // 如果 transferIndex <= 0 (转移状态变化了)
                // 结束循环 
                if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                    sc == rs + MAX_RESIZERS || (nt = nextTable) == null ||
                    transferIndex <= 0)
                    break;
                // 如果可以帮助扩容，那么将 sc 加 1. 表示多了一个线程在帮助扩容
                if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1))
                    // 扩容
                    transfer(tab, nt);
            }
            // 如果不在扩容，将 sc 更新：标识符左移 16 位 然后 + 2. 也就是变成一个负数。高 16 位是标识符，低 16 位初始是 2.
            else if (U.compareAndSwapInt(this, SIZECTL, sc,
                                         (rs << RESIZE_STAMP_SHIFT) + 2))
                // 更新 sizeCtl 为负数后，开始扩容。
                transfer(tab, null);
            s = sumCount();
        }
    }
}
```

总结一下该方法的逻辑：

x 参数表示的此次需要对表中元素的个数加几。check 参数表示是否需要进行扩容检查，大于等于0 需要进行检查，而我们的 putVal 方法的 binCount 参数最小也是 0 ，因此，每次添加元素都会进行检查。（除非是覆盖操作）

1. 判断计数盒子属性是否是空，如果是空，就尝试修改 baseCount 变量，对该变量进行加 X。
2. 如果计数盒子不是空，或者修改 baseCount 变量失败了，则放弃对 baseCount 进行操作。
3. 如果计数盒子是 null 或者计数盒子的 length 是 0，或者随机取一个位置取于数组长度是 null，那么就对刚刚的元素进行 CAS 赋值。
4. 如果赋值失败，或者满足上面的条件，则调用 fullAddCount 方法重新死循环插入。
5. 这里如果操作 baseCount 失败了（或者计数盒子不是 Null），且对计数盒子赋值成功，那么就检查 check 变量，如果该变量小于等于 1. 直接结束。否则，计算一下 count 变量。
6. 如果 check 大于等于 0 ，说明需要对是否扩容进行检查。
7. 如果 map 的 size 大于 sizeCtl（扩容阈值），且 table 的长度小于 1 << 30，那么就进行扩容。
8. 根据 length 得到一个标识符，然后，判断 sizeCtl 状态，如果小于 0 ，说明要么在初始化，要么在扩容。
9. 如果正在扩容，那么就校验一下数据是否变化了（具体可以看上面代码的注释）。如果检验数据不通过，break。
10. 如果校验数据通过了，那么将 sizeCtl 加一，表示多了一个线程帮助扩容。然后进行扩容。
11. 如果没有在扩容，但是需要扩容。那么就将 sizeCtl 更新，赋值为标识符左移 16 位 —— 一个负数。然后加 2。 表示，已经有一个线程开始扩容了。然后进行扩容。然后再次更新 count，看看是否还需要扩容。

## 总结一下

总结下来看，addCount 方法做了 2 件事情：

1. 对 table 的长度加一。无论是通过修改 baseCount，还是通过使用 CounterCell。当 CounterCell 被初始化了，就优先使用他，不再使用 baseCount。
2. 检查是否需要扩容，或者是否正在扩容。如果需要扩容，就调用扩容方法，如果正在扩容，就帮助其扩容。

有几个要点注意：

1. 第一次调用扩容方法前，sizeCtl 的低 16 位是加 2 的，不是加一。所以 sc == rs + 1 的判断是表示是否完成任务了。因为完成扩容后，sizeCtl == rs + 1。
2. 扩容线程最大数量是 65535，是由于低 16 位的位数限制。
3. 这里也是可以帮助扩容的，类似 helpTransfer 方法。

**说明：** put操作全部完成后，别忘了更新元素数量。`addCount`用来更新 ConcurrentHashMap 的元素数，根据所传参数`check`决定是否检查扩容，如果需要，调用`transfer`方法进行扩容/节点转移。这里面有一个看起来比较复杂的方法`fullAddCount`，作用是在线程争用资源时，使用它来计算更新元素数。这个方法的实现类似于LongAdder的add（LongAdder在上面有简单介绍），源码在此就不再详细分析了，有兴趣的同学可以研究下。

# 4. 总结

到此，ConcurrentHashMap的分析就告一段落了。总的来说源码比较复杂，真正理解它还是需要一些耐心的。重点是它的**数据结构**和**扩容**的实现。
 ConcurrentHashMap 源码分析到此结束，希望对大家有所帮助，如您发现文章中有不妥的地方，请留言指正，谢谢

