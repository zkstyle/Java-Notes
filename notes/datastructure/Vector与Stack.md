# Vector与Stack的前世今生
## 一、初始Vector　

　　　　　　　　　　1、Vector是一个可变化长度的数组

　　　　　　　　　　2、Vector增加长度通过的是capacity和capacityIncrement这两个变量，目前还不知道如何实现自动扩增的，等会源码分析

　　　　　　　　　　3、Vector也可以获得iterator和listIterator这两个迭代器，并且他们发生的是fail-fast，而不是fail-safe，注意这里，不要觉得这个vector是线程安全就搞错了，具体分析在下面会说

　　　　　　　　　　4、Vector是一个线程安全的类，如果使用需要线程安全就使用Vector，如果不需要，就使用arrayList

　　　　　　　　　　5、Vector和ArrayList很类似，就少许的不一样，从它继承的类和实现的接口来看，跟arrayList一模一样。

## 二、构造方法
~~~java
/**
    * 构造一个空向量，使其内部数据数组的大小为 10，其标准容量增量为零。
    */
    public Vector() {
           this(10);
    }

   /**
    * 构造一个包含指定 collection 中的元素的向量，这些元素按其 collection 的迭代器返回元素的顺序排列。
    */
   public Vector(Collection<? extends E> c) {
       elementData = c.toArray();
       elementCount = elementData.length;
       // c.toArray might (incorrectly) not return Object[] (see 6260652)
       if (elementData.getClass() != Object[].class)
           elementData = Arrays.copyOf(elementData, elementCount,
                   Object[].class);
   }

   /**
    * 使用指定的初始容量和等于零的容量增量构造一个空向量。
    */
   public Vector(int initialCapacity) {
       this(initialCapacity, 0);
   }

   /**
    *  使用指定的初始容量和容量增量构造一个空的向量。
    */
   public Vector(int initialCapacity, int capacityIncrement) {
       super();
       if (initialCapacity < 0)
           throw new IllegalArgumentException("Illegal Capacity: "+
                                              initialCapacity);
       this.elementData = new Object[initialCapacity];
       this.capacityIncrement = capacityIncrement;
   }
~~~

## 三、常用方法
- add(E e)

将指定元素添加到此向量的末尾。
~~~java
public synchronized boolean add(E e) {
       modCount++;     
       ensureCapacityHelper(elementCount + 1);    //确认容器大小，如果操作容量则扩容操作
       elementData[elementCount++] = e;   //将e元素添加至末尾
       return true;
   }
~~~
这个方法相对而言比较简单，具体过程就是先确认容器的大小，看是否需要进行扩容操作，然后将E元素添加到此向量的末尾。扩容机制是若传入了扩容量capacityIncrement
则每次扩容增加capacityIncrement 否则就将容量增加一倍

~~~java
private void ensureCapacityHelper(int minCapacity) {
       //如果
       if (minCapacity - elementData.length > 0)
           grow(minCapacity);
   }

   /**
    * 进行扩容操作
    * 如果此向量的当前容量小于minCapacity，则通过将其内部数组替换为一个较大的数组俩增加其容量。
    * 新数据数组的大小为原来的大小 + capacityIncrement，
    * 除非 capacityIncrement 的值小于等于零，在后一种情况下，新的容量将为原来容量的两倍，不过，如果此大小仍然小于 minCapacity，则新容量将为 minCapacity。
    */
   private void grow(int minCapacity) {
       int oldCapacity = elementData.length;     //当前容器大小
       /*
        * 新容器大小
        * 若容量增量系数(capacityIncrement) > 0，则将容器大小增加到capacityIncrement
        * 否则将容量增加一倍
        */
       int newCapacity = oldCapacity + ((capacityIncrement > 0) ?
                                        capacityIncrement : oldCapacity);

       if (newCapacity - minCapacity < 0)
           newCapacity = minCapacity;

       if (newCapacity - MAX_ARRAY_SIZE > 0)
           newCapacity = hugeCapacity(minCapacity);

       elementData = Arrays.copyOf(elementData, newCapacity);
   }

   /**
    * 判断是否超出最大范围
    * MAX_ARRAY_SIZE：private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    */
   private static int hugeCapacity(int minCapacity) {
       if (minCapacity < 0)
           throw new OutOfMemoryError();
       return (minCapacity > MAX_ARRAY_SIZE) ? Integer.MAX_VALUE : MAX_ARRAY_SIZE;
   }
