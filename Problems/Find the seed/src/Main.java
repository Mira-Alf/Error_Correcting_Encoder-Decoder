import java.util.*;

public class Main {

    public static int getMinimumPosition(int[] array) {
        int minimum = array[0], indexPosition = 0;
        for( int i = 1; i < array.length; i++ ) {
            if(array[i]<minimum) {
                minimum = array[i];
                indexPosition = i;
            }
        }
        return indexPosition;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int a = scanner.nextInt();
        int b = scanner.nextInt();
        int n = scanner.nextInt();
        int k = scanner.nextInt();

        int[] seeds = new int[b-a+1];
        for(int i = a, counter=0; i <= b; i++, counter++ ) {
            seeds[counter] = i;
        }

        int[] maximums = new int[seeds.length];
        for(int i = 0; i < seeds.length; i++) {
            Random random = new Random(seeds[i]);
            maximums[i] = random.nextInt(k);
            for(int j = 1; j < n; j++) {
                int number = random.nextInt(k);
                if( number > maximums[i] )
                    maximums[i] = number;
            }
        }
        int index = getMinimumPosition(maximums);
        System.out.println(seeds[index]);
        System.out.println(maximums[index]);

    }
}