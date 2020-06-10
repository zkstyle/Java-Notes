package codedemo;

import java.util.HashMap;

/**
 * @BelongsProject: Java-Notes
 * @BelongsPackage: codedemo
 * @Author: elvis
 * @CreateTime: 2020-06-07 09:39
 * @Description:
 */
public class Test02 {

    private static final int COUNT_BITS = Integer.SIZE - 3;
    private static final int CAPACITY   = (1 << COUNT_BITS) - 1;

    public static void main(String[] args) {
        System.out.println(COUNT_BITS);
    }
}
