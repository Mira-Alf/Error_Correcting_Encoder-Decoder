import java.io.CharArrayWriter;
import java.io.IOException;

class Converter {
    public static char[] convert(String[] words) throws IOException {
        try(CharArrayWriter writer = new CharArrayWriter()) {
            for (String w : words)
                writer.write(w);
            return writer.toCharArray();
        }
    }
}