package monitoring.node;

import monitoring.common.LamportClock;
import monitoring.common.ResourceStatus;
import monitoring.rmi.GetStatusService;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.rmi.RemoteException;
import java.time.Duration;

public class GetStatusServiceImpl implements GetStatusService {
    private final LamportClock clock;
    private final long startNanos = System.nanoTime();

    public GetStatusServiceImpl(LamportClock clock) {
        this.clock = clock;
    }

    @Override
    public ResourceStatus getStatus() throws RemoteException {
        long lamport = clock.tick();
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        double load = 0.0;

        try {
            Class<?> sunOsClazz = Class.forName("com.sun.management.OperatingSystemMXBean");
            if (sunOsClazz.isInstance(os)) {
                Object sunOs = sunOsClazz.cast(os);
                double processCpuLoad = (double) sunOsClazz.getMethod("getSystemCpuLoad").invoke(sunOs);
                if (!Double.isNaN(processCpuLoad) && processCpuLoad >= 0) {
                    load = processCpuLoad;
                }
            }
        } catch (Exception ignored) {}

        long free = Runtime.getRuntime().freeMemory();
        long total = Runtime.getRuntime().totalMemory();
        long uptimeMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
        return new ResourceStatus(load, free, total, uptimeMs, lamport);
    }
}