package codedemo;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * @BelongsProject: Java-Notes
 * @BelongsPackage: codedemo
 * @Author: elvis
 * @CreateTime: 2020-06-01 17:22
 * @Description: x
 */
public class Test00 {
   static int a=1;
    static void f(){
        System.out.println(a);
    }

    public static void main(String[] args) {
//        Arrays.asList("a","d","c","x","q","t","z","w").forEach(e -> System.out.println(e+","));
       Arrays.asList("a","d","c","x","q","t","z","w").sort((e1,e2)->{
            int res=e1.compareTo(e2);
           System.out.println(res);
            return res;
        } );

    }



}
