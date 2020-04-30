# 深入理解happens-before规则

### 为什么会有happens-before 规则？

因为jvm会对代码进行编译优化，指令会出现重排序的情况，为了避免编译优化对并发编程安全性的影响，需要happens-before规则定义一些禁止编译优化的场景，保证并发编程的正确性。

```java
public class VolatileExample {
    int x = 0 ;
    volatile boolean v = false;
    public void writer(){
        x = 42;
        v = true;
    }

    public void reader(){
        if (v == true){
            // 这里x会是多少呢
        }
    }
}
```

> 抛出问题：假设有两个线程A和B，A执行了writer方法，B执行reader方法，那么B线程中独到的变量x的值会是多少呢？

jdk1.5之前，线程B读到的变量x的值可能是0，也可能是42，jdk1.5之后，变量x的值就是42了。原因是jdk1.5中，对volatile的语义进行了增强。来看一下happens-before规则在这段代码中的体现。

### 1. 规则一：程序的顺序性规则

> 一个线程中，按照程序的顺序，前面的操作happens-before后续的任何操作。

对于这一点，可能会有疑问。顺序性是指，我们可以按照顺序推演程序的执行结果，但是编译器未必一定会按照这个顺序编译，但是编译器保证结果一定==顺序推演的结果。

### 2. 规则二：volatile规则

> 对一个volatile变量的写操作，happens-before后续对这个变量的读操作。

### 3. 规则三：传递性规则

> 如果A happens-before B，B happens-before C，那么A happens-before C。

jdk1.5的增强就体现在这里。回到上面例子中，线程A中，根据规则一，对变量x的写操作是happens-before对变量v的写操作的，根据规则二，对变量v的写操作是happens-before对变量v的读操作的，最后根据规则三，也就是说，线程A对变量x的写操作，一定happens-before线程B对v的读操作，那么线程B在注释处读到的变量x的值，一定是42.

### 4.规则四：管程中的锁规则

> 对一个锁的解锁操作，happens-before后续对这个锁的加锁操作。

这一点不难理解。

### 5.规则五：线程start()规则

> 主线程A启动线程B，线程B中可以看到主线程启动B之前的操作。也就是start() happens before 线程B中的操作。

### 6.规则六：线程join()规则

> 主线程A等待子线程B完成，当子线程B执行完毕后，主线程A可以看到线程B的所有操作。也就是说，子线程B中的任意操作，happens-before join()的返回。