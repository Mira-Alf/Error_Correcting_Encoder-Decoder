import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int k = scanner.nextInt();
        int n = scanner.nextInt();
        double m = scanner.nextDouble();
        //System.out.printf("K=%d, N=%d, M=%f%n", k, n, m);
        int seed = k, counter = n;
        Random random = new Random(seed);

        while(counter > 0) {

            double randInt = random.nextGaussian();
            //System.out.printf("Seed=%d, Random=%f%n", seed, randInt);

            if(randInt <= m) {
                counter--;
            }
            else {
                seed++;
                random = new Random(seed);
                counter = n;
            }
        }
        System.out.println(seed);
    }
}