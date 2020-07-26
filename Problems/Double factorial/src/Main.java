import java.math.BigInteger;
import java.util.Scanner;

class DoubleFactorial {

    public static BigInteger calcDoubleFactorial(int n) {
        if(n == 0)
            return BigInteger.ONE;

        BigInteger input = new BigInteger(String.valueOf(n));
        BigInteger baseCase = input.mod(BigInteger.TWO) == BigInteger.ZERO ? BigInteger.TWO : BigInteger.ONE;
        BigInteger result = BigInteger.ONE;
        for(BigInteger i = input; !i.equals(baseCase); i = i.subtract(BigInteger.TWO)) {
            result = result.multiply(i);
        }
        return result.multiply(baseCase);
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int num = scanner.nextInt();
        System.out.println(calcDoubleFactorial(num).toString());

    }
}