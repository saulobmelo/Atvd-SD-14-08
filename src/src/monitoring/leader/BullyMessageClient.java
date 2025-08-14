package monitoring.leader;

import monitoring.common.NodeId;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.function.Consumer;

public class BullyMessageClient {

    public static void startElection(NodeId myId, List<NodeId> nodes, Consumer<NodeId> onNewLeader) {
        System.out.println("[BullyClient] Iniciando eleição. Meu ID: " + myId.id());
        boolean higherResponded = false;

        for (NodeId n : nodes) {
            if (n.id() <= myId.id()) continue; // só envia para IDs maiores
            if (sendMessage(n.host(), n.hbPort() + 200, "ELECTION " + myId.id())) {
                higherResponded = true;
            }
        }

        if (!higherResponded) {
            // Ninguém maior respondeu → sou líder
            announceLeader(myId, nodes);
            onNewLeader.accept(myId);
        }
    }

    private static boolean sendMessage(String host, int port, String msg) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), 1000);
            try (PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
                out.println(msg);
                String reply = in.readLine();
                return reply != null && reply.equals("OK");
            }
        } catch (IOException e) {
            return false;
        }
    }

    public static void announceLeader(NodeId leader, List<NodeId> nodes) {
        System.out.println("[BullyClient] Anunciando novo líder: " + leader);
        for (NodeId n : nodes) {
            if (n.id() == leader.id()) continue;
            sendMessage(n.host(), n.hbPort() + 200, "LEADER " + leader.id());
        }
    }
}