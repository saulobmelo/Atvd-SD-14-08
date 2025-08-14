package monitoring.leader;

import monitoring.common.NodeId;

import java.util.List;

public class ElectionBully {
    private volatile NodeId currentLeader;

    public ElectionBully(NodeId initialLeader) {
        this.currentLeader = initialLeader;
    }

    public NodeId getLeader() { return currentLeader; }

    public NodeId elect(List<NodeId> alive) {
        NodeId max = null;
        for (NodeId n : alive) {
            if (max == null || n.compareTo(max) > 0) max = n;
        }
        currentLeader = max;
        System.out.println("[Bully] Novo líder: " + currentLeader);
        return currentLeader;
    }
}