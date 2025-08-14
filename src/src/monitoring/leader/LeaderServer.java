package monitoring.leader;

import monitoring.common.NodeId;
import monitoring.common.Snapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class LeaderServer {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Uso: LeaderServer <mcastGroup> <mcastPort> --nodes \"id,host,rmi,hb;...\"");
            return;
        }
        String mcastGroup = args[0];
        int mcastPort = Integer.parseInt(args[1]);
        String nodesArg = args[2].equals("--nodes") ? args[3] : args[2];

        List<NodeId> nodes = parseNodes(nodesArg);
        System.out.println("[Leader] Nós configurados: " + nodes);

        HeartbeatManager hb = new HeartbeatManager(800, 1);
        GlobalStateAggregator aggregator = new GlobalStateAggregator();
        MulticastPublisher publisher = new MulticastPublisher(mcastGroup, mcastPort);
        ElectionBully bully = new ElectionBully(null);

        Timer timer = new Timer("supervisor", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                try {
                    List<NodeId> alive = new ArrayList<>();
                    for (NodeId n : nodes) {
                        boolean ok = hb.ping(n);
                        if (!hb.isInactive(n) && ok) alive.add(n);
                    }
                    if (alive.isEmpty()) {
                        System.out.println("[Leader] Nenhum nó vivo no momento.");
                        return;
                    }
                    bully.elect(alive);
                    List<Snapshot> snapshots = aggregator.collect(alive);
                    String payload = snapshots.stream()
                            .map(s -> s.getNode().id() + ":" + s.getStatus().toString())
                            .collect(Collectors.joining(" | "));
                    System.out.println("[Leader] Publicando: " + payload);
                    publisher.publish(payload);
                } catch (Exception e) {
                    System.err.println("[Leader] Erro ciclo: " + e.getMessage());
                }
            }
        }, 1000, 5000);

        System.out.println("[Leader] Rodando. Pressione Ctrl+C para sair.");
    }

    private static List<NodeId> parseNodes(String arg) {
        List<NodeId> list = new ArrayList<>();
        String[] parts = arg.split(";");
        for (String p : parts) {
            if (p.isBlank()) continue;
            String[] f = p.split(",");
            int id = Integer.parseInt(f[0].trim());
            String host = f[1].trim();
            int rmi = Integer.parseInt(f[2].trim());
            int hb = Integer.parseInt(f[3].trim());
            list.add(new NodeId(id, host, rmi, hb));
        }
        return list;
    }
}