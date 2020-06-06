# 源码解读 String、StringBuffer、StringBuilder区别

> 引言：
> 相信大多数面试都会问到关于Java中的String、StringBuffer、StringBuilder之间的区别。
> 可能很多人会直接通过百度查看直接最后结果，但今天从源码维度谈谈为什么三者会有各自的区别和特点。

## 一、String、StringBuffer、StringBuilder三者之间的类结构关系

Stringl类



```java
public final class String
    implements java.io.Serializable, Comparable<String>, CharSequence {}
```

StringBuffer

```java
public final class StringBuffer
    extends AbstractStringBuilder
    implements java.io.Serializable, CharSequence
{}
```
StringBuilder

```java
public final class StringBuilder
    extends AbstractStringBuilder
    implements java.io.Serializable, CharSequence
{}
```
String、StringBuffer、StringBuilder 三者的类关系图

![img](https://upload-images.jianshu.io/upload_images/1592745-13865027eff49514.png?imageMogr2/auto-orient/strip|imageView2/2/w/504/format/webp)

String、StringBuffer、StringBuilder类关系图



可以发现StringBuffer 与StringBuilder是继承了AbstractStringBuilder抽象类。

## 二、String部分源码解读

这里就针对String类核心关键代码解读，final 数组、多个构造方法、部分主要方法。

####  final 数组 与部分构造方法

```csharp
    private final char value[];

    public String() {
        this.value = "".value;
    }

    public String(String original) {
        this.value = original.value;
        this.hash = original.hash;
    }

    public String(char value[]) {
        this.value = Arrays.copyOf(value, value.length);
    }
```

从上面的构造方法可以看出，实际上String类是将内容存放在`final value[]`数组中。
因为value[]数组是final的，所以这也就代表着在给对象赋值新的值时，因final value[] 常量，所以每次赋值都是产生新对象。例如：



```rust
String str = "abc";
str = "bcd";
```

如果执行以下代码操作，jvm方法区中是生成两个对象后，再进行计算获得最后的内容。



```rust
String str = "abc";
str = str + "def";
```

## 三、StringBuffer、StringBuilder部分源码解读

为什么要将StringBuffer和StringBuilder一起解读？ 因为两个源码几乎一样，不一样的是StringBuffer的操作方法用了synchronized字段。

#### ① 数组 与部分构造方法

StringBuffer类



```java
 public final class StringBuffer
    extends AbstractStringBuilder
    implements java.io.Serializable, CharSequence
{
    private transient char[] toStringCache;
    
    static final long serialVersionUID = 3388685877147921107L;
    
    public StringBuffer() {
        super(16);
    }
    
    public StringBuffer(int capacity) {
        super(capacity);
    }
    
    public StringBuffer(String str) {
        super(str.length() + 16);
        append(str);
    }
    
    public StringBuffer(CharSequence seq) {
        this(seq.length() + 16);
        append(seq);
    }   
```

StringBuilder类



```java
public final class StringBuilder
    extends AbstractStringBuilder
    implements java.io.Serializable, CharSequence
{
    public StringBuilder() {
        super(16);
    }
    
    public StringBuilder(int capacity) {
        super(capacity);
    }
    
    public StringBuilder(String str) {
        super(str.length() + 16);
        append(str);
    }
    
    public StringBuilder(CharSequence seq) {
        this(seq.length() + 16);
        append(seq);
    }
```

StringBuffer 和StringBuilder类都是继承了AbstractStringBuilder类，同时构造方法中都传入了(+16)长度给父类。下面继续跟进下AbstractStringBuilder类。



```csharp
abstract class AbstractStringBuilder implements Appendable, CharSequence {
    char[] value;

    int count;

    AbstractStringBuilder() {
    }

    AbstractStringBuilder(int capacity) {
        value = new char[capacity];
    }
```

从抽象类AbstractStringBuilder解读，带有数组常量value ，并将对应内容存在了value中。

#### ② StringBuffer与StringBuilder的其他方法

StringBuffer类的其他方法，如图所示都加入了synchronized字段



![img](https://upload-images.jianshu.io/upload_images/1592745-7b1380af63e79c56.png?imageMogr2/auto-orient/strip|imageView2/2/w/785/format/webp)

StringBuffer其他方法



StringBuilder类的其他方法，如图所示没有加入了synchronized字段



![img](https://upload-images.jianshu.io/upload_images/1592745-e711189f42938b59.png?imageMogr2/auto-orient/strip|imageView2/2/w/563/format/webp)

StringBuilder类的其他方法

#### ③ 重点讲解下append方法

StringBuffer和StringBuilder有很多方法，因为常用的append操作，所以这里重点介绍下append()方法。
先看下StringBuffer、StringBuilder的源代码



```java
 // StringBuffer类
    @Override
    public synchronized StringBuffer append(String str) {
        toStringCache = null;
        super.append(str);
        return this;
    }

// StringBuilder类
    @Override
    public StringBuilder append(String str) {
        super.append(str);
        return this;
    }
```

StringBuffer和StringBuilder都重写了父类AbstractStringBuilder的方法，并且同时调用了父类的append()方法。
接着跟进查看AbstractStringBuilder类的append()方法查看源码。



```csharp
    public AbstractStringBuilder append(String str) {
        if (str == null)
            return appendNull();
        int len = str.length();
        ensureCapacityInternal(count + len);  
        str.getChars(0, len, value, count);
        count += len;
        return this;
    }
```

上面的代码有两个关键语句：

1. 是`ensureCapacityInternal(count + len);`这个语句里两个操作，a. 为数组value[]扩容 b. 将旧数组搬移到新扩容数组中。
2. 是`str.getChars(0, len, value, count);` 这个语句是通过调用String.getChars()方法，正式将value数组与str数组相连接。

#### ④ 进一步解读`ensureCapacityInternal(count + len);`与 `str.getChars(0, len, value, count);`

##### A. 解读`ensureCapacityInternal(count + len);`



```csharp
    private void ensureCapacityInternal(int minimumCapacity) {
        // overflow-conscious code
        if (minimumCapacity - value.length > 0) {
            value = Arrays.copyOf(value,
                    newCapacity(minimumCapacity));
        }
    }
```

这个ensureCapacityInternal()确认容量内部方法通过判断新的容量大小是否大于以有容量。如果打算申请的容量大于已有容量，则进行 `newCapacity(minimumCapacity)`容量申请 + `Arrays.copyOf()`数组复制两个操作。接着跟进查看 `newCapacity(minimumCapacity)`容量申请方法。



```csharp
    private int newCapacity(int minCapacity) {
        // overflow-conscious code
        int newCapacity = (value.length << 1) + 2;
        if (newCapacity - minCapacity < 0) {
            newCapacity = minCapacity;
        }
        return (newCapacity <= 0 || MAX_ARRAY_SIZE - newCapacity < 0)
            ? hugeCapacity(minCapacity)
            : newCapacity;
    }
```

###### 创建新容量大小newCapacity()方法：

- 首先，自动扩容的容量newCapacity 大小为`(value.length << 1) + 2`旧长度x2 + 2，也就是如果旧容量是16，则自动扩容的容量是 16x2 +2 = 34 。

- 接着，如果自动扩容的容量newCapacity 小于 预期申请的minCapacity ，则将预期申请的minCapacity赋值给自动扩容的容量newCapacity。

- 然后，同时还进行了容量上下限大小判断

  ```
  newCapacity <= 0 || MAX_ARRAY_SIZE - newCapacity < 0
  ```

  

  ```cpp
  private int hugeCapacity(int minCapacity) {
      if (Integer.MAX_VALUE - minCapacity < 0) { // overflow
          throw new OutOfMemoryError();
      }
      return (minCapacity > MAX_ARRAY_SIZE)
          ? minCapacity : MAX_ARRAY_SIZE;
  }
  ```

  如果自动申请容量超过

  ```
  Integer.MAX_VALUE public static final int MAX_VALUE = 0x7fffffff;
  ```

  值后抛异常。

- 最后，返回 自动申请的容量大小。

###### 旧数组拷贝Arrays.copyOf()

在经过上面容量申请确认后，则将旧数组拷贝到新容量的数组中。



```csharp
    public static char[] copyOf(char[] original, int newLength) {
        char[] copy = new char[newLength];
        System.arraycopy(original, 0, copy, 0,
                         Math.min(original.length, newLength));
        return copy;
    }
```

##### B. 解读 `str.getChars(0, len, value, count);`

str.getChars()调用的是String的getChars()方法，其实就是System.arraycopy()系统数组拷贝方式。
直接上代码：



```csharp
    public void getChars(int srcBegin, int srcEnd, char dst[], int dstBegin) {
        if (srcBegin < 0) {
            throw new StringIndexOutOfBoundsException(srcBegin);
        }
        if (srcEnd > value.length) {
            throw new StringIndexOutOfBoundsException(srcEnd);
        }
        if (srcBegin > srcEnd) {
            throw new StringIndexOutOfBoundsException(srcEnd - srcBegin);
        }
        System.arraycopy(value, srcBegin, dst, dstBegin, srcEnd - srcBegin);
    }
```

## 总结：

String类存放在常量final value[]数组中，StringBuffer、StringBuilder存放在变量value[]中，String对象一但创建后对象是不能变更的（重新赋值话每次都是重新创建对象和赋值操作），而StringBuffer、StringBuilder是可以变更的。故此，运行速度：`StringBuilder > StringBuffer > String`
StringBuffer 方法有synchronized ，故此线程安全。StringBuffer方法没有synchronized，故此线程不安全。两者没有其他差异。

- String: 不可变字符序列-常量
- StringBuffer：可变字符序列-变量、效率低、线程安全
- StringBuilder：可变字符序列-变量、效率搞、线程不安全

| String                                                       | StringBuffer                                                 | StringBuilder                                                |
| ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| String是不可变常量 final value[]                             | StringBuffer变量 value[]                                     | StringBuilder变量 value[] 数组                               |
| String每次操作都会生成新的String对象，不仅效率低下，而且浪费空间 | StringBuffer 操作不会产生新的对象。StringBuffer自身带有缓冲容量，当字符串大小没有超过容量时，不会分配新容量；当字符串超过容量时，会自动增加容量。 | StringBuilder 操作不会产生新的对象。StringBuilder 自身带有缓冲容量，当字符串大小没有超过容量时，不会分配新容量；当字符串超过容量时，会自动增加容量。 |
| 不可变                                                       | 可变                                                         | 可变                                                         |
| -                                                            | 线程安全                                                     | 线程不安全                                                   |
| -                                                            | 支持多线程操作字符串                                         | 只支持单线程操作字符串                                       |