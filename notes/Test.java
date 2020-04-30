
import java.lang.ref.PhantomReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.channels.DatagramChannel;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @BelongsProject: Java-Notes
 * @BelongsPackage: PACKAGE_NAME
 * @Author: elvis
 * @CreateTime: 2020-04-13 14:58
 * @Description:
 */
public class Test implements InvocationHandler {
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
