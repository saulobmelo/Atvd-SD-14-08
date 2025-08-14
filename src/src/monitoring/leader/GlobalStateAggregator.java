package monitoring.leader;

import monitoring.common.NodeId;
import monitoring.common.ResourceStatus;
import monitoring.common.Snapshot;
import monitoring.rmi.GetStatusService;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class GlobalStateAggregator {
    public List<Snapshot> collect(List<NodeId> nodes) {
        List<Snapshot> list = new ArrayList<>();
        for (NodeId n : nodes) {
            try {
                Registry reg = LocateRegistry.getRegistry(n.host(), n.rmiPort());
                String name = "GetStatusService-" + n.id();
                GetStatusService svc = (GetStatusService) reg.lookup(name);
                ResourceStatus rs = svc.getStatus();
                list.add(new Snapshot(n, rs, Instant.now()));
            } catch (RemoteException | NotBoundException e) {
                System.err.println("[Aggregator] Falha ao consultar nó " + n + ": " + e.getMessage());
            }
        }
        return list;
    }
}