在1.7和1.8版本中，计算size()方法有写不同。先介绍1.7版本的实现。

## **1.7版本**

在1.7版本中，有一个重要的类`Segment`，利用它来实现分段锁

```java
static final class Segment<K,V> extends ReentrantLock implements Serializable {  
        private static final long serialVersionUID = 2249069246763182397L;  
    // 最大尝试获取锁次数，tryLock可能会阻塞，准备锁住segment操作获取锁。  
     //在多处理器中，用一个有界的尝试次数，保证在定位node的时候，可以从缓存直接获取。  
        static final int MAX_SCAN_RETRIES =  
            Runtime.getRuntime().availableProcessors() > 1 ? 64 : 1;   
    //segment内部的Hash table，访问HashEntry，通过具有volatile的entryAt/setEntryAt方法  
        transient volatile HashEntry<K,V>[] table;   
     //segment的table中HashEntry的数量，只有在lock或其他保证可见性的volatile reads中，才可以访问count  
    transient int count;  
    //在segment上所有的修改操作数。尽管可能会溢出，但它为isEmpty和size方法，  
    //提供了有效准确稳定的检查或校验。只有在lock或其他保证可见性的volatile reads 中，才可以访问  
        transient int modCount;  
        transient int threshold;   
        final float loadFactor;  
        Segment(float lf, int threshold, HashEntry<K,V>[] tab) {  
            this.loadFactor = lf;  
            this.threshold = threshold;  
            this.table = tab;  
        }  
}  
```

```
static final class HashEntry<K,V> {
    final int hash;
    final K key;
    volatile V value;
    volatile HashEntry<K,V> next;

    HashEntry(int hash, K key, V value, HashEntry<K,V> next) {
        this.hash = hash;
        this.key = key;
        this.value = value;
        this.next = next;
    }
}
```

刚一开始不加锁，前后计算两次所有segment里面的数量大小和，两次结果相等，表明没有新的元素加入，计算的结果是正确的。如果不相等，就对每个segment加锁，再进行计算，返回结果并释放锁。

```
public int size() {
  final Segment<K,V>[] segments = this.segments;
  int size;
  boolean overflow; // true if size overflows 32 bits
  long sum;         // sum of modCounts
  long last = 0L;   // previous sum
  int retries = -1; // first iteration isn't retry
  try {
    for (;;) {
      if (retries++ == RETRIES_BEFORE_LOCK) {
        for (int j = 0; j < segments.length; ++j)
          ensureSegment(j).lock(); // force creation
      }
      sum = 0L;
      size = 0;
      overflow = false;
      for (int j = 0; j < segments.length; ++j) {
        Segment<K,V> seg = segmentAt(segments, j);
        if (seg != null) {
          sum += seg.modCount;
          int c = seg.count;
          if (c < 0 || (size += c) < 0)
            overflow = true;
        }
      }
      if (sum == last)
        break;
      last = sum;
    }
  } finally {
    if (retries > RETRIES_BEFORE_LOCK) {
      for (int j = 0; j < segments.length; ++j)
        segmentAt(segments, j).unlock();
    }
  }
  return overflow ? Integer.MAX_VALUE : size;
}
```

## **1.8版本**

先利用`sumCount()`计算，然后如果值超过int的最大值，就返回int的最大值。但是有时size就会超过最大值，这时最好用`mappingCount`方法


```
public int size() {
        long n = sumCount();
        return ((n < 0L) ? 0 :
                (n > (long)Integer.MAX_VALUE) ? Integer.MAX_VALUE :
                (int)n);
    }
 public long mappingCount() {
        long n = sumCount();
        return (n < 0L) ? 0L : n; // ignore transient negative values
    }
```

sumCount有两个重要的属性`baseCount`和`counterCells`,如果`counterCells`不为空，那么总共的大小就是baseCount与遍历`counterCells`的value值累加获得的。

```
final long sumCount() {
        CounterCell[] as = counterCells; CounterCell a;
        long sum = baseCount;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    sum += a.value;
            }
        }
        return sum;
    }
```

baseCount是从哪里来的？

```
//当没有线程争用时，使用这个变量计数
 private transient volatile long baseCount;
```

一个volatile变量，在addCount方法会使用它，而addCount方法在put结束后会调用

```
addCount(1L, binCount);
```

```
 if ((as = counterCells) != null ||
            !U.compareAndSwapLong(this, BASECOUNT, b = baseCount, s = b + x)) 
```

从上可知，在put操作结束后，会调用addCount，更新计数。
在并发情况下，如果CAS修改baseCount失败后，就会使用到CounterCell类，会创建一个对象，通常对象的volatile的value属性是1。

```
// 一种用于分配计数的填充单元。改编自LongAdder和Striped64。请查看他们的内部文档进行解释。
@sun.misc.Contended 
static final class CounterCell {
    volatile long value;
    CounterCell(long x) { value = x; }
}
```

并发时，利用CAS修改baseCount失败后，会利用CAS操作修改CountCell的值，

```
 if (as == null || (m = as.length - 1) < 0 ||
                (a = as[ThreadLocalRandom.getProbe() & m]) == null ||
                !(uncontended =
                  U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))) {
                fullAddCount(x, uncontended);
                return;
            }
```

如果上面CAS操作也失败了，在fullAddCount方法中，会继续死循环操作，知道成功。

~~~
for (;;) {
            CounterCell[] as; CounterCell a; int n; long v;
            if ((as = counterCells) != null && (n = as.length) > 0) {
                if ((a = as[(n - 1) & h]) == null) {
                    if (cellsBusy == 0) {            // Try to attach new Cell
                        CounterCell r = new CounterCell(x); // Optimistic create
                        if (cellsBusy == 0 &&
                            U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                            boolean created = false;
                            try {               // Recheck under lock
                                CounterCell[] rs; int m, j;
                                if ((rs = counterCells) != null &&
                                    (m = rs.length) > 0 &&
                                    rs[j = (m - 1) & h] == null) {
                                    rs[j] = r;
                                    created = true;
                                }
                            } finally {
                                cellsBusy = 0;
                            }
                            if (created)
                                break;
                            continue;           // Slot is now non-empty
                        }
                    }
~~~