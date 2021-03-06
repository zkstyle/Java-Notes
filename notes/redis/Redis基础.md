# Redis基础
## redis简介
1.基于内存的key-value数据库
2.基于c语言编写的，可以支持多种语言的api //set每秒11万次，取get 81000次
3.支持数据持久化
4.value可以是string，hash， list， set, sorted set

## 使用场景
1. 去最新n个数据的操作
2. 排行榜，取top n个数据 //最佳人气前10条
3. 精确的设置过期时间
4. 计数器
5. 实时系统， 反垃圾系统
6. pub， sub发布订阅构建实时消息系统
7. 构建消息队列
8. 缓存
![](../images/redis.png)

## 基础命令
- key
~~~
      keys * 获取所有的key
      select 0 选择第一个库
      move myString 1 将当前的数据库key移动到某个数据库,目标库有，则不能移动
      flush db      清除指定库
      randomkey     随机key
      type key      类型
      set key1 value1 设置key
      get key1    获取key
      mset key1 value1 key2 value2 key3 value3
      mget key1 key2 key3
      del key1   删除key
      exists key      判断是否存在key
      expire key 10   10过期
      pexpire key 1000 毫秒
      persist key     删除过期时间
~~~
- string
~~~
    set name cxx
    get name
    getrange name 0 -1        字符串分段
    getset name new_cxx       设置值，返回旧值
    mset key1 key2            批量设置
    mget key1 key2            批量获取
    setnx key value           不存在就插入（not exists）
    setex key time value      过期时间（expire）
    setrange key index value  从index开始替换value
    incr age        递增
    incrby age 10   递增
    decr age        递减
    decrby age 10   递减
    incrbyfloat key   增减浮点数
    append   key       追加
    strlen   key       长度
    getbit/setbit/bitcount/bitop  key offset  位操作
~~~
- hash
~~~
    hset myhash name cxx
    hget myhash name
    hmset myhash name cxx age 25 note "i am notes"
    hmget myhash name age note   
    hgetall myhash               获取所有的
    hexists myhash name          是否存在
    hsetnx myhash score 100      设置不存在的
    hincrby myhash id 1          递增
    hdel myhash name             删除
    hkeys myhash                 只取key
    hvals myhash                 只取value
    hlen myhash                  长度    
~~~
- list
~~~
    lpush mylist a b c  左插入
    rpush mylist x y z  右插入
    lrange mylist 0 -1  数据集合
    lpop mylist  弹出元素
    rpop mylist  弹出元素
    llen mylist  长度
    lrem mylist count value  删除
    lindex mylist 2          指定索引的值
    lset mylist 2 n          索引设值
    ltrim mylist 0 4         删除key
    linsert mylist before a  插入
    linsert mylist after a   插入
    rpop/lpush list list2     转移列表的数据
~~~
- set
~~~
    sadd myset redis 
    smembers myset       数据集合
    srem myset set1         删除
    sismember myset set1 判断元素是否在集合中
    scard key_name       个数
    sdiff | sinter | sunion 操作：集合间运算：差集 | 交集 | 并集
    srandmember          随机获取集合中的元素
    spop                 从集合中弹出一个元素
~~~
- zset
~~~
    zadd zset 1 one
    zadd zset 2 two
    zadd zset 3 three
    zincrby zset 1 one              增长分数
    zscore zset two                 获取分数
    zrange zset 0 -1 withscores     范围值
    zrangebyscore zset 10 25 withscores 指定范围的值
    zrangebyscore zset 10 25 withscores limit 1 2 分页
    Zrevrangebyscore zset 10 25 withscores  指定范围的值
    zcard zset  元素数量
    Zcount zset 获得指定分数范围内的元素个数
    Zrem zset one two        删除一个或多个元素
    Zremrangebyrank zset 0 1  按照排名范围删除元素
    Zremrangebyscore zset 0 1 按照分数范围删除元素
    Zrank zset 0 -1    分数最小的元素排名为0
    Zrevrank zset 0 -1  分数最大的元素排名为0
    Zinterstore
    zunionstore rank:last_week 7 rank:20150323 rank:20150324 rank:20150325  weights 1 1 1 1 1 1 1
