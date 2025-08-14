package monitoring.leader;

import monitoring.common.Snapshot;

import java.util.*;

public class SnapshotRepository {
    private final Map<Long, List<Snapshot>> snapshotsByClock = new HashMap<>();

    public synchronized void store(long clockVal, List<Snapshot> snapshots) {
        snapshotsByClock.put(clockVal, new ArrayList<>(snapshots));
    }

    public synchronized List<Snapshot> getByClock(long clockVal) {
        return snapshotsByClock.getOrDefault(clockVal, Collections.emptyList());
    }

    public synchronized void printAll() {
        for (var entry : snapshotsByClock.entrySet()) {
            System.out.println("Clock " + entry.getKey() + ": " + entry.getValue());
        }
    }
}