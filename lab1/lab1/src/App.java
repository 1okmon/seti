import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.server.UID;
import java.util.HashMap;

public class App {
    public static void main(String[] args) throws IOException {

        MulticastSocket socket=new MulticastSocket(8000);
        socket.joinGroup(InetAddress.getByName("228.5.6.7"));
        byte [] buffer=new byte[1000];
        DatagramPacket packet= new DatagramPacket(buffer, buffer.length);
        HashMap<String,Integer> copies= new HashMap<>();

        Thread rThread= new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try {
                        socket.receive(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    String str=new String(packet.getData(),packet.getOffset(),packet.getLength());
                    synchronized(copies){
                        if(copies.get(str)==null)
                            copies.put(str,5);
                    }
                }
            }
        });

        Thread mThread= new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    synchronized (copies) {
                        System.out.println(copies);
                        copies.clear();
                    }
                }
            }
        });

        UID uid= new UID();
        String pts=uid.toString();
        byte[] msg=pts.getBytes();
        DatagramPacket msg1=new DatagramPacket(msg,msg.length,InetAddress.getByName("228.5.6.7"),8000);

        Thread wThread= new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    try {
                        socket.send(msg1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        mThread.start();
        rThread.start();
        wThread.start();
    }
}
