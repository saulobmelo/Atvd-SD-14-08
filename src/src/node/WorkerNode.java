package node;

import common.LamportClock;
import common.NodeId;
import leader.BullyMessageClient;
import leader.BullyMessageServer;
import rmi.GetStatusService;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class WorkerNode {
    public static void main(String[] args) throws Exception {
        if (args.length < 6) {
            System.out.println("Uso: WorkerNode <id> <host> <rmiPort> <hbPort> <clockPort> \"id,host,rmi,hb;...\"");
            return;
        }
        int id = Integer.parseInt(args[0]);
        String host = args[1];
        int rmiPort = Integer.parseInt(args[2]);
        int hbPort = Integer.parseInt(args[3]);
        int clockPort = Integer.parseInt(args[4]);
        String nodesArg = args[5];

        NodeId me = new NodeId(id, host, rmiPort, hbPort);
        System.out.println("[Worker] Iniciando " + me);

        // Monta lista de todos os nós a partir do argumento
        List<NodeId> allNodes = parseNodes(nodesArg);

        // Heartbeat responder
        HeartbeatResponder hb = new HeartbeatResponder(hbPort);
        Thread hbThread = new Thread(hb, "hb-responder");
        hbThread.start();

        // Clock sync responder
        LamportClock clock = new LamportClock();
        ClockSyncServer clockServer = new ClockSyncServer(clockPort, clock);
        Thread clockThread = new Thread(clockServer, "clock-sync");
        clockThread.start();

        // Serviço RMI
        GetStatusService impl = new GetStatusServiceImpl(clock);
        GetStatusService stub = (GetStatusService) UnicastRemoteObject.exportObject(impl, 0);

        Registry registry;
        try {
            registry = LocateRegistry.createRegistry(rmiPort);
            System.out.println("[Worker] RMI Registry criado na porta " + rmiPort);
        } catch (RemoteException e) {
            System.out.println("[Worker] RMI Registry já existente? Tentando localizar...");
            registry = LocateRegistry.getRegistry(rmiPort);
        }

        String bindingName = "GetStatusService-" + id;
        try {
            registry.bind(bindingName, stub);
        } catch (AlreadyBoundException ex) {
            registry.rebind(bindingName, stub);
        }
        System.out.println("[Worker] Serviço RMI publicado como '" + bindingName + "'");

        // Servidor de mensagens Bully
        BullyMessageServer bullyServer = new BullyMessageServer(
                hbPort + 200, // porta dedicada ao Bully
                me,
                allNodes,
                newLeader -> {
                    System.out.println("[Worker] Novo líder atualizado para: " + newLeader);
                }
        );
        Thread bullyThread = new Thread(bullyServer, "bully-server");
        bullyThread.start();

        // Thread para monitorar o líder e iniciar eleição
        new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(5000); // intervalo de checagem
                    NodeId currentLeader = findCurrentLeader(allNodes);
                    if (currentLeader == null || !isLeaderAlive(currentLeader)) {
                        System.out.println("[Worker] Líder inativo. Iniciando eleição...");
                        BullyMessageClient.startElection(me, allNodes, newLeader -> {
                            System.out.println("[Worker] Novo líder eleito: " + newLeader);
                        });
                    }
                }
            } catch (InterruptedException ignored) {}
        }, "leader-monitor").start();

        System.out.println("[Worker] Pronto. Pressione Ctrl+C para sair.");
    }

    // Verifica se líder responde ao heartbeat
    private static boolean isLeaderAlive(NodeId leader) {
        try (java.net.Socket s = new java.net.Socket()) {
            s.connect(new java.net.InetSocketAddress(leader.host(), leader.hbPort()), 800);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Para simplificação, aqui assume que o líder é o maior ID vivo
    private static NodeId findCurrentLeader(List<NodeId> nodes) {
        return nodes.stream().max(NodeId::compareTo).orElse(null);
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