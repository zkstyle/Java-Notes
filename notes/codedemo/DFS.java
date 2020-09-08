package codedemo;

import java.util.*;

/**
 * @BelongsProject: Java-Notes
 * @BelongsPackage: codedemo
 * @Author: elvis
 * @CreateTime: 2020-09-06 15:47
 * @Description:
 */
public class DFS {

    private static int[][] matrix;
    private static List<List<Integer>> res = new ArrayList<>();
    static int start ;

    public static void main(String[] args) {
        Scanner in  = new Scanner(System.in);

        int n = in.nextInt();
        in.nextLine();

        matrix = new int[n][n];

        for (int i = 0; i < n*n; i++) {
            matrix[i/n][i%n] = in.nextInt();
        }

        int need = in.nextInt();
        List<Integer> list = new ArrayList<>();
        Set<Integer> set = new HashSet<>();
        start = need;
        list.add(need);

        dfs(list, need, set);

        for (List<Integer> re : res) {
            Collections.reverse(re);
            for (Integer integer : re) {
                System.out.print(integer);
            }
            System.out.println();
        }




    }

    private static void dfs(List<Integer> list, int need, Set<Integer> set) {
        if (set.contains(need)&&need==start) {
            res.add(new ArrayList<>(list));
            return;
        }

        for (int i = 0; i < matrix.length; i++) {

            if (matrix[i][need]==1) {
                if(i!=start&&set.contains(i)) continue;
                list.add(i);
                set.add(need);
                dfs(list, i, set);
                list.remove(list.size()-1);
                set.remove(need);
            }
        }

    }

   /* static int[][] matrix;
    static List<String> ret = new ArrayList<>();
    static int pos;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        matrix = new int[n][n];
        for (int i = 0; i < n * n; i++) {
            matrix[i / n][i % n] = sc.nextInt();
        }
        pos = sc.nextInt();
        boolean[] use = new boolean[n];
        dfs(pos, use, new StringBuilder().append(pos));
        for (String s : ret) {
            System.out.println(s);
        }


    }

    private static void dfs(int idx, boolean[] use, StringBuilder sb) {
        if (use[idx] && idx == pos) {
            ret.add(sb.toString());
            return;
        }

        for (int i = 0; i < use.length; i++) {
            if (matrix[idx][i] == 1) {
                if (i != pos && use[i]) continue;
                sb.append(i);
                use[idx] = true;
                dfs(i, use, sb);
                use[idx] = false;
                sb.deleteCharAt(sb.length() - 1);
            }
        }
    }*/
}
