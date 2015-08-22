package thoth;

import java.io.*;

public class Utils {

    /**
     * Checks if <code>value</code> has given flag
     * @param value
     *      The value to check
     * @param flag
     *      The flag to search
     * @return
     *      <code>true</code> if <code>value</code> has given flag, <code>false</code> otherwise
     */
    public static boolean hasFlag(int value, int flag) {
        return (value & flag) != 0;
    }

    public static String readString(InputStream input, String charset) throws IOException {
        BufferedInputStream in = new BufferedInputStream(input);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream out = new BufferedOutputStream(baos);

        int i;
        byte[] buffer = new byte[1024*8];
        while((i = in.read(buffer)) != -1) {
            out.write(buffer, 0, i);
        }
        out.flush();
        baos.close();
        in.close();
        return new String(baos.toByteArray(), charset);
    }
}
