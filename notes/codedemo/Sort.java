package codedemo;

import java.util.*;
import java.util.stream.IntStream;

/**
 * @BelongsProject: Java-Notes
 * @BelongsPackage: PACKAGE_NAME
 * @Author: elvis
 * @CreateTime: 2020-04-30 11:00
 * @Description: 集中联系常见排序算法
 */
public class Sort {

    public static int biSearch(int[] array, int a) {
        int lo = 0;
        int hi = array.length - 1;
        int mid;
        while (lo <= hi) {
            mid = (lo + hi) / 2;//中间位置
            if (array[mid] == a) {
                return mid + 1;
            } else if (array[mid] < a) { //向右查找
                lo = mid + 1;
            } else { //向左查找
                hi = mid - 1;
            }
        }
        return -1;
    }

    public static void bubbleSort(int[] a, int n) {
        int i, j;
        for (i = 0; i < n; i++) {//表示 n 次排序过程。
            for (j = 1; j < n - i; j++) {
                if (a[j - 1] > a[j]) {//前面的数字大于后面的数字就交换
                    //交换 a[j-1]和 a[j]
                    int temp;
                    temp = a[j - 1];
                    a[j - 1] = a[j];
                    a[j] = temp;
                }
            }
        }
    }


    public static void Insertsort(int arr[]) {
        for (int i = 1; i < arr.length; i++) {
            //插入的数
            int insertVal = arr[i];
            //被插入的位置(准备和前一个数比较)
            int index = i - 1;
            //如果插入的数比被插入的数小
            while (index >= 0 && insertVal < arr[index]) {
                //将把 arr[index] 向后移动
                arr[index + 1] = arr[index];
                //让 index 向前移动
                index--;
            }
            //把插入的数放入合适位置
            arr[index + 1] = insertVal;
        }

    }

    /**
     * 快速排序的原理：选择一个关键值作为基准值。比基准值小的都在左边序列（一般是无序的），比基准值大的都在右边（一般是无序的）。
     * 一般选择序列的第一个元素。一次循环：从后往前比较，用基准值和最后一个值比较，如果比基准值小的交换位置，如果没有继续比较下一个，
     * 直到找到第一个比基准值小的值才交换。找到这个值之后，又从前往后开始比较，如果有比基准值大的，交换位置，如果没有继续比较下一个，
     * 直到找到第一个比基准值大的值才交换。直到从前往后的比较索引>从后往前比较的索引，结束第一次循环，此时，对于基准值来说，左右两边就是有序
     * @param a
     * @param left
     * @param right
     */
    public static void quickSort(int[] a, int left, int right) {
        if (left > right) return;
        int pivot = a[left];//定义基准值为数组第一个数
        int i = left;
        int j = right;

        while (i < j) {
            while (i < j && pivot <= a[j]) j--;//从右往左找比基准值小的数

            while (i < j && pivot >= a[i]) i++;  //从左往右找比基准值大的数

            if (i < j) {                    //如果i<j，交换它们
                int temp = a[i];
                a[i] = a[j];
                a[j] = temp;
            }
        }
        a[left] = a[i];
        a[i] = pivot;//把基准值放到合适的位置
        quickSort(a, left, i - 1);//对左边的子数组进行快速排序
        quickSort(a, i + 1, right);//对右边的子数组进行快速排序
    }

    public static void main(String[] args) {
        int[] num={6,2,3,5,9,0,4,7,1};
        //quickSort(num,0,num.length-1);
        //shellSort(a);
        //Insertsort(num);
        num=MergeSort(num);
        for (int i = 0; i < num.length; i++) {
            System.out.print(num[i]+",");
        }
        System.out.println();

        int[][] matrix = {{0, 30}, {15, 20}, {5, 10}, {-10, 20}};
        //排序
        Arrays.sort(matrix, (a, b) -> (a[0] - b[0]));
        //Arrays.sort(matrix,(a,b)->(a[1]-b[1]));
        /*Arrays.sort(matrix, new Comparator<int[]>() {
            @Override
            public int compare(int[] o1, int[] o2) {
                return o1[0]-o2[0];
            }
        });*/
        //打印结果
        for (int[] arr : matrix) {
            for (int a : arr) {
                System.out.print(a + " ");
            }
            System.out.println();
        }

    }


    /**
     * 希尔排序
     *
     * @param arr
     */
    public static void shellSort(int[] arr) {
        int n = arr.length;
        for (int step = n / 2; step > 0; step /= 2) {
            //下面这段代码和插入排序相同，只是这里的间隔是increment
            //直接插入排序的间隔是1

            for (int i = step; i < arr.length; i++) {
                int temp = arr[i];
                int t = i - step;
                while (t >= 0 && arr[t] > temp) {
                    arr[i] = arr[t];
                    t -= step;
                }
                arr[t + step] = temp;
            }
        }
        
    }



    public static int[] MergeSort(int[] array) {
        if (array.length < 2) return array;
        int mid = array.length / 2;
        int[] left = Arrays.copyOfRange(array, 0, mid);
        int[] right = Arrays.copyOfRange(array, mid, array.length);
        return merge(MergeSort(left), MergeSort(right));
    }
    /**
     * 归并排序——将两段排序好的数组结合成一个排序数组
     */
    public static int[] merge(int[] left, int[] right) {
        int leftLen=left.length,rightLen=right.length;
        int[] result = new int[leftLen+rightLen];
        for (int index = 0, i = 0, j = 0; index < result.length; index++) {
            int l=i<leftLen?left[i]:Integer.MAX_VALUE;
            int r=j<rightLen?right[j]:Integer.MAX_VALUE;
            result[index]=l<r?left[i++]:right[j++];
        }
        return result;
    }




    //声明全局变量，用于记录数组array的长度；
    static int len;
    /**
     * 堆排序算法
     */
    public static int[] HeapSort(int[] array) {
        len = array.length;
        if (len < 1) return array;
        //1.构建一个最大堆
        buildMaxHeap(array);
        //2.循环将堆首位（最大值）与末位交换，然后在重新调整最大堆
        while (len > 0) {
            swap(array, 0, len - 1);
            len--;
            adjustHeap(array, 0);
        }
        return array;
    }

    private static void swap(int[] array, int i, int j) {
        int tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }

    /**
     * 建立最大堆
     *
     * @param array
     */
    public static void buildMaxHeap(int[] array) {
        //从最后一个非叶子节点开始向上构造最大堆
        //for循环这样写会更好一点：i的左子树和右子树分别2i+1和2(i+1)
        for (int i = (len/2- 1); i >= 0; i--) {
            adjustHeap(array, i);
        }
    }
    /**
     * 调整使之成为最大堆
     *
     * @param array
     * @param i
     */
    public static void adjustHeap(int[] array, int i) {
        int maxIndex = i;
        //如果有左子树，且左子树大于父节点，则将最大指针指向左子树
        if (i * 2 < len && array[i * 2] > array[maxIndex])
            maxIndex = i * 2 + 1;
        //如果有右子树，且右子树大于父节点，则将最大指针指向右子树
        if (i * 2 + 1 < len && array[i * 2 + 1] > array[maxIndex])
            maxIndex = i * 2 + 2;
        //如果父节点不是最大值，则将父节点与最大值交换，并且递归调整与父节点交换的位置。
        if (maxIndex != i) {
            swap(array, maxIndex, i);
            adjustHeap(array, maxIndex);
        }
    }


    /**
     * 排序汇总
     */






}
