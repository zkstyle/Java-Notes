# JUC源码分析—CAS和Unsafe

> 在对J.U.C包的源码分析之前，首先介绍下一个比较重要的概念-CAS（Compare-and-Swap）。在J.U.C包中大量使用了CAS，涉及并发或资源争用的地方都使用了sun.misc.Unsafe类的方法进行CAS操作。

------

## CAS

> 在针对多处理器操作而设计的处理器中提供了一些特殊指令，用于管理共享数据的并发访问。现在，几乎所有的现代处理器中都包含了某种形式的原子读-改-写指令，例如比较并交换（Compare-and-Swap）或者关联加载/条件存储（Load-Linked/Store-Conditional）。操作系统和JVM可以使用这些指令来实现锁和并发的数据结构。

**CAS,compare and swap比较并替换。 CAS有三个参数：需要读写的内存位值（V）、进行比较的预期原值（A）和拟写入的新值(B)。当且仅当V的值等于A时，CAS才会通过原子方式用新值B来更新V的值，否则不会执行任何操作。**简单来说，CAS的含义是：“我认为V的值应该是A，如果是，那么将V的值更新为B，否则不修改并告诉V的值实际为多少”。CAS是一项乐观的技术，它希望能成功地执行更新操作，并且如果有另一个线程在最近一次检查后更新了该变量，那么CAS能检测到这个错误。当多个线程尝试使用CAS同时更新同一个变量时，只有其中一个线程能更新变量的值，而其他线程都将失败。但是，失败的线程并不会被挂起（这就是与获取锁的机制不同之处），而是被告知在这次竞争中失败，并可以多次尝试。这种灵活性就大大减少了与锁相关的活跃性风险。

以AtomicInteger为例，内部的CAS实现如下：



```cpp
public class AtomicInteger extends Number implements java.io.Serializable {
    private static final long serialVersionUID = 6214790243416807050L;

    // setup to use Unsafe.compareAndSwapInt for updates
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    //value的偏移地址
    private static final long valueOffset;

    static {
        try {
            valueOffset = unsafe.objectFieldOffset
                    (AtomicInteger.class.getDeclaredField("value"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    private volatile int value;

    public AtomicInteger(int initialValue) {
        value = initialValue;
    }

    public final int getAndUpdate(IntUnaryOperator updateFunction) {
        int prev, next;
        do {
            prev = get();
            next = updateFunction.applyAsInt(prev);
        } while (!compareAndSet(prev, next));
        return prev;
    }
    public final boolean compareAndSet(int expect, int update) {
        return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
    }
}
```

**说明**： 可以看到`AtomicInteger`内部都是使用了`Unsafe`类来进行CAS操作，`valueOffset`表示的是value值的偏移地址，因为Unsafe就是根据内存偏移地址获取数据的原值的, 偏移量可以简单理解为指针指向该变量的内存地址。 `value`使用volatile修饰，直接从共享内存中操作变量，保证多线程之间看到的value值是同一份。
以方法`getAndUpdate()`为例，执行步骤如下：

1. 从内存中读取修改前的值`prev`，并执行给定函数式计算修改后的值`next`；
2. 调用`compareAndSet`修改`value`值（内部是调用了unsafe的`compareAndSwapInt`方法）。如果此时有其他线程也在修改这个`value`值，那么CAS操作就会失败，继续进入do循环重新获取新值，再次执行CAS直到修改成功。


### CAS原子操作原理
**1.原子操作意为“不可被中断的一个或一系列操作”。再多处理器上实现原子操作就变的有点复杂**。

**2.处理器如何实现原子操作？**

​    首先处理器能自动保证基本的内存操作是原子性的。表示当一个处理器读取一个字节时，其他处理器不能访问这个字节的内存地址。但是对于复杂的内存操作处理器是不能自动保证其原子性的，比如跨总线宽度、跨多个缓存行和跨页表的访问。但是，处理器提供了**总线锁定**和**缓存锁定**两个机制来保证复杂内存操作的原子性。

**3.总线锁定和缓存锁定**

   －**1）使用总线锁保证原子性**

   **第一机制是通过总线锁保证原子性。**如果多个处理器同时对共享变量进行读改写操作（i＋＋就是经典的读改写操作），那么共享变量就会被多个处理器同时进行操作，这样读改写操作就不是原子的，操作完之后的共享变量的值会和期望的不一致。举个例子，如果i＝1，我们进行两次i＋＋操作，我们期望的结果是3，但是有可能结果是2。  原因可能是多个处理器同时从各自的缓存中读取变量i，分别进行加1操作，然后分别写入系统内存中。那么，想要保证读改写共享变量的操作是原子的，就必须保证CPU1读改写共享变量的时候，CPU2不能操作缓存了该共享变量内存地址的缓存。

