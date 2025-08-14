package leader;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ClockSyncClient {
    private final int timeoutMs;

    public ClockSyncClient(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public boolean sendClock(String host, int port, long clockVal) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), timeoutMs);
            try (PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
                out.println("CLOCK " + clockVal);
                String reply = in.readLine();
                return "OK".equals(reply);
            }
        } catch (IOException e) {
            System.err.println("[ClockSyncClient] Falha ao enviar clock para " + host + ":" + port);
            return false;
        }
    }
}