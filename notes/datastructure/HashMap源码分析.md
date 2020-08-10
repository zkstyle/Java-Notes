# HashMap源码解析

### 目录

- HashMap的常量介绍
- HashMap的构造函数
- HashMap的数据操作函数
- TreeNode介绍
- 参考文章

### HashMap的常量介绍

- ```
  static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16
  ```

  初始化默认的容量 ，必须是2的指数幂。

  > 这里可以解释一下为什么要求`table`的长度为`2`的幂
  > `n`为`2`的幂，那么化成二进制就是`100...00`，减一之后成为`0111..11`
  > 对于小于`n-1`的`hash`值，索引位置就是`hash`，大于`n-1`的就是取模，这样在获取`table`索引可以提高`&`运算的速度且最后一位为`1`，这样保证散列的`均匀性`。

- `static final int MAXIMUM_CAPACITY = 1 << 30;`
  最大的容量值。

- `static final float DEFAULT_LOAD_FACTOR = 0.75f;`
  默认的负载因子 当数量达到 **容量 \* 负载因子** 时， 则扩充当前`HashMap`的容量 为当前的`2`倍。

  [HashMap的loadFactor为什么是0.75？](https://www.jianshu.com/p/64f6de3ffcc1)

- `static final int TREEIFY_THRESHOLD = 8;`
  链表转化为树的阈值 。

- `static final int UNTREEIFY_THRESHOLD = 6;`
  树转化为链表的阈值。

- `static final int MIN_TREEIFY_CAPACITY = 64;`
  桶（`bin`）中的数据要采用红黑树结构进行存储时，整个`Table`的最小容量

------

### 构造函数



```cpp
public HashMap() {
    this.loadFactor = DEFAULT_LOAD_FACTOR; // all other fields defaulted
}
public HashMap(int initialCapacity) {
    this(initialCapacity, DEFAULT_LOAD_FACTOR);
}
public HashMap(int initialCapacity, float loadFactor) {
    if (initialCapacity < 0)
        throw new IllegalArgumentException("Illegal initial capacity: " +
                initialCapacity);
    if (initialCapacity > MAXIMUM_CAPACITY)
        initialCapacity = MAXIMUM_CAPACITY;
    if (loadFactor <= 0 || Float.isNaN(loadFactor))
        throw new IllegalArgumentException("Illegal load factor: " +
                loadFactor);
    this.loadFactor = loadFactor;
    this.threshold = tableSizeFor(initialCapacity);
}
public HashMap(Map<? extends K, ? extends V> m) {
    this.loadFactor = DEFAULT_LOAD_FACTOR;
    putMapEntries(m, false);
}
```

- 无参的构造方法，赋值`loadFactor`为默认的负载因子 `0.75`。

- 带 容量 和 负载因子的参数，分别对两个参数做 范围判断，然后赋值给

  ```
  loadFactor
  ```

  和

  ```
  threshold
  ```

  。

  通过

  ```
  tableSizeFor(int cap)
  ```

  对传入的 容量 取

   

  大于等于

   

  该值

   

  最小的2的指数幂

  。

  

  ```java
  static final int tableSizeFor(int cap) {
      int n = cap - 1;
      n |= n >>> 1;
      n |= n >>> 2;
      n |= n >>> 4;
      n |= n >>> 8;
      n |= n >>> 16;
      return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
  }
  ```

- ```
  map
  ```

  参数的构造方法，赋值

  ```
  loadFactor
  ```

  为默认的负载因子

   

  ```
  0.75
  ```

  ，然后在

  ```
  putMapEntries
  ```

  中通过

  ```
  tableSizeFor(t)
  ```

  计算当前应该分配的容量(

  ```
  2的指数幂数
  ```

  )，然后再把 传入的数据 存入。

  

  ```dart
  final void putMapEntries(Map<? extends K, ? extends V> m, boolean evict) {
      int s = m.size();
      if (s > 0) {
          if (table == null) { // pre-size
              float ft = ((float)s / loadFactor) + 1.0F;
              int t = ((ft < (float)MAXIMUM_CAPACITY) ?
                       (int)ft : MAXIMUM_CAPACITY);
              if (t > threshold)
                  threshold = tableSizeFor(t);
          }
          else if (s > threshold)
              resize();
          for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
              K key = e.getKey();
              V value = e.getValue();
              putVal(hash(key), key, value, false, evict);
          }
      }
  }
  ```

------

### 数据操作相关的方法

- `put(K key, V value)`
- `get(Object key)`
- `remove(Object key)`
- `resize()`
- `treeifyBin(Node[] tab, int hash)`

##### resize() 扩容

- `(1.0)` 如果当前的容量 已经达到 最大容量`MAXIMUM_CAPACITY`(`1<<30`)了，就不再扩容了，直接返回当前的`table`。
- `(2.0)` 如果当前的容量`>= DEFAULT_INITIAL_CAPACITY`(`16`)，并且 容量 扩大一倍之后还`< MAXIMUM_CAPACITY`，则容量扩大一倍，得到`newCap`。
- `(3.0)` 如果没有之前没有数据，则赋值 `newCap` 为默认的容量(`16`) 以及 默认的扩容阈值(`16 * 0.75`)。
- `(4.0)` 如果 扩容阈值 还为`0`，则更具当前的 **容量** 和 **负载因子** 计算 扩容阈值(`threshold`)。
- `(5.0)` 然后构建一个容量为 `newCap`的 新 `table`，把之前的数据存进去。



```java
final Node<K,V>[] resize() {
    Node<K,V>[] oldTab = table;
    int oldCap = (oldTab == null) ? 0 : oldTab.length;
    int oldThr = threshold;
    int newCap, newThr = 0;
    if (oldCap > 0) {
        if (oldCap >= MAXIMUM_CAPACITY) {
            //1.0
            threshold = Integer.MAX_VALUE;
            return oldTab;
        }
        else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                oldCap >= DEFAULT_INITIAL_CAPACITY)
            //2.0
            newThr = oldThr << 1; // double threshold
    }
    else if (oldThr > 0) // initial capacity was placed in threshold
        newCap = oldThr;
    else {               // zero initial threshold signifies using defaults
        //3.0
        newCap = DEFAULT_INITIAL_CAPACITY;
        newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
    }
    if (newThr == 0) {
        //4.0
        float ft = (float)newCap * loadFactor;
        newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                (int)ft : Integer.MAX_VALUE);
    }
    threshold = newThr;
    //5.0
    @SuppressWarnings({"rawtypes","unchecked"})
    Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
    table = newTab;
    if (oldTab != null) {
        for (int j = 0; j < oldCap; ++j) {
            Node<K,V> e;
            if ((e = oldTab[j]) != null) {
                oldTab[j] = null;
                if (e.next == null)
                    newTab[e.hash & (newCap - 1)] = e;
                else if (e instanceof TreeNode)
                    ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                else { // preserve order
                    Node<K,V> loHead = null, loTail = null;
                    Node<K,V> hiHead = null, hiTail = null;
                    Node<K,V> next;
                    do {
                        next = e.next;
                        if ((e.hash & oldCap) == 0) {
                            if (loTail == null)
                                loHead = e;
                            else
                                loTail.next = e;
                            loTail = e;
                        }
                        else {
                            if (hiTail == null)
                                hiHead = e;
                            else
                                hiTail.next = e;
                            hiTail = e;
                        }
                    } while ((e = next) != null);
                    if (loTail != null) {
                        loTail.next = null;
                        newTab[j] = loHead;
                    }
                    if (hiTail != null) {
                        hiTail.next = null;
                        newTab[j + oldCap] = hiHead;
                    }
                }
            }
        }
    }
    return newTab;
}
```

##### put(K key, V value) 存数据

分为以下三种情况：

- 插入位置无数据，直接存入当前的`key`在`table`的位置
- 插入位置有数据，但是较少且符合链表结构存储的条件，那么以链表操作存入
- 插入位置有数据，但是以树结构进行存储，那么以树的相关操作进行存入

源码解析：

- `(1.0)` 通过 `key.hashCode() ^ (key.hashCode() >>> 16)` 获取 `key` 的 `hash` 值。
- `(2.0)` 如果当前的`table` 为 `空` 或者 长度为`0` 则做一次扩容操作。
- `(3.0)` 通过 `(n-1) ^ hash` 获取 `key` 所在 `table` 的位置，如果当前的 `node=null || size = 0`，则把当前的数据构建一个新的 `node` 存在当前位置，否则看`(4.0)` 。
- `(4.0)` 如果当前 `node` 的 `hash` 和 `key` 和传入的 `hash` 和 `key` 相同，则通过`(7.0)` 更新 `node`。
- `(5.0)` 如果当前的 `node` 已经变为`TreeNode`，则执行 `TreeNode` 的 插入操作，后面介绍 `TreeNode`再详细介绍。
- `(6.0)` 遍历当前 `node` 链表，如果找到满足`(4.0)`的条件的 `node`，则通过`(7.0)` 更新 `node`，否则新建一个 `node` 添加到当前的链表最后，并判断`(6.1)`，再执行`(8.0)`。
- `(6.1)` 如果当前的node的数据已达到 `TREEIFY_THRESHOLD - 1`， 则通过`treeifyBin(tab, hash)`转化为树。
- `(7.0)` 更新当前的`node`的值为 传入的 `value`，并返回之前的`oldValue`。
- `(8.0)` 判断是否需要做扩容操作。



```csharp
public V put(K key, V value) {
    return putVal(hash(key), key, value, false, true);
}
static final int hash(Object key) {
    // 1.0
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
               boolean evict) {
    Node<K,V>[] tab; Node<K,V> p; int n, i;
    if ((tab = table) == null || (n = tab.length) == 0)
        // 2.0
        n = (tab = resize()).length;
    if ((p = tab[i = (n - 1) & hash]) == null)
        //3.0
        tab[i] = newNode(hash, key, value, null);
    else {
        Node<K,V> e; K k;
        if (p.hash == hash &&
                ((k = p.key) == key || (key != null && key.equals(k))))
            //4.0
            e = p;
        else if (p instanceof TreeNode)
            //5.0
            e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
        else {
            //6.0
            for (int binCount = 0; ; ++binCount) {
                if ((e = p.next) == null) {
                    p.next = newNode(hash, key, value, null);
                    if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                        //6.1
                        treeifyBin(tab, hash);
                    break;
                }
                if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                    break;
                p = e;
            }
        }
        //7.0
        if (e != null) { // existing mapping for key
            V oldValue = e.value;
            if (!onlyIfAbsent || oldValue == null)
                e.value = value;
            afterNodeAccess(e);
            return oldValue;
        }
    }
    //8.0
    ++modCount;
    if (++size > threshold)
        resize();
    afterNodeInsertion(evict);
    return null;
}
```

### get(Object key) 取数据

和存数据类似:

- 如果当前的 `tab` 没数据，或者没有对应的`key` 则返回`null`.
- 否则先校验第一个 `node`，再看是 通过 `getTreeNode(int h, Object k)`去树中找 还是 遍历当前的 链表。



```csharp
public V get(Object key) {
    Node<K,V> e;
    return (e = getNode(hash(key), key)) == null ? null : e.value;
}

final Node<K,V> getNode(int hash, Object key) {
    Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
    if ((tab = table) != null && (n = tab.length) > 0 &&
            (first = tab[(n - 1) & hash]) != null) {
        if (first.hash == hash && // always check first node
                ((k = first.key) == key || (key != null && key.equals(k))))
            return first;
        if ((e = first.next) != null) {
            if (first instanceof TreeNode)
                return ((TreeNode<K,V>)first).getTreeNode(hash, key);
            do {
                if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                    return e;
            } while ((e = e.next) != null);
        }
    }
    return null;
}
```

### remove(Object key) 删除数据

- `(1.0)` 通过 `tab != null && (n = tab.length) > 0 && (p = tab[index = (n - 1) & hash]) != null`校验`table`不为空，并且 在`table`上对应索引的值不为空。

- `(2.0)` 判断链表 `key` 对应位置的第一个是不是对应的`key`，是的话就赋值给 `node`。

- `(3.0)` 判断当前是否已经树化，是的话，则通过`getTreeNode`去获取对应的节点。

- `(4.0)` 是链表的话，就遍历链表找到对应的`key`的节点，赋值给 ·node`。

- `(5.0)` 判断`node`是否为空，并且对应的值是否相等。`(5.1)`树化则调用`removeTreeNode`移除树的节点；`(5.2)`如果为`key`在当前`tab`的索引位置，直接覆盖；`(5.3)` 否则通过`p.next = node.next` 直接移除`node`.

- ```
  (6.0)
  ```

   

  ```
  afterNodeRemoval
  ```

  方法默认是空实现。

  

  ```java
  // Callbacks to allow LinkedHashMap post-actions
  void afterNodeAccess(Node<K,V> p) { }
  void afterNodeInsertion(boolean evict) { }
  void afterNodeRemoval(Node<K,V> p) { }
  ```



```csharp
public V remove(Object key) {
    Node<K,V> e;
    return (e = removeNode(hash(key), key, null, false, true)) == null ?
            null : e.value;
}
final Node<K,V> removeNode(int hash, Object key, Object value,
                           boolean matchValue, boolean movable) {
    Node<K,V>[] tab; Node<K,V> p; int n, index;
    if ((tab = table) != null && (n = tab.length) > 0 &&
            (p = tab[index = (n - 1) & hash]) != null) { //1.0
        Node<K,V> node = null, e; K k; V v;
        if (p.hash == hash &&
                ((k = p.key) == key || (key != null && key.equals(k))))
            node = p;//2.0
        else if ((e = p.next) != null) {
            if (p instanceof TreeNode)//3.0
                node = ((TreeNode<K,V>)p).getTreeNode(hash, key);
            else {//4.0
                do {
                    if (e.hash == hash &&
                            ((k = e.key) == key ||
                                    (key != null && key.equals(k)))) {
                        node = e;
                        break;
                    }
                    p = e;
                } while ((e = e.next) != null);
            }
        }
        if (node != null && (!matchValue || (v = node.value) == value ||
                (value != null && value.equals(v)))) { //5.0
            if (node instanceof TreeNode)//5.1
                ((TreeNode<K,V>)node).removeTreeNode(this, tab, movable);
            else if (node == p)//5.2
                tab[index] = node.next;
            else//5.3
                p.next = node.next;
            ++modCount;
            --size;
            afterNodeRemoval(node);//6.0
            return node;
        }
    }
    return null;
}
```

### treeifyBin(Node<K,V>[] tab, int hash) 链表树化

- `(1.0)` 没有达到树化的最小数量`MIN_TREEIFY_CAPACITY`，则进行扩容操作。
- `(2.0)` 满足树化的条件，则把链表的每个节点都转化为 `TreeNode`。
- `(3.0)` 通过`TreeNode`的`treeify(Node[] tab)`方法构建树。



```java
final void treeifyBin(Node<K,V>[] tab, int hash) {
    int n, index; Node<K,V> e;
    if (tab == null || (n = tab.length) < MIN_TREEIFY_CAPACITY)//1.0
        resize();
    else if ((e = tab[index = (n - 1) & hash]) != null) {
        TreeNode<K,V> hd = null, tl = null;
        do {//2.0
            TreeNode<K,V> p = replacementTreeNode(e, null);
            if (tl == null)
                hd = p;
            else {
                p.prev = tl;
                tl.next = p;
            }
            tl = p;
        } while ((e = e.next) != null);
        if ((tab[index] = hd) != null)
            hd.treeify(tab);//3.0
    }
}
```

------

### TreeNode介绍

`TreeNode` 继承自 `LinkedHashMap.LinkedHashMapEntry`，变量包括，`parent`：父节点 、`left`：左节点、`right`：右节点、`prev`：删除后断开连接、`red`：是否是红黑树的红节点([红黑树的介绍](https://links.jianshu.com/go?to=http%3A%2F%2Fwww.360doc.com%2Fcontent%2F18%2F0904%2F19%2F25944647_783893127.shtml))。



```ruby
static final class TreeNode<K,V> extends LinkedHashMap.LinkedHashMapEntry<K,V> {
    TreeNode<K,V> parent;  // red-black tree links
    TreeNode<K,V> left;
    TreeNode<K,V> right;
    TreeNode<K,V> prev;    // needed to unlink next upon deletion
    boolean red;
    TreeNode(int hash, K key, V val, Node<K,V> next) {
        super(hash, key, val, next);
    }
```

主要有以下方法：

- `root()`：获取根节点
- `getTreeNode(int h, Object k)`：查找节点
- `treeify(Node[] tab)`：链表转树
- `untreeify(HashMap map)`：树转链表
- `putTreeVal(HashMap map, Node[] tab, int h, K k, V v)`： 插入数据操作
- `removeTreeNode(HashMap map, Node[] tab, boolean movable)`：移除数据操作
- `rotateLeft(TreeNode root, TreeNode p)`：红黑树的左旋操作
- `rotateRight(TreeNode root, TreeNode p)`：红黑树的右旋操作
- `balanceInsertion(TreeNode root, TreeNode x)`：红黑树的插入数据之后的自平衡
- `balanceDeletion(TreeNode root, TreeNode x)`：红黑树的删除数据之后的自平衡

#### getTreeNode 查找节点

从根节点开始查找，大于则查找右子树，小于则查找左子树。



```kotlin
final TreeNode<K,V> getTreeNode(int h, Object k) {
    return ((parent != null) ? root() : this).find(h, k, null);
}
final TreeNode<K,V> find(int h, Object k, Class<?> kc) {
    TreeNode<K,V> p = this;
    do {
        int ph, dir; K pk;
        TreeNode<K,V> pl = p.left, pr = p.right, q;
        if ((ph = p.hash) > h)
            p = pl;
        else if (ph < h)
            p = pr;
        else if ((pk = p.key) == k || (k != null && k.equals(pk)))
            return p;
        else if (pl == null)
            p = pr;
        else if (pr == null)
            p = pl;
        else if ((kc != null ||
                (kc = comparableClassFor(k)) != null) &&
                (dir = compareComparables(kc, k, pk)) != 0)
            p = (dir < 0) ? pl : pr;
        else if ((q = pr.find(h, k, kc)) != null)
            return q;
        else
            p = pl;
    } while (p != null);
    return null;
}
```

#### treeify 链表转树

- `(1.0)` 首先构建根节点
- `(2.0)` 比较`hash`值的大小
- `(3.0)` 根据上一步比较的大小存入左子树还是右子树。



```java
final void treeify(Node<K,V>[] tab) {
    TreeNode<K,V> root = null;
    for (TreeNode<K,V> x = this, next; x != null; x = next) {
        next = (TreeNode<K,V>)x.next;
        x.left = x.right = null;
        if (root == null) { //1.0
            x.parent = null;
            x.red = false;
            root = x;
        } else {
            K k = x.key;
            int h = x.hash;
            Class<?> kc = null;
            for (TreeNode<K,V> p = root;;) {
                int dir, ph;
                K pk = p.key;
                if ((ph = p.hash) > h)//2.0
                    dir = -1;
                else if (ph < h)
                    dir = 1;
                else if ((kc == null &&
                        (kc = comparableClassFor(k)) == null) ||
                        (dir = compareComparables(kc, k, pk)) == 0)
                    dir = tieBreakOrder(k, pk);

                TreeNode<K,V> xp = p;
                if ((p = (dir <= 0) ? p.left : p.right) == null) {//3.0
                    x.parent = xp;
                    if (dir <= 0)
                        xp.left = x;
                    else
                        xp.right = x;
                    root = balanceInsertion(root, x);
                    break;
                }
            }
        }
    }
    moveRootToFront(tab, root);
}
```

#### untreeify 树转链表



```kotlin
/**
 * Returns a list of non-TreeNodes replacing those linked from this node.
 */
final Node<K,V> untreeify(HashMap<K,V> map) {
    Node<K,V> hd = null, tl = null;
    for (Node<K,V> q = this; q != null; q = q.next) {
        Node<K,V> p = map.replacementNode(q, null);
        if (tl == null)
            hd = p;
        else
            tl.next = p;
        tl = p;
    }
    return hd;
}
```

#### putTreeVal 插入数据

- `(1.0)` 比较`hash`值的大小，大于或者小于则查找子树，相等则直接返回。
- `(2.0)` 当 `key` 相等的时候则返回当前的节点
- `(3.0)` 根据比较`hash`值的大小的结果更新 `p` 为 左子树还是右子树。



```java
final TreeNode<K,V> putTreeVal(HashMap<K,V> map, Node<K,V>[] tab,
                               int h, K k, V v) {
    Class<?> kc = null;
    boolean searched = false;
    TreeNode<K,V> root = (parent != null) ? root() : this;
    for (TreeNode<K,V> p = root;;) {
        int dir, ph; K pk;
        //1.0
        if ((ph = p.hash) > h)
            dir = -1;
        else if (ph < h)
            dir = 1;
        else if ((pk = p.key) == k || (k != null && k.equals(pk)))
            return p;//2.0
        else if ((kc == null &&
                (kc = comparableClassFor(k)) == null) ||
                (dir = compareComparables(kc, k, pk)) == 0) {
            if (!searched) {
                TreeNode<K,V> q, ch;
                searched = true;
                if (((ch = p.left) != null &&
                        (q = ch.find(h, k, kc)) != null) ||
                        ((ch = p.right) != null &&
                                (q = ch.find(h, k, kc)) != null))
                    return q;
            }
            dir = tieBreakOrder(k, pk);
        }

        TreeNode<K,V> xp = p;
        if ((p = (dir <= 0) ? p.left : p.right) == null) {//3.0
            Node<K,V> xpn = xp.next;
            TreeNode<K,V> x = map.newTreeNode(h, k, v, xpn);
            if (dir <= 0)
                xp.left = x;
            else
                xp.right = x;
            xp.next = x;
            x.parent = x.prev = xp;
            if (xpn != null)
                ((TreeNode<K,V>)xpn).prev = x;
            moveRootToFront(tab, balanceInsertion(root, x));
            return null;
        }
    }
}
```

#### removeTreeNode移除数据

- `(1.0) (1.1) (1.2)` tab为空、根节点为空 或者 树的数量太少 则直接返回 `null`，当树的数量太少则转为链表。
- `(2.0)` 寻找要替换的点。
- `(3.0)` 如果要移除的不是当前这个`treenode`， 则替换当前节点的 父节点或者其左右节点。
- `(4.0)` 如果是当前节点是红黑树的黑树则通过`balanceDeletion`实现平衡。
- `(5.0)` 如果要移除的是当前这个`treenode`，则从树中移除当前节点。



```java
final void removeTreeNode(HashMap<K,V> map, Node<K,V>[] tab,
                          boolean movable) {
    int n;
    if (tab == null || (n = tab.length) == 0)//1.0
        return;
    ...
    if (first == null)//1.1
        return;
    ...
    if (root == null || root.right == null ||
            (rl = root.left) == null || rl.left == null) {//1.2
        tab[index] = first.untreeify(map);  // too small
        return;
    }
    //2.0
    TreeNode<K,V> p = this, pl = left, pr = right, replacement;
    if (pl != null && pr != null) {//寻找要替换的点
        ... //省略查找的代码
        if (sr != null)
            replacement = sr;
        else
            replacement = p;
    }
    else if (pl != null)
        replacement = pl;
    else if (pr != null)
        replacement = pr;
    else
        replacement = p;

    if (replacement != p) {//3.0
        TreeNode<K,V> pp = replacement.parent = p.parent;
        if (pp == null)
            root = replacement;
        else if (p == pp.left)
            pp.left = replacement;
        else
            pp.right = replacement;
        p.left = p.right = p.parent = null;
    }

    TreeNode<K,V> r = p.red ? root : balanceDeletion(root, replacement);//4.0
    //5.0
    if (replacement == p) {  // detach
        TreeNode<K,V> pp = p.parent;
        p.parent = null;
        if (pp != null) {
            if (p == pp.left)
                pp.left = null;
            else if (p == pp.right)
                pp.right = null;
        }
    }
    if (movable)
        moveRootToFront(tab, r);
}
```