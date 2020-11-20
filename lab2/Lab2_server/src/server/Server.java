package server;

import client.Client;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private final ServerSocket serverSocket;
    private final List<Client> activeClient;

    public  Server(int port) throws IOException {
        serverSocket = new ServerSocket(port,10, InetAddress.getByName("127.0.0.1"));
        activeClient = new ArrayList<>();
        File f = new File("uploads");
        if (!f.exists()){
            f.mkdir();
        }
    }

    public void launch(){
        manage();
        accept();
    }

    private void manage() {
        new Thread(() -> {
            while (true) {
                synchronized (activeClient) {
                    if (activeClient.size() > 0) {
                        System.out.println("Speed of active clients:");
                        activeClient.forEach(e -> System.out.println("Client name is: " + e.getName() + " from IP " + e.getInetAddress() +""+ " speed is: " + + e.getMomentSpeed() + "KB/s : " +  e.getSessionSpeed() + "KB/s : transfered data : "+e.tranferedData()+ "Mb"));
                        System.out.println("--------------");
                    }

                    activeClient.removeIf(client -> !client.isActive());
                }
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }).start();
    }

    private void accept() {
        while (true) {
            try {
                Socket client = serverSocket.accept();
                System.out.println("Client accept");
                Client newClient = new Client(client);
                newClient.start();

                synchronized (activeClient) {
                    activeClient.add(newClient);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
