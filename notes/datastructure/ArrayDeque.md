# ArrayDeque源码解析

### 目录

- `ArrayDeque`简介
- `ArrayDeque`的常量和成员变量介绍
- `ArrayDeque`的构造函数
- `ArrayDeque`相关的函数
- 小结
- 参考文章

------

### ArrayDeque简介

`ArrayDeque`类是双端队列`Deque`的实现类，类的继承结构如下:



```java
public class ArrayDeque<E> extends AbstractCollection<E>
                           implements Deque<E>, Cloneable, Serializable
```

就其实现而言，`ArrayDeque`采用了循环数组的方式来完成双端队列的功能。

`ArrayDeque`有以下基本特征：

- 无限的扩展，自动扩展队列大小的。（当然在不会内存溢出的情况下。）
- 非线程安全的，不支持并发访问和修改。
- 支持`fast-fail`.
- 作为栈使用的话比`Stack`要快.
- 当队列使用比`LinkedList`要快。
- `null`元素被禁止使用。

------

### ArrayDeque的常量和成员变量介绍



```java
private static final int MIN_INITIAL_CAPACITY = 8; //初始化 elements 的 最小长度
transient Object[] elements; // ArrayDeque保存数据的数组
transient int head;// 第一个元素的位置
transient int tail;// 最后一个元素的位置
```

------

### ArrayDeque的构造函数

- 数组默认的长度时 `16`，当指定长度时，会通过`allocateElements`计算 大于`numElements` 的最小 `2``n` `(n >=3)`为数组的长度。 为什么是 `2``n` ，和 [HashMap](https://www.jianshu.com/p/d4fee00fe2f8) 类似，通过`index & (elements.length - 1)`计算索引。



```java
public ArrayDeque() {
    elements = new Object[16];
}
public ArrayDeque(int numElements) {
    allocateElements(numElements);
}
public ArrayDeque(Collection<? extends E> c) {
    allocateElements(c.size());
    addAll(c);
}
private void allocateElements(int numElements) {
    int initialCapacity = MIN_INITIAL_CAPACITY;
    if (numElements >= initialCapacity) {
        initialCapacity = numElements;
        initialCapacity |= (initialCapacity >>>  1);
        initialCapacity |= (initialCapacity >>>  2);
        initialCapacity |= (initialCapacity >>>  4);
        initialCapacity |= (initialCapacity >>>  8);
        initialCapacity |= (initialCapacity >>> 16);
        initialCapacity++;

        if (initialCapacity < 0)    
            initialCapacity >>>= 1;
    }
    elements = new Object[initialCapacity];
}
```

------

### ArrayDeque相关的函数

```java
//添加到head前面
public void push(E e)
public boolean offerFirst(E e)
public void addFirst(E e)

//添加到tail后面
public boolean add(E e)
public boolean offerLast(E e)
public void addLast(E e)

//删除head所在位置的元素
public E pop()
public E remove()
public E poll()
public E removeFirst()
public E pollFirst()

//删除 tail所在位置的元素
public E removeLast()
public E pollLast()

//获取head所在位置的元素
public E element()
public E peek()
public E peekFirst()

//获取tail所在位置的元素
public E peekLast()

//获取队列元素的个数
public int size()
```

##### addFirst(E e)

- 因为 `head` 和 `tail` 默认为 `0`，所以默认第一个元素存入的位置为 `element` 数组的最后的索引位置。当`head==tail` 时，说明`elements`数组满了，需要进行数组扩容，新数组长度是原来的`2`倍，然后重新赋值`head = 0` 。



```java
public void addFirst(E e) {
    if (e == null)
        throw new NullPointerException();
    elements[head = (head - 1) & (elements.length - 1)] = e;
    if (head == tail)
        doubleCapacity();
}
private void doubleCapacity() {
    assert head == tail;
    int p = head;
    int n = elements.length;
    int r = n - p; // number of elements to the right of p
    int newCapacity = n << 1;
    if (newCapacity < 0)
        throw new IllegalStateException("Sorry, deque too big");
    Object[] a = new Object[newCapacity];
    System.arraycopy(elements, p, a, 0, r);
    System.arraycopy(elements, 0, a, r, p);
    elements = a;
    head = 0;
    tail = n;
}
```

![img](https://upload-images.jianshu.io/upload_images/1709375-9d22a30da417345d.png?imageMogr2/auto-orient/strip|imageView2/2/w/691/format/webp)

扩容

------

##### addLast(E e)

- `addLast`即赋值 `tail`索引位置的值为`e`，然后`tail++`，当`head==tail` 时，同`addFirst(E e)`进行数组扩容。



```csharp
public void addLast(E e) {
    if (e == null)
        throw new NullPointerException();
    elements[tail] = e;
    if ( (tail = (tail + 1) & (elements.length - 1)) == head)
        doubleCapacity();
}
```

------

##### pollFirst()、pollLast()、peekFirst()、peekLast()

- 返回对应索引的元素。



```kotlin
public E pollFirst() {
    final Object[] elements = this.elements;
    final int h = head;
    @SuppressWarnings("unchecked")
    E result = (E) elements[h];
    // Element is null if deque empty
    if (result != null) {
        elements[h] = null; // Must null out slot
        head = (h + 1) & (elements.length - 1);
    }
    return result;
}
public E pollLast() {
    final Object[] elements = this.elements;
    final int t = (tail - 1) & (elements.length - 1);
    @SuppressWarnings("unchecked")
    E result = (E) elements[t];
    if (result != null) {
        elements[t] = null;
        tail = t;
    }
    return result;
}
public E peekFirst() {
    // elements[head] is null if deque empty
    return (E) elements[head];
}
public E peekLast() {
    return (E) elements[(tail - 1) & (elements.length - 1)];
}
```

------

##### size()



```cpp
public int size() {
    return (tail - head) & (elements.length - 1);
}
```

------

### 小结

- `ArrayDeque`是带`首尾标记`的数组实现的双端队列。