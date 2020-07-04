# Linux相关
## select poll epoll区别
IO多路复用是linux中IO模式的一种，与多进程和多线程相比，I/O多路复用技术最大的优势是系统开销小，系统不必创建进程/线程，也不必维护这些进程/线程，减小了不必要的上下文切换，降低了系统的开销。
I/O多路复用就是通过一种机制，一个进程可以监听多个描述符，一旦某个描述符就绪(一般就是读就绪或者写就绪)，能够通知程序进行相应的读写操作。但select、poll和epoll本质上都是同步I/O,他们在读写时间就绪后自己负责读写，这时候，读写过程是阻塞的

- 1. select
select由用户进程调用
select函数监控的文件描述符分为3类，分别是可读、可写或者异常，调用后select函数会阻塞用户进程，直到有文件描述符就绪，用户进程再将数据从kernel拷贝到用户进程
select目前在所有的操作系统上都有支持，linux默认的文件描述符数量为1024.
select的缺点：

单个进程所打开的FD数量是有限制的
对socket进行线性扫描，采用轮询的方法，效率较低。如果能给套接字注册某个回调函数，当他们活跃时，自动完成相关操作，那就避免了轮询。
需要维护一个用来存放大量fd的数据结构，这样会使得用户空间和内核空间传递该结构时，复制的开销过大。
- 2. poll
poll和select本质上没有区别，他没有最大连接数的限制，因为它的fd使用链表来存储的。

- 3. epoll
epoll在linux内核中申请一个简单的文件系统。把原先的select/poll调用分成了三个部分：

调用epoll_create()建立一个epoll对象（在文件系统中为这个对象分配资源）
调用epoll_ctl向epoll对象中添加这100万个连接的套接字
调用epoll_wait收集发生事件的连接
epoll没有采取轮询的方式而是采取回调的方式，当相应事件发生时通知程序进行IO操作

ET 边缘触发模式
在此模式下，当描述符由未就绪变为就绪时，内核通过epoll告知。然后它会假设用户知道文件描述符已经就绪，并且不会再为那个文件描述符发送更多的就绪通知，直到某些操作导致那个文件描述符不再为就绪状态了。（高速模式）

LT 水平触发
缺省工作方式，支持blocksocket和no_blocksocket。在LT模式下内核会告知一个文件描述符是否就绪了，然后可以对这个就绪的fd进行IO操作。如果不作任何操作，内核还是会继续通知！若数据没有读完，内核也会继续通知，直至设备数据为空为止！
I/O多路复用总结：
当需要处理多个客户端接入请求时，可以利用多线程或者I/O多路复用技术进行处理。I/O多路复用将多个I/O的阻塞复用到一个select阻塞上，使得系统在单线程情况下可处理多个客户端请求。同步非阻塞模型，epoll也存在阻塞，即用户进程从内核态拷贝数据。

异步IO
linux下，用户进程进行aio_read系统调用后，无论内核数据是否准备好，都会直接返回给用户进程，然后用户态进程可以去做别的事情。等到socket数据准备好了，内核直接复制数据给进程，然后内核向进程发送通知。IO两个阶段，进程都是非阻塞的，拷贝工作交由了内核进程。

## Linux 系统下你关注过哪些内核参数，说说你知道的。
    
   - Tcp/ip io cpu memory 
   net.ipv4.tcp_syncookies = 1  #启用syncookies 
   
   net.ipv4.tcp_max_syn_backlog = 8192 #SYN队列长度 
   
   net.ipv4.tcp_synack_retries=2 #SYN ACK重试次数 
   
   net.ipv4.tcp_fin_timeout = 30 #主动关闭方FIN-WAIT-2超时时间
    
   net.ipv4.tcp_keepalive_time = 1200 #TCP发送keepalive消息的频度 
   
   net.ipv4.tcp_tw_reuse = 1 #开启TIME-WAIT重用 
   
   net.ipv4.tcp_tw_recycle = 1 #开启TIME-WAIT快速回收 
   
   net.ipv4.ip_local_port_range = 1024 65000 #向外连接的端口范围 
   
   net.ipv4.tcp_max_tw_buckets = 5000  #最大TIME-WAIT数量，超过立即清除 
  
   net.ipv4.tcp_syn_retries = 2 #SYN重试次数 
   
   echo “fs.file-max=65535” >> /etc/sysctl.conf  #将文件内容输出到/etc/sysctl.conf
   
   sysctl -p  #打印系统内核参数
 




























