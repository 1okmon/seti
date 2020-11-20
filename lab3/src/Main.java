import java.net.InetAddress;

        import java.io.BufferedReader;
        import java.io.IOException;
        import java.io.InputStreamReader;
        import java.net.*;
        import java.util.concurrent.ConcurrentLinkedQueue;

public class Main {

    public static void main(String[] args) {
        try {
            String username;
            final int port;
            final int percentage;
            InetAddress parentIP;
            int parentPort;
            InetSocketAddress parent = null;

            if (args.length == 3){
                username = args[0];
                port = Integer.parseInt(args[1]);
                percentage = Integer.parseInt(args[2]);
            } else if (args.length == 5){
                username = args[0];
                port = Integer.parseInt(args[1]);
                percentage = Integer.parseInt(args[2]);
                parentIP = InetAddress.getByName(args[3]);
                parentPort = Integer.parseInt(args[4]);
                parent = new InetSocketAddress(parentIP, parentPort);
            } else {
                System.err.println("invalid arguements");
                return;
            }


            ConcurrentLinkedQueue<String> messageList = new ConcurrentLinkedQueue<>();

            Station station = new Station(port, parent, percentage, messageList);
            new Thread(station).start();


            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            while (true) {
                try {
                    String message = username +": "+ br.readLine();
                    messageList.add(message); /*#fixed*/
                    /*station.getMsg(message); #old_version#*/
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.err.println("couldn't connect to parrent");
        } catch (SocketException e) {
            System.err.println("couldn't create a socket");
            e.printStackTrace();
        }
    }
}
