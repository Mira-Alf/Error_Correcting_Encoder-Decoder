import java.io.InputStream;

class Main {
    public static void main(String[] args) throws Exception {
        InputStream inputStream = System.in;
        int byteRead = inputStream.read();
        while(byteRead!=-1) {
            System.out.print(byteRead);
            byteRead = inputStream.read();
        }
        inputStream.close();
    }
}