package handwrite;

import handwrite.node.LRUNode;

import java.util.HashMap;

/**
 * @BelongsProject: Java-Notes
 * @BelongsPackage: handwrite
 * @Author: elvis
 * @CreateTime: 2020-07-03 22:56
 * @Description:
 * LRU的简单实现
 */
public class LRUCache {

    /**
     * LRU（Least Recently Used）直译为“最近最少使用”。
     *
     * 其实LRU这个概念映射到现实生活中非常好理解，就好比说小明的衣柜中有很多衣服，假设他的衣服都只能放在这个柜子里，小明每过一阵子小明就会买新衣服，不久小明的衣柜就放满了衣服。这个小明想了个办法，按照衣服上次穿的时间排序，丢掉最长时间没有穿过那个。这就是LRU策略。
     *
     * 映射到计算机概念中，上述例子中小明的衣柜就是内存，而小明的衣服就是缓存数据。我们的内存是有限的。所以当缓存数据在内存越来越多，以至于无法存放即将到来的新缓存数据时，就必须扔掉最不常用的缓存数据。所以对于LRU的抽象总结如下：
     *
     * 缓存的容量是有限的 当缓存容量不足以存放需要缓存的新数据时，必须丢掉最不常用的缓存数据
     *
     * 一个最容易想到的办法是我们在给这个缓存数据加一个时间戳，每次get缓存时就更新时间戳，这样找到最久没有用的缓存数据问题就能够解决，但与之而来的会有两个新问题：
     *
     * 虽然使用时间戳可以找到最久没用的数据，但我们最少的代价也要将这些缓存数据遍历一遍，除非我们维持一个按照时间戳排好序的SortedList。
     * 添加时间戳的方式为我们的数据带来了麻烦，我们并不太好在缓存数据中添加时间戳的标识，这可能需要引入新的包含时间戳的包装对象。
     * 而且我们的需要只是找到最久没用使用的缓存数据，并不需要精确的时间。添加时间戳的方式显然没有利用这一特性，这就使得这个办法从逻辑上来讲可能不是最好的。
     *
     * 然而办法总是有的，我们可以维护一个链表，当数据每一次查询就将数据放到链表的head，当有新数据添加时也放到head上。这样链表的tail就是最久没用使用的缓存数据，每次容量不足的时候就可以删除tail，并将前一个元素设置为tail，显然这是一个双向链表结构
     * HashMap快速获取访问节点
     */
    private HashMap<String, LRUNode> map;
    private int capacity;
    private LRUNode head;
    private LRUNode tail;
    public void set(String key, Object value) {
        LRUNode node = map.get(key);
        if (node != null) {
            //node = map.get(key);
            node.value = value;
            remove(node, false);
        } else {
            node = new LRUNode(key, value);
            if (map.size() >= capacity) {
                // 每次容量不足时先删除最久未使用的元素
                remove(tail, true);
            }
            map.put(key, node);
        }
        // 将刚添加的元素设置为head
        setHead(node);
    }
    public Object get(String key) {
        LRUNode node = map.get(key);
        if (node != null) {
            // 将刚操作的元素放到head
            remove(node, false);
            setHead(node);
            return node.value;
        }
        return null;
    }
    private void setHead(LRUNode node) {
        // 先从链表中删除该元素
        if (head != null) {
            node.next = head;
            head.prev = node;
        }
        head = node;
        if (tail == null) {
            tail = node;
        }
    }
    // 从链表中删除此Node，此时要注意该Node是head或者是tail的情形
    private void remove(LRUNode node, boolean flag) {
        if (node.prev != null) {
            node.prev.next = node.next;
        } else {
            head = node.next;
        }
        if (node.next != null) {
            node.next.prev = node.prev;
        } else {
            tail = node.prev;
        }
        node.next = null;
        node.prev = null;
        if (flag) {
            map.remove(node.key);
        }
    }

    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.map = new HashMap<>();
    }

    public static void main(String[] args)  {
        LRUCache lruCache=new LRUCache(4);
        lruCache.set("1",new LRUNode("1","2"));
        lruCache.set("2",new LRUNode("2","2"));
        lruCache.set("3",new LRUNode("3","2"));
        lruCache.set("4",new LRUNode("4","2"));
        lruCache.set("5",new LRUNode("5","2"));
        lruCache.get("3");
        lruCache.get("4");
        System.out.println(lruCache);
    }
}
