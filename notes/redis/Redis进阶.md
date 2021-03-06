###讲一讲为什么Redis这么快？

首先，采用了多路复用io阻塞机制
阻塞IO
如果接收缓冲区的数据为空的时候，那么receive调用scoket的read方法就会处于阻塞状态，直到有数据过来。
同样，对于写来说，如果发送缓冲区满了，那么调用scoket的write方法就会处于阻塞状态，直到报文送到网络上。
这就是阻塞IO！！

非阻塞IO
阻塞IO会一直等待，所以非阻塞IO是用来解决IO线程与scoket之间的解耦问题（引入事件机制），如果scoket发送缓存区可写的话会通知IO线程进行write，同样如果scoket的接受缓冲区可读的话会通知IO线程进行read。
这就是非阻塞IO！！

- 完全基于内存，绝大部分请求是纯粹的内存操作，非常快速。它的，数据存在内存中，类似于HashMap，HashMap的优势就是查找和操作的时间复杂度都是O(1)；
- 数据结构简单，对数据操作也简单，Redis中的数据结构是专门进行设计的；
- 采用单线程，避免了不必要的上下文切换和竞争条件，也不存在多进程或者多线程导致的切换而消耗 CPU，不用去考虑各种锁的问题，不存在加锁释放锁操作，没有因为可能出现死锁而导致的性能消耗；
- 使用多路I/O复用模型，非阻塞IO；
- 使用底层模型不同，它们之间底层实现方式以及与客户端之间通信的应用协议不一样，Redis直接自己构建了VM 机制 ，因为一般的系统调用系统函数的话，会浪费一定的时间去移动和请求；

### Redis为什么是单线程的？

Redis官方解释因为Redis的瓶颈不是cpu的运行速度，而往往是网络带宽和机器的内存大小。单线程切换开销小，容易实现既然单线程容易实现，而且CPU不会成为瓶颈，那就顺理成章地采用单线程的方案了。
如果万一CPU成为你的Redis瓶颈了，或者，你就是不想让服务器其他核闲置，那怎么办？

多起几个Redis进程就好了。Redis是key-value数据库，又不是关系数据库，数据之间没有约束。只要客户端分清哪些key放在哪个Redis进程上就可以了。redis-cluster可以帮你做的更好。
单线程可以处理高并发请求吗？

当然可以了，Redis都实现了。有一点概念需要澄清，并发并不是并行。
（相关概念：并发性I/O流，意味着能够让一个计算单元来处理来自多个客户端的流请求。并行性，意味着服务器能够同时执行几个事情，具有多个计算单元）
我们使用单线程的方式是无法发挥多核CPU 性能，有什么办法发挥多核CPU的性能嘛？

我们可以通过在单机开多个Redis 实例来完善！
警告：这里我们一直在强调的单线程，只是在处理我们的网络请求的时候只有一个线程来处理，一个正式的Redis Server运行的时候肯定是不止一个线程的，这里需要大家明确的注意一下！
例如Redis进行持久化的时候会以子进程或者子线程的方式执行（具体是子线程还是子进程待读者深入研究）
###简述一下Redis值的五种类型

String 整数，浮点数或者字符串
Set 集合
Zset 有序集合
Hash 散列表
List 列表

###有序集合的实现方式是哪种数据结构？

[跳跃表](/redis/跳跃表.md)

###请列举几个用得到Redis的常用使用场景?
~~~
缓存，毫无疑问这是Redis当今最为人熟知的使用场景。再提升服务器性能方面非常有效；

排行榜，在使用传统的关系型数据库（mysql oracle 等）来做这个事儿，非常的麻烦，而利用Redis的SortSet(有序集合)数据结构能够简单的搞定；

计算器/限速器，利用Redis中原子性的自增操作，我们可以统计类似用户点赞数、用户访问数等，这类操作如果用MySQL，频繁的读写会带来相当大的压力；限速器比较典型的使用场景是限制某个用户访问某个API的频率，常用的有抢购时，防止用户疯狂点击带来不必要的压力；

