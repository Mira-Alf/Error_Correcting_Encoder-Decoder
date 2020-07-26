import java.io.BufferedReader;
import java.io.InputStreamReader;

class Main {
    public static void main(String[] args) throws Exception {
        StringBuilder result = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line = reader.readLine();
        while(line!=null) {
            result.append(line);
            line = reader.readLine();
        }
        reader.close();
        System.out.println(result.reverse());
    }
}