​    **处理器使用总线锁就是来解决这个问题的。所谓总线锁就是使用处理器提供的一个LOCK＃信号，当一个处理器在总线上输出此信号时，其他处理器的请求将被阻塞住，那么该处理器可以独占共享内存。**

   **－2）使用缓存锁保证原子性**

   **第二机制是通过缓存锁定来保证原子性。**在同一时刻，只需保证对某个内存地址的操作是原子性即可，但总线锁定把CPU和内存之间的通信锁住了，这使得锁定期间，其它处理器不能操作其他内存地址的数据，所以总线锁定的开销比较大，目前处理器在某些场合下使用缓存锁定代替总线锁定来进行优化。频繁使用的内存会缓存在处理器的L1、L2和L3高速缓存里，那么原子操作就可以直接在处理器内部缓存中进行，并不需要声明总线锁，在Pentium6和目前的处理器中可以使用“缓存锁定”的方式来实现复杂的原子性。

   所谓“缓存锁定”是指如果共享内存如果被缓存在处理器的缓存行中，并且在Lock操作期间被锁定，那么当它执行锁操作回写到内存时，处理器不在总线上声言LOCK＃信号，而是修改内部的内存地址，并允许它的缓存一致性机制来保证操作的原子性，因为缓存一致性机制会阻止同时修改由两个以上处理器缓存的内存区域数据，当其他处理器回写已被锁定的缓存行的数据时，会使缓存行无效。

   **但是有两种情况下处理器不会使用缓存锁定**

   第一种情况是：当操作的数据不能被缓存在处理器内部，或操作的数据跨多个缓存行时，则处理器会调用总线锁定。

   第二种情况是：有些处理器不支持缓存锁定。对于Intel486和Pentium处理器，就算锁定的内存区域在处理器的缓存行中也会调用总线锁定。

   针对以上两个机制，我们通过Intel处理器提供了很多Lock前缀的指令来实现。例如，位测试和修改指令：BTS、BTR、BTC；交换指令XADD、CMPXCHG，以及其他一些操作数和逻辑指令。被这些指令操作的内存区域就会加锁，导致其他处理器不能同时访问它。

### ABA问题

> ABA问题是一种异常现象：如果算法中的节点可以被循环利用，那么在使用“比较并替换”指令时就可能出现这种问题（主要在没有垃圾回收机制的环境中）。

如果有两个线程x和y，如果x初次从内存中读取变量值为A；线程y对它进行了一些操作使其变成B，然后再改回A，那么线程x进行CAS的时候就会误认为这个值没有被修改过。尽管CAS操作会成功执行，但是不代表它是没有问题的，如果有一个单向链表A B组成的栈，栈顶为A，线程T1准备执行CAS操作`head.compareAndSet(A,B)`，在执行之前线程T2介入，T2将A、B出栈，然后又把C、A放入栈，T2执行完毕；切回线程T1，T1发现栈顶元素依然为A，也会成功执行CAS将栈顶元素修改为B，但因为B.next为null，所以栈结构就会丢弃C元素。

针对这种情况，有一种简单的解决方案：不是更新某个引用的值，而是更新两个值，包括一个引用和一个和版本号，即这个值由A变为B，然后又变成A，版本号也将是不同的。Java中提供了`AtomicStampedReference`和`AtomicMarkableReference`来解决ABA问题。他们支持在两个变量上执行原子的条件更新。`AtomicStampedReference`将更新一个“对象-引用”二元组，通过在引用上加上“版本号”，从而避免ABA问题。 类似地，`AtomicMarkableReference`将更新一个“对象引用-布尔值”二元组，在某些算法中将通过这种二元组使节点保存在链表中同时又将其标记为“已删除的节点”。**不过目前来说，这两个类比较鸡肋，大部分情况下的ABA问题不会影响程序并发的正确性，如果需要解决ABA问题，改用传统的互斥同步可能会比原子类更高效。**

## Unsafe

> Unsafe是实现CAS的核心类，Java无法直接访问底层操作系统，而是通过本地（native）方法来访问。Unsafe类提供了硬件级别的原子操作。

### Unsafe函数列表

