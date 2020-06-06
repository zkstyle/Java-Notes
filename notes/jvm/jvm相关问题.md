# JVM相关
## 什么情况下会发生堆内存溢出，栈内存溢出
一、 栈溢出(StackOverflowError)
栈是线程私有的，他的生命周期与线程相同，每个方法在执行的时候都会创建一个栈帧，用来存储局部变量表，操作数栈，动态链接，方法出口灯信息。

局部变量表又包含基本数据类型，对象引用类型（局部变量表编译器完成，运行期间不会变化）

栈溢出可以理解方法执行时创建的栈帧超过了栈的深度。那么最有可能的就是方法递归调用产生这种结果。 

我们需要使用参数 -Xss 去调整JVM栈的大小

二、 堆溢出(OutOfMemoryError:java heap space)
heap space表示堆空间，堆中主要存储的是对象。如果不断的new对象则会导致堆中的空间溢出

可以通过 -Xmx4096M 调整堆的总大小

三、 永久代溢出(OutOfMemoryError: PermGen space)
由于JDK8移除永久带，所以上述代码在JDK1.7的情况中会出现永久带溢出的现象。

## 1. 类的实例化顺序，比如父类静态数据，构造函数，字段，子类静态数据，构造函数，字段，他们的执行顺序
   
 > 先静态、先父后子。
   先静态：父静态 > 子静态
   优先级：父类 > 子类 静态代码块 > 非静态代码块 > 构造函数
   一个类的实例化过程：
   1，父类中的static代码块，当前类的static
   2，顺序执行父类的普通代码块
   3，父类的构造函数
   4，子类普通代码块
   5，子类（当前类）的构造函数，按顺序执行。
   6，子类方法的执行
   
## Java 8的内存分代改进
   从永久代到元空间，在小范围自动扩展永久代避免溢出
## jvm中一次完整的GC流程（从ygc到fgc）是怎样的，重点讲讲对象如何晋升到老年代等
   对象优先在新生代区中分配，若没有足够空间，Minor GC；
   
   大对象（需要大量连续内存空间）直接进入老年态；长期存活的对象进入老年态。如果对象在新生代出生并经过第一次MGC后仍然存活，年龄+1，若年龄超过一定限制（15），则被晋升到老年代。
## JVM常用参数
-X ：非标准选项

-XX：非稳定选项

在选项名前用 “+” 或 “-” 表示开启或关闭特定的选项，例：

    -XX:+UseCompressedOops：表示开启 压缩指针

    -XX:-UseCompressedOops：表示关闭 压缩指针

- 1.堆分配参数
-Xmn10M：设置新生代区域大小为10M

-XX:NewSize=2M：设置新生代初始大小为2M

-XX:MaxNewSize=2M：设置新生代最大值为2M

- - ##（如果以上三个同时设置了，谁在后面谁生效。生产环境使用-Xmn即可，避免抖动）

-Xms128M：设置java程序启动时堆内存128M（默认为物理内存1/64,且小于1G）

-Xmx256M：设置最大堆内存256M，超出后会出现 OutOfMemoryError（默认为物理内存1/64,且小于1G）

- - ##（生产环境 -Xms 与 -Xmx 最好一样，避免抖动）
-Xss1M：设置线程栈的大小 1M（默认1M）

- - ##  -XX:ThreadStackSize，-Xss 设置在后面，以-Xss为准；  -XX:ThreadStackSize设置在后面，主线程以 -Xss为准，其他线程以  -XX:ThreadStackSize为准

-XX:MinHeapFreeRatio=40：设置堆空间最小空闲比例（默认40）（当-Xmx与-Xms相等时，该配置无效）

-XX:MaxHeapFreeRatio=70：设置堆空间最大空闲比例（默认70）（当-Xmx与-Xms相等时，该配置无效）

-XX:NewRatio=2：设置年轻代与年老代的比例为2:1

-XX:SurvivorRatio=8：设置年轻代中eden区与survivor区的比例为8：1

-XX:MetaspaceSize=64M：设置元数据空间初始大小（取代-XX:PermSize）

-XX:MaxMetaspaceSize=128M：设置元数据空间最大值（取代之前-XX:MaxPermSize）

-XX:TargetSurvivorRatio=50：设置survivor区使用率。当survivor区达到50%时，将对象送入老年代

-XX:+UseTLAB：在年轻代空间中使用本地线程分配缓冲区(TLAB)，默认开启

-XX:TLABSize=512k：设置TLAB大小为512k

-XX:+UseCompressedOops：使用压缩指针，默认开启

-XX:MaxTenuringThreshold=15：对象进入老年代的年龄（Parallel是15，CMS是6）

2.垃圾回收器相关
-XX:MaxGCPauseMillis：设置最大垃圾收集停顿时间（收集器工作时会调整其他参数大小，尽可能将停顿控制在指定时间内）

-XX:+UseAdaptiveSizePolicy：打开自适应GC策略（该摸式下，各项参数都会被自动调整）

 

-XX:+UseSerialGC：在年轻代和年老代使用串行回收器

 
-XX:+UseParallelGC：使用并行垃圾回收收集器，默认会同时启用 -XX:+UseParallelOldGC（默认使用该回收器）

-XX:+UseParallelOldGC：开启老年代使用并行垃圾收集器，默认会同时启用 -XX:+UseParallelGC

-XX:ParallelGCThreads=4：设置用于垃圾回收的线程数为4（默认与CPU数量相同）

 
-XX:+UseConcMarkSweepGC：使用CMS收集器（年老代）

-XX:CMSInitiatingOccupancyFraction=80：设置CMS收集器在年老代空间被使用多少后触发

-XX:+CMSClassUnloadingEnabled：允许对类元数据进行回收

-XX:+UseCMSInitiatingOccupancyOnly：只在达到阈值的时候，才进行CMS回收

 
-XX:+UseG1GC：使用G1回收器

-XX:G1HeapRegionSize=16m：使用G1收集器时设置每个Region的大小（范围1M - 32M）

-XX:MaxGCPauseMillis=500 ：设置最大暂停时间（毫秒）

 
-XX:+DisableExplicitGC：禁止显示GC的调用（即禁止开发者的 System.gc();）

 

2.GC日志
-XX:+PrintGCDetails：打印GC信息

-XX:+PrintGCTimeStamps ：打印每次GC的时间戳（现在距离启动的时间长度）

-XX:+PrintGCDateStamps ：打印GC日期

-XX:+PrintHeapAtGC：每次GC时，打印堆信息

-Xloggc:/usr/local/tomcat/logs/gc.$$.log ：GC日志存放的位置


3.堆快照
-XX:+HeapDumpOnOutOfMemoryError：出现内存溢出时存储堆信息，配合 -XX:HeapDumpPath 使用

-XX:HeapDumpPath=/usr/local/tomcat/logs/oom.%t.log：堆快照存储位置

-XX:+UseLargePages：使用大页  

-XX:LargePageSizeInBytes=4m：指定大页的大小（必须为2的幂）

滚动日志记录
-XX:+UseGCLogFileRotation  ： 开启滚动日志记录
-XX:NumberOfGCLogFiles=5 ：滚动数量，命名为filename.0, filename.1 .....  filename.n-1,  然后再从filename.0 开始，并覆盖已经存在的文件
-XX:GCLogFileSize=8k  :  每个文件大小，当达到该指定大小时，会写入下一个文件
-Xloggc:/gc/log   ：日志文件位置
 