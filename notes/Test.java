
import java.lang.ref.PhantomReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.channels.DatagramChannel;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @BelongsProject: Java-Notes
 * @BelongsPackage: PACKAGE_NAME
 * @Author: elvis
 * @CreateTime: 2020-04-13 14:58
 * @Description:
 */
public class Test implements InvocationHandler {

    private static final int anInt = new Random().nextInt(20);
    public static void main(String[] args){
        //Proxy.newProxyInstance(Object obj,);
        //InvocationHandler
        ReentrantLock lock;
        LinkedHashMap linkedHashMap;
        CountDownLatch latch;
        ExecutorService executor;
        Phaser phaser;
        HashMap hashMap;
        Semaphore semaphore;
        SortedSet sortedSet;
        ConcurrentHashMap concurrentHashMap;
        ArrayList<Integer> list;
        Vector vector;
        LinkedList linkedList;
        HashSet set;
        AbstractQueuedSynchronizer synchronizer;
        ReentrantLock lock1;

        //JUC数据结构
        CopyOnWriteArrayList list1;
        CopyOnWriteArraySet set2;

        ConcurrentSkipListMap map;
        ConcurrentLinkedQueue queue;
        ConcurrentLinkedDeque deque;

        ThreadPoolExecutor executor1;



        Test.inner in=new Test().new inner();
        System.out.println(in.a);

        in in1=new in();
        System.out.println(in1.b);

        out out=new Test().new out();
        System.out.println(out.c);

        pri pri=new Test().new pri();
        System.out.println(pri.d);



    }

    class inner{
        int a=0;

    }

    static class in{
        int b=1;
    }

    public class out{
        int c=2;
    }

    private class pri{
        int d=4;
    }




























    private Object obj;

    //传入被代理对象
    public Object getProxy(Object objA){
        this.obj = objA;

        this.obj=objA;
        Object objectProxy=Proxy.newProxyInstance(obj.getClass().getClassLoader(),obj.getClass().getInterfaces(),this);
        return objectProxy;
        //创建代理对象，并关联被代理对象
        /*Object objProxy = Proxy.newProxyInstance(objA.getClass().getClassLoader(),
                objA.getClass().getInterfaces(), this);
        return objProxy;*/
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object o= method.invoke(obj,args);

        return o;
    }
}