好友关系，利用集合的一些命令，比如求交集、并集、差集等。可以方便搞定一些共同好友、共同爱好之类的功能；

简单消息队列，除了Redis自身的发布/订阅模式，我们也可以利用List来实现一个队列机制，比如：到货通知、邮件发送之类的需求，不需要高可靠，但是会带来非常大的DB压力，完全可以用List来完成异步解耦；

Session共享，以PHP为例，默认Session是保存在服务器的文件中，如果是集群服务，同一个用户过来可能落在不同机器上，这就会导致用户频繁登陆；采用Redis保存Session后，无论用户落在那台机器上都能够获取到对应的Session信息。
~~~


Redis怎样防止异常数据不丢失？

RDB 持久化
将某个时间点的所有数据都存放到硬盘上。
可以将快照复制到其它服务器从而创建具有相同数据的服务器副本。
如果系统发生故障，将会丢失最后一次创建快照之后的数据。
如果数据量很大，保存快照的时间会很长。
AOF 持久化
将写命令添加到 AOF 文件（Append Only File）的末尾。
使用 AOF 持久化需要设置同步选项，从而确保写命令同步到磁盘文件上的时机。这是因为对文件进行写入并不会马上将内容同步到磁盘上，而是先存储到缓冲区，然后由操作系统决定什么时候同步到磁盘。有以下同步选项：
选项同步频率always每个写命令都同步everysec每秒同步一次no让操作系统来决定何时同步
always 选项会严重减低服务器的性能；
everysec 选项比较合适，可以保证系统崩溃时只会丢失一秒左右的数据，并且 Redis 每秒执行一次同步对服务器性能几乎没有任何影响；
no 选项并不能给服务器性能带来多大的提升，而且也会增加系统崩溃时数据丢失的数量
随着服务器写请求的增多，AOF 文件会越来越大。Redis 提供了一种将 AOF 重写的特性，能够去除 AOF 文件中的冗余写命令。

###　缓存雪崩以及缓存击穿、缓存穿透

> 缓存穿透：就是客户持续向服务器发起对不存在服务器中数据的请求。客户先在Redis中查询，查询不到后去数据库中查询。
缓存击穿：就是一个很热门的数据，突然失效，大量请求到服务器数据库中
缓存雪崩：就是大量数据同一时间失效。


缓存穿透：
1. 接口层增加校验，对传参进行校验，比如说我们的id是从1开始的，那么id<=0的直接拦截；
2. 缓存中取不到的数据，在数据库中也没有取到，这时可以将key-value对写为key-null，这样可以防止攻击用户反复用同一个id暴力攻击
3. 布隆过滤器也可以用来解决此类问题,布隆过滤器查询数据不存在一定不存在
缓存击穿：
最好的办法就是设置热点数据永不过期，拿到刚才的比方里，那就是你买腾讯一个永久会员
缓存雪崩：
1.缓存数据的过期时间设置随机，防止同一时间大量数据过期现象发生。
2.如果缓存数据库是分布式部署，将热点数据均匀分布在不同得缓存数据库中。

##手写一个LRU算法
思想：使用LinkedHashMap，一个有序的HashMap。
~~~java
import java.util.LinkedHashMap;
import java.util.Map;
 
public class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int CACHE_SIZE;
 
    /**
     * 传递进来最多能缓存多少数据
     * @param cacheSize 缓存大小
     */
    public LRUCache(int cacheSize) {
        
        //true 表示让 linkedHashMap 按照访问顺序来进行排序，最近访问的放在头部，最老访问的的放在尾部
        super((int) Math.ceil(cacheSize / 0.75) + 1, 0.75f, true);
        CACHE_SIZE = cacheSize;
    }
 
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        //当map中的数据量大于制定的缓存个数的时候，就自动删除最老的数据
        return size() > CACHE_SIZE;
    }
}
~~~