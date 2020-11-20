package client;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.rmi.server.UID;

public class Client {
    public static void main(String[] args) {
        String fileName = new File(args[0]).getName();
        String filePath = new File(args[0]).getPath();

        int port = Integer.parseInt(args[2]);
        try {
            File file = new File(filePath);
            System.out.println(file.exists());
            FileInputStream fileInput = new FileInputStream(file);
            Socket client = new Socket(InetAddress.getByName(args[1]), port);
            byte[] outByte = new byte[16 * 1024];
            BufferedOutputStream out = new BufferedOutputStream(client.getOutputStream());
            UID uid = new UID();
            long sizeFile = file.length();
            short offset = 4;
            byte[] byteFileName = fileName.getBytes();
            byte[] byteClientName = uid.toString().getBytes();
            //Записываем имя пользователя и длину имени
            System.arraycopy(byteClientName, 0, outByte, offset, byteClientName.length);
            offset += 28;
            outByte[3] = (byte) byteClientName.length;

            //Записываем имя файла и длину имени
            System.arraycopy(ByteBuffer.allocate(4).putInt(byteFileName.length).array(), 2, outByte, offset, 2);
            offset += 2;
            System.arraycopy(byteFileName, 0, outByte, offset, byteFileName.length);
            offset += byteFileName.length;

            System.arraycopy(ByteBuffer.allocate(8).putLong(file.length()).array(), 0, outByte, offset, 8);
            offset += 8;

            //Записываем размер данных
            System.arraycopy(ByteBuffer.allocate(4).putInt(offset).array(), 2, outByte, 0, 2);

            //Устанавливаем необходимые флаги
            outByte[2] = 0b00000111;
            out.write(outByte, 0, offset);
            out.flush();

            while (fileInput.available() > 0) {
                int count = fileInput.read(outByte, 3, outByte.length - 3);
                outByte[2] = 0b00001000;
                System.arraycopy(ByteBuffer.allocate(4).putInt(count + 3).array(), 2, outByte, 0, 2);

                out.write(outByte, 0, count + 3);
                out.flush();
            }

            outByte[2] = 0b00010000;
            System.arraycopy(ByteBuffer.allocate(4).putInt(3).array(), 2, outByte, 0, 2);

            out.write(outByte, 0, 10);
            out.flush();

            fileInput.close();
            byte[] tmp = new byte[16 * 1024];
            client.getInputStream().read(tmp);
            int len = ByteBuffer.wrap(tmp, 0, 2).getShort();
            if ((tmp[2] & 0b00100000) == 32) {
                System.out.println(new String(tmp, 3, len - 3));
            }
            client.close();
        }
        catch (UnknownHostException e){
            System.out.println("Problems with host");
        } catch (FileNotFoundException e){
            System.out.println("Bad address or file name");
        } catch (IOException e) {
            System.out.println("Connection failed");
        }

    }
}
