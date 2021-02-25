## Explain调优

使用explain语法，对SQL进行解释，根据其结果进行调优：

MySQL 表关联的算法是 Nest Loop Join，是通过驱动表的结果集作为循环基础数据，然后一条一条地通过该结果集中的数据作为过滤条件到下一个表中查询数据，然后合并结果：

​     a.　EXPLAIN 结果中，第一行出现的表就是驱动表

​     b.　**对驱动表可以直接排序**，对非驱动表（的字段排序）需要对循环查询的合并结果（临时表）进行排序（Important!），即using temporary; 

​     c. 　[驱动表] 的定义为：1）指定了联接条件时，满足查询条件的**记录行数少的表为[驱动表]**；2）未指定联接条件时，**行数少的表为**[驱动表]（Important!）。

​     d.　**优化的目标**是尽可能减少JOIN中Nested Loop的循环次数，以此保证：**永远用小结果集驱动大结果集**（Important!）！：A JOIN B，A为驱动，A中每一行和B进行循环JOIN，看是否满足条件，所以当A为小结果集时，越快。

​     e.NestedLoopJoin实际上就是通过驱动表的结果集作为循环基础数据，然后一条一条的通过该结果集中的数据作为过滤条件到下一个表中查询数据，然后合并结果。如果还有第三个参与Join，**则再通过前两个表的Join结果集作为循环基础数据**，再一次通过循环查询条件到第三个表中查询数据，如此往复

### 各个列的解释

**table**：显示这一行的数据是关于哪张表的

**type**：这是重要的列，显示连接使用了何种类型。从最好到最差的连接类型为const、eq_reg、ref、range、index和ALL

​     type显示的是访问类型，是较为重要的一个指标，结果值从好到坏依次是：**system > const > eq_ref > ref > fulltext > ref_or_null > index_merge > unique_subquery > index_subquery > range > index > ALL**   一般来说，得保证查询至少达到range级别，最好能达到ref。

|  ALL        |  全表扫描

|  index       |  索引全扫描

|  range       |  索引范围扫描，**常用语<,<=,>=,between等操作**

|  ref         |  使用非唯一索引扫描或唯一索引前缀扫描，返回单条记录，常出现在关联查询中，即哪些列或**常量被用于查找索引列**上的值

|  eq_ref      |  类似ref，区别在于使用的是唯一索引，使用**主键/唯一索引的关联**查询。对于每个索引键值，表中只有一条记录匹配，简单来说，就是多表连接中使用primary key或者 unique key作为关联条件。

|  const/system  |  单条记录，系统会把匹配行中的其他列作为常数处理，如**主键或唯一索引查询**

|  null         |  MySQL不访问任何表或索引，直接返回

由上至下，效率越来越高

**possible_keys**：显示可能应用在这张表中的索引。如果为空，没有可能的索引。可以为相关的域从WHERE语句中选择一个合适的语句

**key**： 实际使用的索引。如果为NULL，则没有使用索引。很少的情况下，MYSQL会选择优化不足的索引。这种情况下，可以在SELECT语句中使用USE INDEX（indexname）来强制使用一个索引或者用IGNORE INDEX（indexname）来强制MYSQL忽略索引

**key_len**：使用的索引的长度。在不损失精确性的情况下，长度越短越好

**ref**：显示索引的哪一列被使用了，如果可能的话，是一个常数

**rows**：MYSQL认为必须检查的用来返回请求数据的行数

**Extra**：关于MYSQL如何解析查询的额外信息。但坏的例子是Using temporary和Using filesort，意思MYSQL根本不能使用索引，结果是检索会很慢

### **extra**

 Distinct:一旦MYSQL找到了与行相联合匹配的行，就不再搜索了

 Not exists: MYSQL优化了LEFT JOIN，一旦它找到了匹配LEFT JOIN标准的行，就不再搜索了

 Range checked for each Record（index map:#）:没有找到理想的索引，因此对于从前面表中来的每一个行组合，MYSQL检查使用哪个索引，并用它来从表中返回行。这是使用索引的最慢的连接之一

