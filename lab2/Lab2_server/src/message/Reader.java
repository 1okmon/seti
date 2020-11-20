package message;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Reader {
    private boolean end;
    private final InputStream stream;
    private byte[] data = null;
    private String fileName;
    private double speed;
    private String name;
    private long fileSize;
    private  long lastData = System.currentTimeMillis();

    public Reader(InputStream inputStream) {
        end = false;
        this.stream = inputStream;
        fileName = "none";
        name = "none";
        speed = 0;
    }

    public String getName() {
        return name;
    }

    public long read() throws IOException {
        byte[] tmp = new byte[16 * 1024];
        int offset = 3;
        stream.read(tmp, 0, 2);
        int sizeData = ByteBuffer.wrap(tmp, 0, 2).getShort();
        stream.read(tmp, 2, sizeData - 2);
        speed = (int) (sizeData / (double) (System.currentTimeMillis() - lastData) * (1000 / (double) 1024));
        lastData = System.currentTimeMillis();

        if ((tmp[2] & 0b00000001) == 1) {
            offset++;
            name = new String(tmp, offset, tmp[3]);
            offset += 28;
        }

        if ((tmp[2] & 0b00000010) == 2) {
            int sizeFileName = ByteBuffer.wrap(tmp, offset, 2).getShort();
            offset += 2;
            fileName = new String(tmp, offset, sizeFileName);
            offset += sizeFileName;
        }

        if ((tmp[2] & 0b00000100) == 4) {
            fileSize = ByteBuffer.wrap(tmp, offset, 8).getLong();
            offset += 8;
        }

        if ((tmp[2] & 0b00001000) == 8) {
            data = new byte[sizeData - offset];
            System.arraycopy(tmp, offset, data, 0, sizeData - offset);
        }

        if ((tmp[2] & 0b00010000) == 16) {
            end = true;
        }
        return sizeData;
    }

    public double getSpeed() {
        return speed;
    }

    public String getFileName() {
        return fileName;
    }

    public boolean end() {
        return end;
    }

    public byte[] getData() {
        return data;
    }

    public long getFileSize() {
        return fileSize;
    }
}
