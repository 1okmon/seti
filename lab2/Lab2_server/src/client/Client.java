package client;

import file.FileWriter;
import message.Reader;
import message.Writer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

public class Client {
    private boolean active;
    private final Socket socket;
    private double speed;
    private long sizeAllData;
    private final long startConnection;
    private String name;
    private FileWriter file;
    private long delData =0;

    public Client(Socket socket) {
        startConnection = System.currentTimeMillis();
        setActive(true);
        speed = 0;
        this.socket = socket;
        try {
            this.socket.setSoTimeout(3000);
        } catch (SocketException ignored) {
        }
        name = "Zero string";
    }

    public void start() {
        new Thread(() -> {
            try {
                Reader reader = new Reader(socket.getInputStream());
                Writer writer = new Writer(socket.getOutputStream());
                long data = reader.read();
                setSpeed(data / ((double)(System.currentTimeMillis() - startConnection) / 1024));
                sizeAllData += data;
                //delData=delData+data;

                new Thread(()->{
                    while (true) {
                        long startSizeData = sizeAllData;
                        try {
                            Thread.sleep(3000);
                           // delData = sizeAllData - startSizeData;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        delData = sizeAllData - startSizeData;
                    }

                }).start();
                setName(reader.getName());
                setSpeed(reader.getSpeed());
                file = new FileWriter(reader.getFileName());

                while (!reader.end()) {
                    sizeAllData += reader.read();
                    setSpeed(reader.getSpeed());
                    if(!reader.end()) {
                        file.write(reader.getData());
                    }
                }

                if (file.getSize() == reader.getFileSize()) {
                    writer.success();
                } else {
                    writer.fail();
                }

                disconnect();
            } catch (IOException e) {
                breakConnection();
            }
            setActive(false);
            System.out.println("Client disconnect");

        }).start();
    }

    private void disconnect() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        file.close();
    }

    private void breakConnection() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Break connection");
        if(file != null) file.delete();
        setActive(false);
    }

    public synchronized boolean isActive() {
        return active;
    }

    public synchronized void setActive(boolean active) {
        this.active = active;
    }

    public String getInetAddress() {
        return socket.getInetAddress().toString();
    }

    public synchronized String getName() {
        return name;
    }

    private synchronized void setName(String name) {
        this.name = name;
    }

    public synchronized double getMomentSpeed() {
       return delData/((double) 3000) * (1000 / (double) 1024);
       // return delData / ((double)(3000) / 1024);
    }

    public synchronized void setSpeed(double speed) {
        this.speed = speed;
    }

    public synchronized double tranferedData(){
        return sizeAllData / (1024 *1024);
    }

    public synchronized double getSessionSpeed() {
        return sizeAllData / ((double) System.currentTimeMillis() - startConnection) * (1000 / (double) 1024);
    }
}