~~~
- remove(Object o)

~~~java
/**
    * 从Vector容器中移除指定元素E
    */
   public boolean remove(Object o) {
       return removeElement(o);
   }

   public synchronized boolean removeElement(Object obj) {
       modCount++;
       int i = indexOf(obj);   //计算obj在Vector容器中位置
       if (i >= 0) {
           removeElementAt(i);   //移除
           return true;
       }
       return false;
   }

   public synchronized void removeElementAt(int index) {
       modCount++;     //修改次数+1
       if (index >= elementCount) {   //删除位置大于容器有效大小
           throw new ArrayIndexOutOfBoundsException(index + " >= " + elementCount);
       }
       else if (index < 0) {    //位置小于 < 0
           throw new ArrayIndexOutOfBoundsException(index);
       }
       int j = elementCount - index - 1;
       if (j > 0) {   
           //从指定源数组中复制一个数组，复制从指定的位置开始，到目标数组的指定位置结束。
           //也就是数组元素从j位置往前移
           System.arraycopy(elementData, index + 1, elementData, index, j);
       }
       elementCount--;   //容器中有效组件个数 - 1
       elementData[elementCount] = null;    //将向量的末尾位置设置为null
   }
~~~
因为Vector底层是使用数组实现的，所以它的操作都是对数组进行操作，只不过其是可以随着元素的增加而动态的改变容量大小，

其实现方法是是使用Arrays.copyOf方法将旧数据拷贝到一个新的大容量数组中。

## 四、Stack

　　　　　　现在来看看Vector的子类Stack，学过数据结构都知道，这个就是栈的意思。那么该类就是跟栈的用法一样了

　　　　　　通过查看他的方法，和查看api文档，很容易就能知道他的特性。就几个操作，出栈，入栈等，构造方法也是空的，用的还是数组，父类中的构造，跟父类一样的扩增方式，并且它的方法也是同步的，所以也是线程安全
~~~java
public
class Stack<E> extends Vector<E> {
    /**
     * Creates an empty Stack.
     */
    public Stack() {
    }

    /**
     * push就是等同于在最后一位添加一个元素　所以调用addElement
     * 与Vector的add()调用的方法一样
     */
    public E push(E item) {
        addElement(item);

        return item;
    }

    /**
     * 分两步　获取返回对象　通过peek()获取
     * 然后删除栈顶元素　也就是数组最后一位元素
     */
    public synchronized E pop() {
        E       obj;
        int     len = size();

        obj = peek();
        removeElementAt(len - 1);

        return obj;
    }

    /**
     * 因为Stack是继承Vector 数组实现　故可以直接访问数组最后一个元素
     */
    public synchronized E peek() {
        int len = size();

        if (len == 0)
            throw new EmptyStackException();
        return elementAt(len - 1);
    }

    /**
     * 判断元素个数是否为0
     */
    public boolean empty() {
        return size() == 0;
    }

    /**
     * 通过lastIndexOf搜索对象的索引　因为是栈　所以返回size()-i
     */
    public synchronized int search(Object o) {
        int i = lastIndexOf(o);

        if (i >= 0) {
            return size() - i;
        }
        return -1;
    }
}

~~~

## 五、总结Vector和Stack

　　　　　　首先，通过Vector源码分析，知道

　　　　　　1、Vector线程安全是因为他的方法都加了synchronized关键字

　　　　　　2、Vector的本质是一个数组，特点能是能够自动扩增，扩增的方式跟capacityIncrement的值有关

　　　　　　3、它也会fail-fast，还有一个fail-safe两个的区别在下面的list总结中会讲到

　　　　　　Stack的总结

　　　　　　1、对栈的一些操作，后进先出

　　　　　　2、底层也是用数组实现的，因为继承了Vector

　　　　　　3、也是线程安全的

----------------------------------------------------------------------------------------------------------------------------------------------------------------------------

## 小结

