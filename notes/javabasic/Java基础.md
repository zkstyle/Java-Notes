# java基础汇总
## 简要说一下final关键字，final可以用来修饰什么？

final可以修饰类、方法、成员变量

当final修饰类的时候，说明该类不能被继承

当final修饰方法的时候，说明该方法不能被重写

在早期，可能使用final修饰的方法，编译器针对这些方法的所有调用都转成内嵌调用，这样提高效率

当final修饰成员变量时，有两种情况：

如果修饰的是基本类型，说明这个变量的所代表数值永不能变(不能重新赋值)！

如果修饰的是引用类型，该变量所引用的不能变，但引用所代表的对象内容是可变的！

值得一说的是：并不是被final修饰的成员变量就一定是编译期常量了。比如说我们可以写出这样的代码：

> private final int ran = new Random().nextInt(20);

## Exception与Error区别

首先Exception和Error都是继承于Throwable 类，在 Java 中只有 Throwable 类型的实例才可以被抛出（throw）或者捕获（catch），它是异常处理机制的基本组成类型。

Exception和Error体现了JAVA这门语言对于异常处理的两种方式。

Exception是java程序运行中可预料的异常情况，咱们可以获取到这种异常，并且对这种异常进行业务外的处理。

Error是java程序运行中不可预料的异常情况，这种异常发生以后，会直接导致JVM不可处理或者不可恢复的情况。所以这种异常不可能抓取到，比如OutOfMemoryError、NoClassDefFoundError等。

其中的Exception又分为检查性异常和非检查性异常。两个根本的区别在于，检查性异常 必须在编写代码时，使用try catch捕获（比如：IOException异常）。

非检查性异常 在代码编写使，可以忽略捕获操作（比如：ArrayIndexOutOfBoundsException），这种异常是在代码编写或者使用过程中通过规范可以避免发生的。

## 反射机制与方式
- 在反射机制中，总共有如下3种方法可以获取到Class类：
1.Class.forName("类的路径")
2.类名.Class
3.实例.getClass()
- 

## 请列出5个运行时异常与检查异常
运行时异常:
> ClassCastException(类转换异常)
  IndexOutOfBoundsException(数组越界)
  NullPointerException(空指针)
  ArithmeticException(算术异常)
  ConcurrentModificationException(并发修改异常,hashMap->modCount发生改变时抛出此异常)

检查异常:
> SQLException(SQL异常)
  IOException(IO异常，在对流操作时有可能会出现的异常)
  FileNotFoundException(找不到某个文件时，会抛出该异常)
  ClassNotFoundException(找不到某个类时，会抛出该异常)
  EOFException (输入过程中意外地到达文件尾或流尾，会抛出该异常，常见于对流的操作)

## 有没有有顺序的Map实现类，如果有，他们是怎么实现有序的？
- Hashmap和Hashtable 都不是有序的。
- TreeMap和LinkedHashmap都是有序的。（TreeMap默认是key升序，LinkedHashmap默认是数据插入顺序）
- TreeMap是基于比较器Comparator来实现有序的。LinkedHashmap是基于链表来实现数据插入有序的。

## 抽象类和接口区别
含有abstract修饰符的class即为抽象类，abstract 类不能创建实例对象。含有abstract方法的类必须定义为abstract class，abstract class类中的方法不必是抽象的。

abstract class类中定义抽象方法必须在具体(Concrete)子类中实现，所以，不能有抽象构造方法或抽象静态方法。如果子类没有实现抽象父类中的所有抽象方法，那么子类也必须定义为abstract类型。

接口（interface）可以说成是抽象类的一种特例，接口中的所有方法都必须是抽象的。接口中的方法定义默认为public abstract类型，接口中的成员变量类型默认为public static final。

下面比较一下两者的语法区别：

1.抽象类可以有构造方法，接口中不能有构造方法。

2.抽象类中可以有普通成员变量，接口中没有普通成员变量

3.抽象类中可以包含非抽象的普通方法，接口中的所有方法必须都是抽象的，不能有非抽象的普通方法。

4. 抽象类中的抽象方法的访问类型可以是public，protected和　默认类型，但接口中的抽象方法只能是public类型的，并且默认即为public abstract类型。

5. 抽象类中可以包含静态方法，接口中不能包含静态方法(jdk1.8以后接口可以有静态方法)

6. 抽象类和接口中都可以包含静态成员变量，抽象类中的静态成员变量的访问类型可以任意，但接口中定义的变量只能是public static final类型，并且默认即为public static final类型。

7. 一个类可以实现多个接口，但只能继承一个抽象类。

两者在应用上的区别：

接口更多的是在系统架构设计方法发挥作用，主要用于定义模块之间的通信契约。而抽象类在代码实现方面发挥作用，可以实现代码的重用

## java实例化顺序
1.父类的静态变量，静态代码块（先声明的先执行）

2.子类的静态变量，静态代码块（先声明的先执行）

3.父类的变量和代码块

4.父类的构造函数

5.子类的变量和代码块

6.子类的构造函数

## 请结合OO设计理念，谈谈访问修饰符public、private、protected、default在应用设计中的作用。
访问修饰符，主要标示修饰块的作用域，方便隔离防护。

public： Java语言中访问限制最宽的修饰符，一般称之为“公共的”。被其修饰的类、属性以及方法不仅可以跨类访问，而且允许跨包（package）访问。

private: Java语言中对访问权限限制的最窄的修饰符，一般称之为“私有的”。被其修饰的类、属性以及方法只能被该类的对象访问，其子类不能访问，更不能允许跨包访问。

protect: 介于public 和 private 之间的一种访问修饰符，一般称之为“保护形”。被其修饰的类、属性以及方法只能被类本身的方法及子类访问，即使子类在不同的包中也可以访问。

default：即不加任何访问修饰符，通常称为“默认访问模式“。该模式下，只允许在同一个包中进行访问