~~~
- 排序  
~~~
    sort mylist  排序
    sort mylist alpha desc limit 0 2 字母排序
    sort list by it:* desc           by命令
    sort list by it:* desc get it:*  get参数
    sort list by it:* desc get it:* store sorc:result  sort命令之store参数：表示把sort查询的结果集保存起来
~~~
- 订阅与发布  
~~~
    订阅频道：subscribe chat1
    发布消息：publish chat1 "hell0 ni hao"
    查看频道：pubsub channels
    查看某个频道的订阅者数量: pubsub numsub chat1
    退订指定频道： unsubscrible chat1   , punsubscribe java.*
    订阅一组频道： psubscribe java.*                
~~~
- 服务器管理
~~~
    dump.rdb
    appendonly.aof
    //BgRewriteAof 异步执行一个aop(appendOnly file)文件重写
    会创建当前一个AOF文件体积的优化版本
    
    //BgSave 后台异步保存数据到磁盘，会在当前目录下创建文件dump.rdb
    //save同步保存数据到磁盘，会阻塞主进程，别的客户端无法连接
    
    //client kill 关闭客户端连接
    //client list 列出所有的客户端
    
    //给客户端设置一个名称
      client setname myclient1
      client getname
      
     config get port
     //configRewrite 对redis的配置文件进行改写
     
     rdb
     save 900 1
     save 300 10
     save 60 10000
     
     aop备份处理
     appendonly yes 开启持久化
     appendfsync everysec 每秒备份一次
     
     命令：
     bgsave异步保存数据到磁盘（快照保存）
     lastsave返回上次成功保存到磁盘的unix的时间戳
     shutdown同步保存到服务器并关闭redis服务器
     bgrewriteaof文件压缩处理（命令）
~~~
 

## Redis的数据淘汰机制
 1. volatile-lru  从已设置过期时间的数据集中挑选最近最少使用的数据淘汰
 2. volatile-ttl  从已设置过期时间的数据集中挑选将要过期的数据淘汰
 3. volatile-random 从已设置过期时间的数据集中任意选择数据淘汰
 4. allkeys-lru 从所有数据集中挑选最近最少使用的数据淘汰
 5. allkeys-random 从所有数据集中任意选择数据进行淘汰
 6. noeviction 禁止驱逐数据
 
 - 定期删除
 
 redis 会将每个设置了过期时间的 key 放入到一个独立的字典中，以后会定期遍历这个字典来删除到期的 key。
 
 定期删除策略
 
 Redis 默认会每秒进行十次过期扫描（100ms一次），过期扫描不会遍历过期字典中所有的 key，而是采用了一种简单的贪心策略。
 
 从过期字典中随机 20 个 key；
 
 删除这 20 个 key 中已经过期的 key；
 
 如果过期的 key 比率超过 1/4，那就重复步骤 1；
 
 - 惰性删除
 
 除了定期遍历之外，它还会使用惰性策略来删除过期的 key，所谓惰性策略就是在客户端访问这个 key 的时候，redis 对 key 的过期时间进行检查，如果过期了就立即删除，不会给你返回任何东西。
 
 - 定期删除是集中处理，惰性删除是零散处理。 
 
## 如果有大量的key需要设置同一时间过期，一般需要注意什么？
如果大量的key过期时间设置的过于集中，到过期的那个时间点，redis可能会出现短暂的卡顿现象。严重的话会出现缓存雪崩，我们一般需要在时间上加一个随机值，使得过期时间分散一些。

电商首页经常会使用定时任务刷新缓存，可能大量的数据失效时间都十分集中，如果失效时间一样，又刚好在失效的时间点大量用户涌入，就有可能造成缓存雪崩
　
## 那你使用过Redis分布式锁么，它是什么回事？
先拿setnx来争抢锁，抢到之后，再用expire给锁加一个过期时间防止锁忘记了释放。

