package node;

import common.LamportClock;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ClockSyncServer implements Runnable {
    private final int port;
    private final LamportClock clock;
    private volatile boolean running = true;

    public ClockSyncServer(int port, LamportClock clock) {
        this.port = port;
        this.clock = clock;
    }

    public void shutdown() {
        running = false;
    }

    @Override
    public void run() {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("[ClockSync] Servidor iniciado na porta " + port);
            while (running) {
                try (Socket s = server.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                     PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {
                    String line = in.readLine();
                    if (line != null && line.startsWith("CLOCK ")) {
                        long received = Long.parseLong(line.substring(6).trim());
                        long newVal = clock.updateOnReceive(received);
                        System.out.println("[ClockSync] Clock atualizado para " + newVal);
                        out.println("OK");
                    }
                } catch (IOException ignored) {}
            }
        } catch (IOException e) {
            System.err.println("[ClockSync] Erro: " + e.getMessage());
        }
    }
}