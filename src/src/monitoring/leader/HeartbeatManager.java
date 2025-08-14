package monitoring.leader;

import monitoring.common.Messages;
import monitoring.common.NodeId;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HeartbeatManager {
    private final Map<NodeId, Integer> failures = new ConcurrentHashMap<>();
    private final int timeoutMs;
    private final int maxRetries;

    public HeartbeatManager(int timeoutMs, int maxRetries) {
        this.timeoutMs = timeoutMs;
        this.maxRetries = maxRetries;
    }

    public boolean ping(NodeId node) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(node.host(), node.hbPort()), timeoutMs);
                try (PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
                    out.println(Messages.HB_PING);
                    s.setSoTimeout(timeoutMs);
                    String reply = in.readLine();
                    if (Messages.HB_PONG.equals(reply)) {
                        failures.put(node, 0);
                        return true;
                    }
                }
            } catch (IOException ignored) {}
        }
        failures.merge(node, 1, Integer::sum);
        return false;
    }

    public boolean isInactive(NodeId node) {
        return failures.getOrDefault(node, 0) >= 3;
    }
}