## 如果在setnx之后执行expire之前进程意外crash或者要重启维护了，那会怎么样？
set指令有非常复杂的参数，可以同时把setnx和expire合成一条指令来用的！

## 假如Redis里面有1亿个key，其中有10w个key是以某个固定的已知的前缀开头的，如何将它们全部找出来？
使用keys指令可以扫出指定模式的key列表。

如果这个redis正在给线上的业务提供服务，那使用keys指令会有什么问题？
这个时候你要回答redis关键的一个特性：redis的单线程的。keys指令会导致线程阻塞一段时间，线上服务会停顿，直到指令执行完毕，服务才能恢复。这个时候可以使用scan指令，scan指令可以无阻塞的提取出指定模式的key列表，但是会有一定的重复概率，在客户端做一次去重就可以了，但是整体所花费的时间会比直接用keys指令长。

不过，增量式迭代命令也不是没有缺点的： 举个例子， 使用 SMEMBERS 命令可以返回集合键当前包含的所有元素， 但是对于 SCAN 这类增量式迭代命令来说， 因为在对键进行增量式迭代的过程中， 键可能会被修改， 所以增量式迭代命令只能对被返回的元素提供有限的保证 。

## 使用过Redis做异步队列么，你是怎么用的？
一般使用list结构作为队列，rpush生产消息，lpop消费消息。当lpop没有消息的时候，要适当sleep一会再重试。

如果对方追问可不可以不用sleep呢？
list还有个指令叫blpop，在没有消息的时候，它会阻塞住直到消息到来。

## 如果对方接着追问能不能生产一次消费多次呢？
使用pub/sub主题订阅者模式，可以实现 1:N 的消息队列。

## 如果对方继续追问 pub/sub有什么缺点？
在消费者下线的情况下，生产的消息会丢失，得使用专业的消息队列如RocketMQ等。

## Redis如何实现延时队列？
使用sortedset，拿时间戳作为score，消息内容作为key调用zadd来生产消息，消费者用zrangebyscore指令获取N秒之前的数据轮询进行处理。

## Redis是怎么持久化的？服务主从数据怎么交互的？
RDB做镜像全量持久化，AOF做增量持久化。因为RDB会耗费较长时间，不够实时，在停机的时候会导致大量丢失数据，所以需要AOF来配合使用。在redis实例重启时，会使用RDB持久化文件重新构建内存，再使用AOF重放近期的操作指令来实现完整恢复重启之前的状态。

这里很好理解，把RDB理解为一整个表全量的数据，AOF理解为每次操作的日志就好了，服务器重启的时候先把表的数据全部搞进去，但是他可能不完整，你再回放一下日志，数据不就完整了嘛。不过Redis本身的机制是 AOF持久化开启且存在AOF文件时，优先加载AOF文件；AOF关闭或者AOF文件不存在时，加载RDB文件；加载AOF/RDB文件城后，Redis启动成功； AOF/RDB文件存在错误时，Redis启动失败并打印错误信息

## 对方追问那如果突然机器掉电会怎样？
取决于AOF日志sync属性的配置，如果不要求性能，在每条写指令时都sync一下磁盘，就不会丢失数据。但是在高性能的要求下每次都sync是不现实的，一般都使用定时sync，比如1s1次，这个时候最多就会丢失1s的数据。

## 对方追问RDB的原理是什么？
你给出两个词汇就可以了，fork和cow。fork是指redis通过创建子进程来进行RDB操作，cow指的是copy on write，子进程创建后，父子进程共享数据段，父进程继续提供读写服务，写脏的页面数据会逐渐和子进程分离开来。

**注：回答这个问题的时候，如果你还能说出AOF和RDB的优缺点**。

tip：两种机制全部开启的时候，Redis在重启的时候会默认使用AOF去重新构建数据，因为AOF的数据是比RDB更完整的。

