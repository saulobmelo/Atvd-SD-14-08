package monitoring.node;

import monitoring.common.LamportClock;
import monitoring.common.NodeId;
import monitoring.rmi.GetStatusService;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class WorkerNode {
    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            System.out.println("Uso: WorkerNode <id> <host> <rmiPort> <hbPort> <clockPort>");
            return;
        }
        int id = Integer.parseInt(args[0]);
        String host = args[1];
        int rmiPort = Integer.parseInt(args[2]);
        int hbPort = Integer.parseInt(args[3]);
        int clockPort = Integer.parseInt(args[4]);

        NodeId me = new NodeId(id, host, rmiPort, hbPort);
        System.out.println("[Worker] Iniciando " + me);

        // Heartbeat
        HeartbeatResponder hb = new HeartbeatResponder(hbPort);
        Thread hbThread = new Thread(hb, "hb-responder");
        hbThread.start();

        // Clock e serviço de status
        LamportClock clock = new LamportClock();
        GetStatusService impl = new GetStatusServiceImpl(clock);
        GetStatusService stub = (GetStatusService) UnicastRemoteObject.exportObject(impl, 0);

        // Clock Sync Server
        ClockSyncServer clockServer = new ClockSyncServer(clockPort, clock);
        Thread clockThread = new Thread(clockServer, "clock-sync");
        clockThread.start();

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
        System.out.println("[Worker] Pronto. Pressione Ctrl+C para sair.");
    }
}