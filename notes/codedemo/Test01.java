package codedemo;

/**
 * @BelongsProject: Java-Notes
 * @BelongsPackage: PACKAGE_NAME
 * @Author: elvis
 * @CreateTime: 2020-05-14 15:06
 * @Description:
 */
public class Test01 {

    static class DListNode{
        int val;
        DListNode child;
        DListNode next;

        public DListNode(int val){
            this.val=val;
        }
    }
    static class ListNode{
        int val;
        ListNode next;
        public ListNode(int val){
            this.val=val;
        }
        public ListNode(){
        }
    }


    public  ListNode node;
    public  ListNode p;
    public  ListNode convert(DListNode head){
        if(head==null) return new ListNode();
        node =new ListNode(0);
        p=node;
        dfs(head);
        return node.next;
    }

    private void dfs(DListNode head){
        if(head==null) return;
        ListNode cur =new ListNode(head.val);
        p.next=cur;
        p=p.next;
        if(head.child!=null) dfs(head.child);
        if(head.next!=null) dfs(head.next);
    }

    public static void main(String[] args) {
        DListNode head=new DListNode(1);
        head.next=new DListNode(2);
        head.next.next=new DListNode(3);
        head.next.next.next=new DListNode(4);
        head.next.child=new DListNode(5);
        head.next.child.next=new DListNode(6);
        head.next.child.next.next=new DListNode(7);
        head.next.child.next.child=new DListNode(8);
        ListNode node=new Test01().convert(head);
        System.out.println(node);
    }
}