+ RDB优点：
他会生成多个数据文件，每个数据文件分别都代表了某一时刻Redis里面的数据，这种方式，有没有觉得很适合做冷备，完整的数据运维设置定时任务，定时同步到远端的服务器，比如阿里的云服务，这样一旦线上挂了，你想恢复多少分钟之前的数据，就去远端拷贝一份之前的数据就好了。
RDB对Redis的性能影响非常小，是因为在同步数据的时候他只是fork了一个子进程去做持久化的，而且他在数据恢复的时候速度比AOF来的快。
+ RDB缺点：
RDB都是快照文件，都是默认五分钟甚至更久的时间才会生成一次，这意味着你这次同步到下次同步这中间五分钟的数据都很可能全部丢失掉。AOF则最多丢一秒的数据，数据完整性上高下立判。
还有就是RDB在生成数据快照的时候，如果文件很大，客户端可能会暂停几毫秒甚至几秒，你公司在做秒杀的时候他刚好在这个时候fork了一个子进程去生成一个大快照，哦豁，出大问题。
我们再来说说AOF
+ AOF优点：
上面提到了，RDB五分钟一次生成快照，但是AOF是一秒一次去通过一个后台的线程fsync操作，那最多丢这一秒的数据。
AOF在对日志文件进行操作的时候是以append-only的方式去写的，他只是追加的方式写数据，自然就少了很多磁盘寻址的开销了，写入性能惊人，文件也不容易破损。
AOF的日志是通过一个叫非常可读的方式记录的，这样的特性就适合做灾难性数据误删除的紧急恢复了，比如公司的实习生通过flushall清空了所有的数据，只要这个时候后台重写还没发生，你马上拷贝一份AOF日志文件，把最后一条flushall命令删了就完事了。
tip：我说的命令你们别真去线上系统操作啊，想试去自己买的服务器上装个Redis试，别到时候来说，敖丙真是个渣男，害我把服务器搞崩了，Redis官网上的命令都去看看，不要乱试！！！
+ AOF缺点：
一样的数据，AOF文件比RDB还要大。
AOF开启后，Redis支持写的QPS会比RDB支持写的要低，他不是每秒都要去异步刷新一次日志嘛fsync，当然即使这样性能还是很高，我记得ElasticSearch也是这样的，异步刷新缓存区的数据去持久化，为啥这么做呢，不直接来一条怼一条呢，那我会告诉你这样性能可能低到没办法用的，大家可以思考下为啥哟。

## 哨兵组件的主要功能：

集群监控：负责监控 Redis master 和 slave 进程是否正常工作。
消息通知：如果某个 Redis 实例有故障，那么哨兵负责发送消息作为报警通知给管理员。
故障转移：如果 master node 挂掉了，会自动转移到 slave node 上。
配置中心：如果故障转移发生了，通知 client 客户端新的 master 地址。 

## Pipeline有什么好处，为什么要用pipeline？
可以将多次IO往返的时间缩减为一次，前提是pipeline执行的指令之间没有因果相关性。使用redis-benchmark进行压测的时候可以发现影响redis的QPS峰值的一个重要因素是pipeline批次指令的数目。

## Redis的同步机制了解么？
Redis可以使用主从同步，从从同步。第一次同步时，主节点做一次bgsave，并同时将后续修改操作记录到内存buffer，待完成后将RDB文件全量同步到复制节点，复制节点接受完成后将RDB镜像加载到内存。加载完成后，再通知主节点将期间修改的操作记录同步到复制节点进行重放就完成了同步过程。后续的增量数据通过AOF日志同步即可，有点类似数据库的binlog。

## 是否使用过Redis集群，集群的高可用怎么保证，集群的原理是什么？
Redis Sentinal着眼于高可用，在master宕机时会自动将slave提升为master，继续提供服务。

Redis Cluster着眼于扩展性，在单个redis内存不足时，使用Cluster进行分片存储。