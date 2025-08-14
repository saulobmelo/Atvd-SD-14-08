package node;

import common.Messages;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class HeartbeatResponder implements Runnable {
    private final int port;
    private volatile boolean running = true;

    public HeartbeatResponder(int port) {
        this.port = port;
    }

    public void shutdown() {
        running = false;
    }

    @Override
    public void run() {
        try (ServerSocket server = new ServerSocket(port)) {
            while (running) {
                try (Socket s = server.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                     PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {
                    String line = in.readLine();
                    if (Messages.HB_PING.equals(line)) {
                        out.println(Messages.HB_PONG);
                    }
                } catch (IOException ignored) {}
            }
        } catch (IOException e) {
            System.err.println("[HB-Responder] Erro: " + e.getMessage());
        }
    }
}