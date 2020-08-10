# HashSet源码解析

------

### 目录

- `HashSet`的成员变量
- `HashSet`的构造方法
- `HashSet`的数据操作方法
- 小结

------

### HashSet的成员变量

- `private transient HashMap map;`

  维护了一个`HashMap`。

- `private static final Object PRESENT = new Object();`

  保存进`HashMap`中的值。

### HashSet的构造方法

- 即调用了`HashMap`的各种初始化方法。



```cpp
public HashSet() {
    map = new HashMap<>();
}
public HashSet(Collection<? extends E> c) {
    map = new HashMap<>(Math.max((int) (c.size()/.75f) + 1, 16));
    addAll(c);
}
public HashSet(int initialCapacity, float loadFactor) {
    map = new HashMap<>(initialCapacity, loadFactor);
}
public HashSet(int initialCapacity) {
    map = new HashMap<>(initialCapacity);
}
HashSet(int initialCapacity, float loadFactor, boolean dummy) {
    map = new LinkedHashMap<>(initialCapacity, loadFactor);
}
```

### HashSet的数据操作方法

- 可以发现`HashSet`的数据操作都是通过构建的全局变量`HashMap`完成的。



```cpp
public boolean contains(Object o) {
    return map.containsKey(o);
}
public boolean add(E e) {
    return map.put(e, PRESENT) == null;
}
public boolean remove(Object o) {
    return map.remove(o) == PRESENT;
}
public void clear() {
    map.clear();
}
public int size() {
    return map.size();
}
public boolean isEmpty() {
    return map.isEmpty();
}
public Iterator<E> iterator() {
    return map.keySet().iterator();
}
```

### 小结

- `HashSet`实际上就是通过`HashMap`保存 `key` 为 `E` ，值为`PRESENT = new Object()`。
- 对应的数据操作即为`HashMap` 的 `key` 的操作。