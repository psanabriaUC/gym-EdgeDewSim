package cl.puc.ing.edgedewsim;

import cl.puc.ing.edgedewsim.server.SimulatorRunnable;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class RemoteSimulator {
    private static int PORT = 3000;
    private static volatile boolean running = true;

    public static void main(String[] args) {
        try {
            if (args.length > 0) {
                PORT = Integer.parseInt(args[0]);
            }
            ServerSocket server = new ServerSocket(PORT);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                running = false;
                System.out.println("Server shutdown signal received!");
            }));

            System.out.println("Server starting in port " + PORT);
            while (running) {
                Socket socket = server.accept();
                socket.setTcpNoDelay(true); 
                Runnable runnable = new SimulatorRunnable(socket);
                Thread thread = new Thread(runnable);
                thread.start();
            }
            System.out.println("Server shutdown, Bye!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