Using filesort: 看到这个的时候，查询就需要优化了。MYSQL需要进行额外的步骤来发现如何对返回的行排序。它根据连接类型以及存储排序键值和匹配条件的全部行的行指针来排序全部行


> **优化方法**：
> 1、修改逻辑，不在mysql中使用order by而是在应用中自己进行排序。
> 2、使用mysql索引，将待排序的内容放到索引中，直接利用索引的排序。

 Using index: 列数据是从仅仅使用了索引中的信息而没有读取实际的行动的表返回的，这发生在对表的全部的请求列都是同一个索引的部分的时候

 Using temporary 看到这个的时候，查询需要优化了。这里，MYSQL需要创建一个临时表来存储结果，这通常发生在对不同的列集进行ORDER BY上，而不是GROUP BY上

 Where used 使用了WHERE从句来限制哪些行将与下一张表匹配或者是返回给用户。如果不想返回表中的全部行，并且连接类型ALL或index，这就会发生，或者是查询有问题不同连接类型的解释（按照效率高低的顺序排序）

 system 表只有一行：system表。这是const连接类型的特殊情况

 const:表中的一个记录的最大值能够匹配这个查询（索引可以是主键或惟一索引）。因为只有一行，这个值实际就是常数，因为MYSQL先读这个值然后把它当做常数来对待

 eq_ref:在连接中，MYSQL在查询时，从前面的表中，对每一个记录的联合都从表中读取一个记录，它在查询使用了索引为主键或惟一键的全部时使用

 ref:这个连接类型只有在查询使用了不是惟一或主键的键或者是这些类型的部分（比如，利用最左边前缀）时发生。对于之前的表的每一个行联合，全部记录都将从表中读出。这个类型严重依赖于根据索引匹配的记录多少—越少越好

 range:这个连接类型使用索引返回一个范围中的行，比如使用>或<查找东西时发生的情况

 index: 这个连接类型对前面的表中的每一个记录联合进行完全扫描（比ALL更好，因为索引一般小于表数据）

 ALL:这个连接类型对于前面的每一个记录联合进行完全扫描，这一般比较糟糕，应该尽量避免

 

亲测：

group by会导致Using temporary; Using filesort，将分组字段使用索引即可解决

