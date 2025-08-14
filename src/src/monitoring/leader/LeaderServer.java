package monitoring.leader;

import monitoring.common.NodeId;
import monitoring.common.Snapshot;
import monitoring.leader.ClockSyncClient;
import monitoring.leader.SnapshotRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class LeaderServer {
    public static void main(String[] args) {
        if (args.length < 5) {
            System.out.println("Uso: LeaderServer <mcastGroup> <mcastPort> <authPort> --nodes \"id,host,rmi,hb;...\"");
            return;
        }
        String mcastGroup = args[0];
        int mcastPort = Integer.parseInt(args[1]);
        int authPort = Integer.parseInt(args[2]);
        String nodesArg = (args.length >= 4 && args[3].equals("--nodes")) ? args[4] : args[3];

        List<NodeId> nodes = parseNodes(nodesArg);
        System.out.println("[Leader] Nós configurados: " + nodes);

        HeartbeatManager hb = new HeartbeatManager(800, 1);
        GlobalStateAggregator aggregator = new GlobalStateAggregator();
        MulticastPublisher publisher = new MulticastPublisher(mcastGroup, mcastPort);
        ElectionBully bully = new ElectionBully(null);

        ClockSyncClient clockSync = new ClockSyncClient(500);
        SnapshotRepository snapshotRepo = new SnapshotRepository();

        // Autenticação
        AuthService authService = new AuthService();
        AuthTcpServer authServer = new AuthTcpServer(authPort, authService);
        Thread authThread = new Thread(authServer, "auth-server");
        authThread.setDaemon(true);
        authThread.start();

        Timer timer = new Timer("supervisor", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
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

                    // Gerar clock lógico para este ciclo (futuramente pode ser um LamportClock no líder)
                    long currentClock = System.currentTimeMillis(); // simplificado para agora

                    // Enviar clock para todos os nós antes da coleta
                    for (NodeId n : alive) {
                        // Aqui vamos supor que a porta do ClockSync seja hbPort + 100
                        int clockPort = n.hbPort() + 100;
                        boolean okClock = clockSync.sendClock(n.host(), clockPort, currentClock);
                        if (!okClock) {
                            System.err.println("[Leader] Falha ao sincronizar clock com " + n);
                        }
                    }

                    // Coletar snapshots com o clock sincronizado
                    List<Snapshot> snapshots = aggregator.collect(alive);

                    // Guardar snapshot no repositório para histórico
                    snapshotRepo.store(currentClock, snapshots);

                    // Montar payload
                    String payload = snapshots.stream()
                            .map(s -> s.getNode().id() + ":" + s.getStatus().toString())
                            .collect(Collectors.joining(" | "));

                    // Publicar por token
                    if (authService.validTokens().isEmpty()) {
                        System.out.println("[Leader] Sem clientes autenticados no momento.");
                        return;
                    }

                    for (String token : authService.validTokens()) {
                        String msg = "[TOKEN:" + token + "] " + payload;
                        System.out.println("[Leader] Publicando p/ token " + token + ": " + payload);
                        publisher.publish(msg);
                    }

                } catch (Exception e) {
                    System.err.println("[Leader] Erro ciclo: " + e.getMessage());
                }
            }
        }, 1000, 5000);

        System.out.println("[Leader] Rodando (Auth@" + authPort + "). Pressione Ctrl+C para sair.");
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