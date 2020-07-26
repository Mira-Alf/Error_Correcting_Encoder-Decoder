import java.math.BigInteger;
import java.util.Scanner;

class Main {

    public static BigInteger factorial(BigInteger num) {
        if(num.equals(BigInteger.ZERO) || num.equals(BigInteger.ONE))
            return BigInteger.ONE;
        return num.multiply(factorial(num.subtract(BigInteger.ONE)));
    }
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        BigInteger numLong = scanner.nextBigInteger();

        for(BigInteger i = BigInteger.ZERO; !i.equals(numLong); i = i.add(BigInteger.ONE)) {
            BigInteger doubleFactorial = factorial(i);
            if(doubleFactorial.compareTo(numLong) >= 0) {
                System.out.println(i);
                break;
            }
        }
    }
}