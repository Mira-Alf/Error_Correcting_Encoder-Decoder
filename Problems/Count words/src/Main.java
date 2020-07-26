import java.io.BufferedReader;
import java.io.InputStreamReader;

class Main {
    public static void main(String[] args) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line = reader.readLine().trim();
        System.out.println(line.equals("") ? 0 : line.split("\\s+").length);
        reader.close();
    }
}