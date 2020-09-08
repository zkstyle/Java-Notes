package handwrite;

import handwrite.node.LRUNode;

import java.util.HashMap;
import java.util.Map;

/**
 * @BelongsProject: Java-Notes
 * @BelongsPackage: handwrite
 * @Author: elvis
 * @CreateTime: 2020-09-08 21:14
 * @Description:
 */
public class LRUNode2 {


    private Map<String,LRUNode> map;
    private int capacity;
    private LRUNode head;
    private LRUNode tail;

    public LRUNode2(int capacity){
        this.capacity=capacity;
        this.map=new HashMap<>();
    }

    public void set(String key,Object value){
        LRUNode node=map.get(key);
        if (node!=null){
            node.value=value;
            remove(node,false);
        }else {
            node=new LRUNode(key,value);
            if (map.size()>=capacity){
                remove(tail,true);
            }
            map.put(key,node);
        }
        setHead(node);
    }

    private void setHead(LRUNode node) {
        if (head!=null){
            node.next=head;
            head.prev=node;
        }
        head=node;
        if(tail==null) tail=node;
    }

    private void remove(LRUNode node, boolean flag) {
        if (node.prev!=null){
            node.prev.next=node.next;
        }else {
            head=node.next;
        }
        if (node.next!=null){
            node.next.prev=node.prev;
        }else {
            tail=node.prev;
        }
        node.next=null;
        node.prev=null;
        if (flag) map.remove(node.key);
    }

    public static void main(String[] args) {
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
