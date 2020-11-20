package server;

import java.io.IOException;

public class LauncherServer {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("There isn't port");
        } else {
            int port = Integer.parseInt(args[0]);
            try {
                Server server = new Server(port);
                server.launch();
            } catch (IOException e) {
                System.out.println("Try to change port");
            }
        }
    }
}
