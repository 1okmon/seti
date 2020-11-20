package file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileWriter {
    private FileOutputStream outputStream;
    private File file;

    public FileWriter(String fileName) throws FileNotFoundException {
        String name = fileName;
        int i = 0;
        while ((file = new File("uploads/" + name)).exists()){
            name = i + fileName;
            ++i;
        }

        outputStream = new FileOutputStream(file);
    }

    public void delete() {
        try {
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        file.delete();
    }

    public void close() {
        try {
            outputStream.close();
        } catch (IOException ignored) {
        }
    }

    public void write(byte[] data) {
        try {
            outputStream.write(data);
        } catch (IOException ignored) {
        }
    }

    public long getSize() {
        return file.length();
    }
}
