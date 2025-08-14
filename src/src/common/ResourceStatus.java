package common;

import java.io.Serializable;

public class ResourceStatus implements Serializable {
    private final double cpuLoad;
    private final long freeMemory;
    private final long totalMemory;
    private final long uptimeMillis;
    private final long lamport;

    public ResourceStatus(double cpuLoad, long freeMemory, long totalMemory, long uptimeMillis, long lamport) {
        this.cpuLoad = cpuLoad;
        this.freeMemory = freeMemory;
        this.totalMemory = totalMemory;
        this.uptimeMillis = uptimeMillis;
        this.lamport = lamport;
    }

    public double getCpuLoad() { return cpuLoad; }
    public long getFreeMemory() { return freeMemory; }
    public long getTotalMemory() { return totalMemory; }
    public long getUptimeMillis() { return uptimeMillis; }
    public long getLamport() { return lamport; }

    @Override
    public String toString() {
        return "ResourceStatus{" +
                "cpuLoad=" + cpuLoad +
                ", freeMemory=" + freeMemory +
                ", totalMemory=" + totalMemory +
                ", uptimeMillis=" + uptimeMillis +
                ", lamport=" + lamport +
                '}';
    }
}