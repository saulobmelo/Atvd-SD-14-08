package monitoring.leader;

import monitoring.common.LamportClock;
import monitoring.common.NodeId;
import monitoring.common.Snapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class LeaderServer {
    public static void main(String[] args) {

        LamportClock leaderClock = new LamportClock();

        if (args.length < 6) {
            System.out.println("Uso: LeaderServer <mcastGroup> <mcastPort> <authPort> <myId> --nodes \"id,host,rmi,hb;...\"");
            return;
        }
        String mcastGroup = args[0];
        int mcastPort = Integer.parseInt(args[1]);
        int authPort = Integer.parseInt(args[2]);
        int myIdNum = Integer.parseInt(args[3]);
        String nodesArg = (args[4].equals("--nodes")) ? args[5] : args[4];

        List<NodeId> nodes = parseNodes(nodesArg);
        NodeId me = nodes.stream()
                .filter(n -> n.id() == myIdNum)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Meu ID não está na lista de nós!"));

        System.out.println("[Leader] Eu sou " + me);
        System.out.println("[Leader] Nós configurados: " + nodes);

        HeartbeatManager hb = new HeartbeatManager(800, 1);
        GlobalStateAggregator aggregator = new GlobalStateAggregator();
        MulticastPublisher publisher = new MulticastPublisher(mcastGroup, mcastPort);
        ElectionBully bully = new ElectionBully(me);

        // Autenticação
        AuthService authService = new AuthService();
        AuthTcpServer authServer = new AuthTcpServer(authPort, authService);
        Thread authThread = new Thread(authServer, "auth-server");
        authThread.setDaemon(true);
        authThread.start();

        // Sincronização de clock
        ClockSyncClient clockSync = new ClockSyncClient(500);
        SnapshotRepository snapshotRepo = new SnapshotRepository();

        // Bully: servidor de mensagens
        BullyMessageServer bullyServer = new BullyMessageServer(
                me.hbPort() + 200,
                me,
                nodes,
                newLeader -> {
                    System.out.println("[Leader] Atualizando líder para: " + newLeader);
                    bully.elect(List.of(newLeader)); // atualiza internamente
                }
        );
        Thread bullyThread = new Thread(bullyServer, "bully-server");
        bullyThread.start();

        // Supervisor: ciclo de coleta e publicação
        Timer timer = new Timer("supervisor", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    NodeId currentLeader = bully.getLeader();
                    if (currentLeader != null && currentLeader.id() != me.id()) {
                        System.out.println("[Leader] Não sou o líder, aguardando...");
                        return;
                    }

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

                    long currentClock = leaderClock.tick();
                    for (NodeId n : alive) {
                        int clockPort = n.hbPort() + 100;
                        clockSync.sendClock(n.host(), clockPort, currentClock);
                    }

                    List<Snapshot> snapshots = aggregator.collect(alive);
                    snapshotRepo.store(currentClock, snapshots);

                    String payload = snapshots.stream()
                            .map(s -> s.getNode().id() + ":" + s.getStatus().toString())
                            .collect(Collectors.joining(" | "));

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