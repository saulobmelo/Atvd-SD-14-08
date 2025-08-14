package monitoring.common;

import java.io.Serializable;
import java.time.Instant;

public class Snapshot implements Serializable {
    private final NodeId node;
    private final ResourceStatus status;
    private final Instant capturedAt;

    public Snapshot(NodeId node, ResourceStatus status, Instant capturedAt) {
        this.node = node;
        this.status = status;
        this.capturedAt = capturedAt;
    }

    public NodeId getNode() { return node; }
    public ResourceStatus getStatus() { return status; }
    public Instant getCapturedAt() { return capturedAt; }
}