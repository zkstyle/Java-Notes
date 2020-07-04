package handwrite.node;

/**
 * @BelongsProject: Java-Notes
 * @BelongsPackage: handwrite.node
 * @Author: elvis
 * @CreateTime: 2020-07-03 22:57
 * @Description: LRU节点类
 */
public class LRUNode {

    public String key;
    public Object value;
    public LRUNode prev;
    public LRUNode next;
    public LRUNode(String key, Object value) {
        this.key = key;
        this.value = value;
    }
}