　　1、arrayList和LinkedList区别？

　　　　　　·arrayList底层是用数组实现的顺序表，是随机存取类型，可自动扩增，并且在初始化时，数组的长度是0，只有在增加元素时，长度才会增加。默认是10，不能无限扩增，有上限，在查询操作的时候性能更好

　　　　　　·LinkedList底层是用链表来实现的，是一个双向链表，注意这里不是双向循环链表,顺序存取类型。在源码中，似乎没有元素个数的限制。应该能无限增加下去，直到内存满了在进行删除，增加操作时性能更好。

　　　　　　·两个都是线程不安全的，在iterator时，会发生fail-fast。

　　2、arrayList和Vector的区别？

　　　　　　·arrayList线程不安全，在用iterator，会发生fail-fast

　　　　　　·Vector线程安全，因为在方法前加了Synchronized关键字。也会发生fail-fast

 

　　3、fail-fast和fail-safe区别是什么?什么情况下会发生？

　　　　　　一句话，在在java.util下的集合都是发生fail-fast，而在java.util.concurrent下的发生的都是fail-safe

　　　　　　fail-fast：快速失败，例如在arrayList中使用迭代器遍历时，有另外的线程对arrayList的存储数组进行了改变，比如add、delete、等使之发生了结构上的改变，所以Iterator就会快速报一个

　　　　　　java.util.ConcurrentModificationException 异常，这就是快速失败。

　　　　　　fail-safe:安全失败，在java.util.concurrent下的类，都是线程安全的类，他们在迭代的过程中，如果有线程进行结构的改变，不会报异常，而是正常遍历，这就是安全失败。

　　　　　　1、为什么在java.util.concurrent包下对集合有结构的改变，却不会报异常？
      
 　　　　　　这里稍微解释一下，在concurrent下的集合类增加元素的时候使用Arrays.copyOf()来拷贝副本，在副本上增加元素，如果有其他线程在此改变了集合的结构，那也是在副本上的改变，而不是影响到原集合，迭代器还是照常遍历，遍历完之后，改变原引用指向副本，所以总的一句话就是如果在次包下的类进行增加删除，就会出现一个副本。所以能防止fail-fast，这种机制并不会出错，所以我们叫这种现象为fail-safe。
      
   　　　　2、vector也是线程安全的，为什么是fail-fast呢？
      
  　　　　　这里搞清楚一个问题，并不是说线程安全的集合就不会报fail-fast，而是报fail-safe，你得搞清楚上面这个问题答案的原理，出现fail-safe是因为他们在实现增删的底层机制不一样，就像上面说的，会有一个副本，而像arrayList、linekdList、verctor等，他们底层就是对着真正的引用进行操作，所以才会发生异常。
      
　　　　　　3、既然是线程安全的，为什么在迭代的时候，还会有别的线程来改变其集合的结构呢(也就是对其删除和增加等操作)？
      
   　　　　首先，我们迭代的时候，根本就没用到集合中的删除、增加，查询的操作，就拿vector来说，我们都没有用那些加锁的方法，也就是方法锁放在那没人拿，在迭代的过程中，有人拿了那把锁，我们也没有办法，因为那把锁就放在那边，


　　　　4、为什么现在都不提倡使用vector了

　　　　　　　1、vector实现线程安全的方法是在每个操作方法上加锁，这些锁并不是必须要的，在实际开发中，一般都是通过锁一系列的操作来实现线程安全，也就是说将需要同步的资源放一起加锁来保证线程安全，

　　　　　　　2、如果多个Thread并发执行一个已经加锁的方法，但是在该方法中，又有vector的存在，vector本身实现中已经加锁了，那么相当于锁上又加锁，会造成额外的开销，

　　　　　　  3、就如上面第三个问题所说的，vector还有fail-fast的问题，也就是说它也无法保证遍历安全，在遍历时又得额外加锁，又是额外的开销，还不如直接用arrayList，然后再加锁呢。

　　　　　　总结：Vector在你不需要进行线程安全的时候，也会给你加锁，也就导致了额外开销，所以在jdk1.5之后就被弃用了，现在如果要用到线程安全的集合，都是从java.util.concurrent包下去拿相应的类。