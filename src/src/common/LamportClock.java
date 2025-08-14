package common;

import java.util.concurrent.atomic.AtomicLong;

public class LamportClock {
    private final AtomicLong time = new AtomicLong(0);

    public long tick() {
        return time.incrementAndGet();
    }

    public long updateOnReceive(long received) {
        long current;
        long next;
        do {
            current = time.get();
            next = Math.max(current, received) + 1;
        } while (!time.compareAndSet(current, next));
        return next;
    }

    public long get() {
        return time.get();
    }
}