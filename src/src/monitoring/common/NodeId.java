package monitoring.common;

import java.io.Serializable;

public class NodeId implements Comparable<NodeId>, Serializable {
    private final int id;
    private final String host;
    private final int rmiPort;
    private final int hbPort;

    public NodeId(int id, String host, int rmiPort, int hbPort) {
        this.id = id;
        this.host = host;
        this.rmiPort = rmiPort;
        this.hbPort = hbPort;
    }

    public int id() { return id; }
    public String host() { return host; }
    public int rmiPort() { return rmiPort; }
    public int hbPort() { return hbPort; }

    @Override
    public int compareTo(NodeId o) {
        return Integer.compare(this.id, o.id);
    }

    @Override
    public String toString() {
        return "NodeId{" +
                "id=" + id +
                ", host='" + host + '\'' +
                ", rmiPort=" + rmiPort +
                ", hbPort=" + hbPort +
                '}';
    }
}