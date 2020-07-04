# LinkedList源码解析

### 目录

- 简介
- `LinkedList`的成员变量介绍
- `LinkedList`的构造函数
- `LinkedList`的数据操作函数
- 小结

------

### 简介

> `LinkedList` 和 `ArrayList` 一样，都实现了 `List` 接口，但其内部的数据结构有本质的不同。`LinkedList` 是基于 **链表** 实现的（通过名字也能区分开来），所以它的 **插入和删除** 操作比 `ArrayList` 更加高效。但也是由于其为基于链表的，所以 **随机访问的效率** 要比 `ArrayList` 差。

> `LinkedList` 继承自 `AbstractSequenceList`，实现了 `List`、`Deque`、`Cloneable`、`java.io.Serializable` 接口。`AbstractSequenceList` 提供了`List`接口骨干性的实现以减少实现 `List` 接口的复杂度，`Deque` 接口定义了双端队列的操作。

> 在 `LinkedList` 中除了本身自己的方法外，还提供了一些可以使其作为栈、队列或者双端队列的方法。这些方法可能彼此之间只是名字不同，以使得这些名字在特定的环境中显得更加合适。

> `LinkedList` 也是 [fail-fast](https://links.jianshu.com/go?to=https%3A%2F%2Fwiki.jikexueyuan.com%2Fproject%2Fjava-enhancement%2Fjava-thirtyfour.html) 的（前边提过很多次了）。

------

### LinkedList的成员变量介绍

- `transient int size = 0;`：链表长度
- `transient Node first;`：头节点
- `transient Node last;`：尾节点

------

### LinkedList的构造函数

无参构造函数没做啥操作，有参的则是进行数据添加操作，下面会介绍。



```cpp
public LinkedList() {
}

public LinkedList(Collection<? extends E> c) {
    this();
    addAll(c);
}
```

------

### LinkedList的数据操作函数

- `node(int index)`：获取对应节点
- `addFirst(E e)` and `addLast(E e)`： 添加头尾节点
- `getFirst()` and `getLast()`： 获取头尾节点
- `remove()` 、`removeFirst()` and `removeLast()`：删除头尾节点
- `remove(int index)` and `remove(Object o)`： 删除对象
- `get(int index)` and `set(int index, E element)`：获取和设置对应节点
- `peek()`、`peekFirst()` and `peekLast()`：获取对应节点的值
- `element()`：获取头节点的值
- `poll()`、`pollFirst()` and `pollLast()`：返回对应节点，并从链表中删除
- `offer(E e)`、`offerFirst(E e)` and `offerLast(E e)`：添加节点
- `push(E e)`：插入一个节点到头部
- `pop()`：删除头部一个节点

##### node(int index) 获取对应节点

- 根据索引的位置靠近头还是尾，靠近那头则从那头开始遍历查找。



```csharp
Node<E> node(int index) {
    if (index < (size >> 1)) {
        Node<E> x = first;
        for (int i = 0; i < index; i++)
            x = x.next;
        return x;
    } else {
        Node<E> x = last;
        for (int i = size - 1; i > index; i--)
            x = x.prev;
        return x;
    }
}
```

##### addFirst(E e) and addLast(E e) 添加头尾节点

- `addFirst(E e)` 构建节点`newNode`,赋值`first = newNode`，如果之前的头节点`f`为空则将`last`设置为`newNode`，否则将`f`的`prev`节点为`newNode`.
- `addLast(E e)` 构建节点`newNode`,赋值`last = newNode`，如果之前的尾节点`l`为空则将`first`设置为`newNode`，否则将`l`的`next`节点为`newNode`.



```java
public void addFirst(E e) {
    linkFirst(e);
}
private void linkFirst(E e) {
    final Node<E> f = first;
    final Node<E> newNode = new Node<>(null, e, f);
    first = newNode;
    if (f == null)
        last = newNode;
    else
        f.prev = newNode;
    size++;
    modCount++;
}
public void addLast(E e) {
    linkLast(e);
}
void linkLast(E e) {
    final Node<E> l = last;
    final Node<E> newNode = new Node<>(l, e, null);
    last = newNode;
    if (l == null)
        first = newNode;
    else
        l.next = newNode;
    size++;
    modCount++;
}
```

##### element() 、getFirst() and getLast() 获取头尾节点

- 如果头尾节点为空则会抛出异常`NoSuchElementException`，否则返回对应的`item`值。



```java
public E element() {
    return getFirst();
}
public E getFirst() {
    final Node<E> f = first;
    if (f == null)
        throw new NoSuchElementException();
    return f.item;
}
public E getLast() {
    final Node<E> l = last;
    if (l == null)
        throw new NoSuchElementException();
    return l.item;
}
```

##### remove()、removeFirst() and removeLast() 删除头尾节点

- `remove()`即为删除头节点
- 删除即为更新节点的`prev` 和 `next` 节点。



```java
public E remove() {
    return removeFirst();
}
public E removeFirst() {
    final Node<E> f = first;
    if (f == null)
        throw new NoSuchElementException();
    return unlinkFirst(f);
}
public E removeLast() {
    final Node<E> l = last;
    if (l == null)
        throw new NoSuchElementException();
    return unlinkLast(l);
}
private E unlinkFirst(Node<E> f) {
    final E element = f.item;
    final Node<E> next = f.next;
    f.item = null;
    f.next = null; // help GC
    first = next;
    if (next == null)
        last = null;
    else
        next.prev = null;
    size--;
    modCount++;
    return element;
}
private E unlinkLast(Node<E> l) {
    final E element = l.item;
    final Node<E> prev = l.prev;
    l.item = null;
    l.prev = null; // help GC
    last = prev;
    if (prev == null)
        first = null;
    else
        prev.next = null;
    size--;
    modCount++;
    return element;
}
```

##### remove(int index) and remove(Object o) 删除对象

- `remove(int index)`会检查下标，然后删除节点
- `remove(Object o)` 则会更具 `o` 是否为 `null` 用 `=` 或 `equals` 找到对应的节点，再删除。删除成功会返回`ture`，失败则为`false`.



```csharp
public E remove(int index) {
    checkElementIndex(index);
    return unlink(node(index));
}
public boolean remove(Object o) {
    if (o == null) {
        for (Node<E> x = first; x != null; x = x.next) {
            if (x.item == null) {
                unlink(x);
                return true;
            }
        }
    } else {
        for (Node<E> x = first; x != null; x = x.next) {
            if (o.equals(x.item)) {
                unlink(x);
                return true;
            }
        }
    }
    return false;
}
```

##### get(int index) and set(int index, E element)

- 都先检查索引，`get`直接返回节点的值，`set`则修改节点的值并返回之前的旧值。



```cpp
public E get(int index) {
    checkElementIndex(index);
    return node(index).item;
}
public E set(int index, E element) {
    checkElementIndex(index);
    Node<E> x = node(index);
    E oldVal = x.item;
    x.item = element;
    return oldVal;
}
```

##### peek()、peekFirst() and peekLast()

- 返回头尾节点的值，如果节点为`null` 再返回 `null`.



```java
public E peek() {
    final Node<E> f = first;
    return (f == null) ? null : f.item;
}
public E peekFirst() {
    final Node<E> f = first;
    return (f == null) ? null : f.item;
}
public E peekLast() {
    final Node<E> l = last;
    return (l == null) ? null : l.item;
}
```

##### poll()、pollFirst() and pollLast()

- 返回头尾节点，并总链表中移除



```java
public E poll() {
    final Node<E> f = first;
    return (f == null) ? null : unlinkFirst(f);
}
public E pollFirst() {
    final Node<E> f = first;
    return (f == null) ? null : unlinkFirst(f);
}
public E pollLast() {
    final Node<E> l = last;
    return (l == null) ? null : unlinkLast(l);
}
```

##### offer(E e)、offerFirst(E e) and offerLast(E e)

- 添加节点



```java
public boolean offer(E e) {
    return add(e);
}
public boolean offerFirst(E e) {
    addFirst(e);
    return true;
}
public boolean offerLast(E e) {
    addLast(e);
    return true;
}
```

##### push(E e)

- 插入一个节点到头部



```cpp
public void push(E e) {
    addFirst(e);
}
```

##### pop()

- 删除头节点



```cpp
public E pop() {
    return removeFirst();
}
```

------

### 小结

- LinkedList查找类似二分查找， 靠近那边则从那边开始查。