```java
///--------------------- peek and poke 指令--------------
//获取对象o中给定偏移地址(offset)的值。以下相关get方法作用相同
public native int getInt(Object o, long offset);
//在对象o的给定偏移地址存储数值x。以下set方法作用相同
public native void putInt(Object o, long offset, int x);
public native Object getObject(Object o, long offset);
public native void putObject(Object o, long offset, Object x);
/**篇幅原因，省略其他类型方法 */
//从给定内存地址获取一个byte。下同
public native byte    getByte(long address);
//在给定内存地址放置一个x。下同
public native void    putByte(long address, byte x);
/**篇幅原因，省略其他类型方法*/
//获取给定内存地址的一个本地指针
public native long getAddress(long address);
//在给定的内存地址处存放一个本地指针x
public native void putAddress(long address, long x);

///------------------内存操作----------------------
//在本地内存分配一块指定大小的新内存，内存的内容未初始化;它们通常被当做垃圾回收。
public native long allocateMemory(long bytes);
//重新分配给定内存地址的本地内存
public native long reallocateMemory(long address, long bytes);
//将给定内存块中的所有字节设置为固定值（通常是0）
public native void setMemory(Object o, long offset, long bytes, byte value);
//复制一块内存，double-register模型
public native void copyMemory(Object srcBase, long srcOffset,
                              Object destBase, long destOffset,
                              long bytes);
//复制一块内存，single-register模型
public void copyMemory(long srcAddress, long destAddress, long bytes) {
    copyMemory(null, srcAddress, null, destAddress, bytes);
}
//释放给定地址的内存
public native void freeMemory(long address);
//获取给定对象的偏移地址
public native long staticFieldOffset(Field f);
public native long objectFieldOffset(Field f);

//------------------数组操作---------------------------------
//获取给定数组的第一个元素的偏移地址
public native int arrayBaseOffset(Class<?> arrayClass);
//获取给定数组的元素增量地址，也就是说每个元素的占位数
public native int arrayIndexScale(Class<?> arrayClass);

//------------------------------------------------------------
//告诉虚拟机去定义一个类。默认情况下，类加载器和保护域都来自这个方法
public native Class<?> defineClass(String name, byte[] b, int off, int len,
                                   ClassLoader loader,
                                   ProtectionDomain protectionDomain);
//定义匿名内部类
public native Class<?> defineAnonymousClass(Class<?> hostClass, byte[] data, Object[] cpPatches);
//定位一个实例，但不运行构造函数
public native Object allocateInstance(Class<?> cls) throws InstantiationException;

///--------------------锁指令（synchronized）-------------------------------
//对象加锁
public native void monitorEnter(Object o);
//对象解锁
public native void monitorExit(Object o);
public native boolean tryMonitorEnter(Object o);
//解除给定线程的阻塞
public native void unpark(Object thread);
//阻塞当前线程
public native void park(boolean isAbsolute, long time);

// CAS
public final native boolean compareAndSwapObject(Object o, long offset,
                                                 Object expected,
                                                 Object x);
//获取对象o的给定偏移地址的引用值（volatile方式）
public native Object getObjectVolatile(Object o, long offset);
public native void    putObjectVolatile(Object o, long offset, Object x);
/** 省略其他类型方法  */


//用于lazySet，适用于低延迟代码。
public native void    putOrderedObject(Object o, long offset, Object x);
/** 省略其他类型方法  */
//获取并加上给定delta，返回加之前的值
public final int getAndAddInt(Object o, long offset, int delta)
/** 省略其他类型方法  */
//为给定偏移地址设置一个新的值，返回设置之前的值
public final int getAndSetInt(Object o, long offset, int newValue)
/** 省略其他类型方法  */

///--------------------1.8新增指令-----------------------
// loadFence() 表示该方法之前的所有load操作在内存屏障之前完成
public native void loadFence();
//表示该方法之前的所有store操作在内存屏障之前完成
public native void storeFence();
//表示该方法之前的所有load、store操作在内存屏障之前完成，这个相当于上面两个的合体功能
public native void fullFence();
```

Unsafe的方法比较简单，直接看方法字面意思就大概知道方法的作用。
在Unsafe里有两个方法模型：
**double-register模型**：给定对象，给定偏移地址offset。从给定对象的偏移地址取值。如`getInt(Object o, long offset)`；
**single-register模型**：给定内存地址，直接从给定内存地址取值，如`getInt(long)`。

这里介绍一下几个比较重要的方法，在之后的源码阅读里会用到。

1. `arrayBaseOffset`：操作数组，用于获取数组的第一个元素的偏移地址
2. `arrayIndexScale`：操作数组，用于获取数组元素的增量地址，也就是说每个元素的占位数。打个栗子：如果有一个数组{1,2,3,4,5,6}，它第一个元素的偏移地址为16，每个元素的占位是4，如果我们要获取数组中“5”这个数字，那么它的偏移地址就是16+4*4。
3. `putOrderedObject`：putOrderedObject 是 lazySet 的实现，适用于低延迟代码。它能够实现非堵塞写入，避免指令重排序，这样它使用快速的存储-存储(store-store) barrier,而不是较慢的存储-加载(store-load) barrier, 后者多是用在volatile的写操作上。但这种性能提升也是有代价的，也就是写后结果并不会被其他线程（甚至是自己的线程）看到，通常是几纳秒后被其他线程看到。类似的方法还有`putOrderedInt、putOrderedLong`。
4. `loadFence`、`storeFence`、`fullFence`：这三个方法是1.8新增，主要针对内存屏障定义，也是为了避免重排序：

- loadFence() 表示该方法之前的所有load操作在内存屏障之前完成。
- storeFence()表示该方法之前的所有store操作在内存屏障之前完成。
- fullFence()表示该方法之前的所有load、store操作在内存屏障之前完成。

