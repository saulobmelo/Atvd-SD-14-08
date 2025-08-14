package monitoring.leader;

import monitoring.common.NodeId;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.function.Consumer;

public class BullyMessageServer implements Runnable {
    private final int port;
    private final NodeId myId;
    private final List<NodeId> allNodes;
    private final Consumer<NodeId> onNewLeader;
    private volatile boolean running = true;

    public BullyMessageServer(int port, NodeId myId, List<NodeId> allNodes, Consumer<NodeId> onNewLeader) {
        this.port = port;
        this.myId = myId;
        this.allNodes = allNodes;
        this.onNewLeader = onNewLeader;
    }

    public void shutdown() {
        running = false;
    }

    @Override
    public void run() {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("[BullyServer] Ouvindo na porta " + port);
            while (running) {
                try (Socket s = server.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                     PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {

                    String line = in.readLine();
                    if (line == null) continue;

                    if (line.startsWith("ELECTION")) {
                        System.out.println("[BullyServer] Recebi pedido de eleição de " + line);
                        // Responde OK se meu ID for maior
                        int senderId = Integer.parseInt(line.split(" ")[1]);
                        if (myId.id() > senderId) {
                            out.println("OK");
                            // Inicia eleição própria
                            new Thread(() -> BullyMessageClient.startElection(myId, allNodes, onNewLeader)).start();
                        } else {
                            out.println("IGNORE");
                        }
                    } else if (line.startsWith("LEADER")) {
                        int leaderId = Integer.parseInt(line.split(" ")[1]);
                        NodeId leader = allNodes.stream()
                                .filter(n -> n.id() == leaderId)
                                .findFirst().orElse(null);
                        if (leader != null) {
                            System.out.println("[BullyServer] Novo líder anunciado: " + leader);
                            onNewLeader.accept(leader);
                        }
                        out.println("ACK");
                    }
                } catch (IOException ignored) {}
            }
        } catch (IOException e) {
            System.err.println("[BullyServer] Erro: " + e.getMessage());
        }
    }
}