order by会导致Using filesort，建立索引。数据量占大部分的情况下也会放弃使用索引。[官网优化](https://dev.mysql.com/doc/refman/5.6/en/order-by-optimization.html)

**需要注意的是**：由于 Using filesort是使用算法在 内存中进行排序，MySQL对于排序的记录的大小也是有做限制：max_length_for_sort_data，默认为1024。

**show variables like '%max_length_for_sort_data%';可查看大小**。

## 导致索引失效的原因

**1. 随着表的增长，where条件出来的结果集数据太多，大于数据总量的15%，使得索引失效（会导致CBO计算走索引花费大于走全表）**

在查询条件上没有使用引导列

查询的数量是大表的大部分，应该是30％以上

查询小表,或者返回值大概在10%以上

隐式转换导致索引失效.这一点应当引起重视.也是开发中经常会犯的错误，varchar字段结果给出

由于表的字段值超过定义的长度 默认1024

like "%_" 百分号在前 左模糊查询

单独引用复合索引里非第一位置的索引列

字符型字段为数字时在where条件里不添加引号

对索引列进行运算.需要建立函数索引

not in ,not exist,or

当变量采用的是time变量，而表的字段采用的是date变量时.或相反情况

B-tree索引 is null不会走,is not null会走,位图索引 is null,is not null 都会走 

索引失效的案例：
1. 隐式转换导致索引失效，如字符串不加单引号索引失效，因为这里有一个隐式的类型转换操作，更严重会导致行锁变表锁，降低SQL效率
2. 违反最佳左前缀法则　如果索引了多列，要遵守最左前缀法则，指的是查询从索引的最左前列开始，不跳过索引中间的列。
3. 在索引列上做任何操作(计算、函数、（自动or手动）类型转换)，会导致索引失效而转向全表扫描
4. 存储引擎不能使用索引中范围条件右边的列。（范围之后全失效）
若中间索引列用到了范围（>、<、like等），则后面的索引全失效
5. 尽量使用覆盖索引（只访问索引的查询(索引列和查询列一致)），减少select *
6. Mysql在使用不等于(!=、<>)或like的左模糊的时候无法试用索引会导致全表扫描
7. IS NULL和IS NOT NULL也无法使用索引
8. 少用or，用它来连接时索引会失效














##  mysql explain 及常见优化手段

在工作中如果遇到慢sql通常都可以用explain进行解析。

先列一下各个列名以及含义

| 列名              | 描述                                               |
| :---------------- | :------------------------------------------------- |
| id                | 在一个大的查询中每一个查询关键字都对应一个id       |
| select type       | select关键字对应的那个查询类型                     |
| table             | 表名                                               |
| partitions（*）   | 分配的分区信息                                     |
| **type**          | 针对单表的访问方法                                 |
| **possible_keys** | 可能用到的索引                                     |
| **key**           | 实际上使用的索引                                   |
| **key len**       | 实际用到的索引长度                                 |
| ref               | 当索引列等值查询时，与索引列进行等值匹配的对象信息 |
| rows              | 预估的需要读取的记录条数                           |
| filtered          | 某个表经过搜索条件过滤后剩余记录条数的百分比       |
| **extra**         | 一些额外的信息                                     |

## id

一个查询语句就会有一个对应的id，如果其内部包含子查询且没有被查询优化器优化掉的情况下就会出现不同的id……

## select type

1. primary 主查询 （出现子查询的语句时会区分子和主查询）

2. subquery （非相关子查询）

   非相关子查询得到的结果表会被物化，只需要执行一遍

3. dependent query（相关子查询）

   相关子查询可能会被执行多次嗷

4. union 联查时

5. union result 临时表

6. simple 简单查询

7. derived派生表

   出现在有子查询时，如果为该类型则代表该查询是以物化的方式执行的

8. materialized

   当子查询物化后与外层查询进行连接时的查询类型。

## type

1. system

   innodb中不存在，MyISAM、Memory引擎中有，代表精确的查询

2. const

   主键或者唯一二级索引时的常量查询

   例如 `where a=1`,a为主键

3. eq_ref

   代表连接查询时，被驱动表是通过逐渐或者唯一二级索引列等值匹配的方式进行访问的

4. ref

   非主键或者唯一索引时使用索引的查询

5. ref or null

   ref的情况+条件中出现null

6. index merge

   索引合并查询，同时使用了多个索引的情况。

7. unique subquery

   通常出现在相关子查询把in优化为exists而且子查询可以使用主键进行查找时

8. index subquery

   与unique类似，但访问的是普通的索引

9. range

   范围查询时出现

10. index

    查询辅助索引字段时出现，遍历辅助索引值。

11. all

    全表扫描

### key lenth

当前使用索引字段的长度

如果索引值可以为空，key length会多存储一个字节

如果为变长字段（例如varchar），需要2个字节的存储空间存储长度。

## ref

代表驱动查询的字段。

例如在相关子查询中，子查询的驱动字段应该为主查询中表的某个值。

### filtered

通过该索引查询到的数据满足 非索引条件的数据所占的百分比。

```sql
select * from table where index_case and non_index_case;
```

假设符合index_case 的值为100个（rows=100），但是符合non_index_case的值为20个，那么filtered就为20。

注：为估算值。

## extra

Extra列是用来说明一些额外信息的，我们可以通过这些额外信息来更准确的理解MySQL到底将如何执行给定的
查询语句。

1. no tables used

   当查询语句的没有FROM子句时将会提示该额外信息。

2. impossible where

   where子句永远为false

3. no matching min/max row

   查询列表中有min或者max聚集函数，但是并没有where子句中的搜索条件记录时会提示该额外信息

4. using index

   查询列表以及搜索条件中只包含属于某个索引的列，既索引覆盖

5. using index condition

   搜索条件中虽然出现了索引列，但是有部分条件无法使用索引，会根据能用索引的条件先搜索一遍再匹配无法使用索引的条件

6. using where

   全表扫描并且where中有针对该表的搜索条件

7. using join buffer（Block Nested Loop）

   在连接查询执行过程中，当被驱动表不能有效的利用索引加快访问速度时就分配一块join buffer内存块来加快查询速度。

8. using filesort

   多数情况下排序操作无法用到索引，只能在内存中（记录较少时）或者磁盘中进行排序，这种在情况统称为文件排序。

9. using temporary

   在诸多查询过程中，可能会借助临时表来完成一些查询功能，比如去重、排序之类的，比如我们在执行许多包含distinct、group by、union等子句的查询过程中，如果不能有效利用索引完成查询，mysql可能通过建立内部临时表来执行查询。

10. Start temporary, End temporary

    子查询可以优化成半连接，但通过了临时表进行去重

11. firstmatch（table_name）

    子查询时可以优化成半连接，但直接进行数据比较去重

## index hint

### use index

```
select * from table use index (index_name,index_name2) where case;
```

强制查询优化器在指定的索引中做选择。

### force index

```sql
select * from table force index (index_name) where case;
```

强制查询优化器使用该索引

### ignore index

```sql
select * from ignore index (index_name) where case;
```

强制忽略该索引。

## 小结

### 性能按照type排序

system > const > eq_ref > ref > ref_or_null > index_merge > unique_subquery > index_subquery > range >index > ALL

### 性能按照extra排序

1. Using index：用了覆盖索引
2. Using index condition：用了条件索引（索引下推）
3. Using where：从索引查出来数据后继续用where条件过滤
4. Using join buffer (Block Nested Loop)：join的时候利用了join buffer（优化策略：去除外连接、增大join buffer大小）
5. Using filesort：用了文件排序，排序的时候没有用到索引
6. Using temporary：用了临时表（优化策略：增加条件以减少结果集、增加索引，思路就是要么减少待排序的数量，要么提前排好序）
7. Start temporary, End temporary：子查询的时候，可以优化成半连接，但是使用的是通过临时表来去重
8. FirstMatch(tbl_name)：子查询的时候，可以优化成半连接，但是使用的是直接进行数据比较来去重

### 常见优化手段

1. SQL语句中IN包含的值不应过多，不能超过200个，200个以内查询优化器计算成本时比较精准，超过200个是估算的成本，另外建议能用between就不要用in，这样就可以使用range索引了。
2. SELECT语句务必指明字段名称：SELECT * 增加很多不必要的消耗（cpu、io、内存、网络带宽）；增加
   了使用覆盖索引的可能性；当表结构发生改变时，前断也需要更新。所以要求直接在select后面接上字段
   名。
3. 当只需要一条数据的时候，使用limit 1
4. 排序时注意是否能用到索引
5. 使用or时如果没有用到索引，可以改为union all 或者union
6. 如果in不能用到索引，可以改成exists看是否能用到索引
7. 使用合理的分页方式以提高分页的效率
8. 不建议使用%前缀模糊查询
9. 避免在where子句中对字段进行表达式操作
10. 避免隐式类型转换
11. 对于联合索引来说，要遵守最左前缀法则
12. 必要时可以使用force index来强制查询走某个索引
13. 对于联合索引来说，如果存在范围查询，比如between,>,<等条件时，会造成后面的索引字段失效。
14. 尽量使用inner join，避免left join，让查询优化器来自动选择小表作为驱动表
15. 必要时刻可以使用straight_join来指定驱动表，前提条件是本身是inner join