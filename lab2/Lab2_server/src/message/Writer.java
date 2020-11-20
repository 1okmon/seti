package message;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class Writer {
    private byte[] out;
    private OutputStream stream;


    public Writer(OutputStream outputStream) {
        this.stream = outputStream;
        out = new byte[16 * 1024];
    }

    public void success() {
        int count;
        out[2] = 0b00100000;
        byte[] success = "Success".getBytes();
        System.arraycopy(ByteBuffer.allocate(4).putInt(success.length + 3).array(), 2, out, 0, 2);
        System.arraycopy(success, 0, out, 3, success.length);
        count = success.length + 3;
        try {
            stream.write(out, 0, count);
            stream.flush();
        } catch (IOException ignored) {
        }
    }

    public void fail() {
        int count;
        out[2] = 0b00100000;
        byte[] fail = "Fail".getBytes();
        System.arraycopy(ByteBuffer.allocate(4).putInt(fail.length + 3).array(), 2, out, 0, 2);
        System.arraycopy(fail, 0, out, 3, fail.length);
        count = fail.length + 3;
        try {
            stream.write(out, 0, count);
            stream.flush();
        } catch (IOException ignored) {
        }
    }
}
