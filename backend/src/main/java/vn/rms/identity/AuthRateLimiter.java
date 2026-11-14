package vn.rms.identity;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.stereotype.Component;

@Component
public class AuthRateLimiter {
    private static final int MAX_FAILURES = 10;
    private static final long WINDOW_SECONDS = 60;
    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<Instant>> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        ConcurrentLinkedDeque<Instant> queue = attempts.get(key);
        if (queue == null) return false;
        prune(queue, Instant.now());
        return queue.size() >= MAX_FAILURES;
    }

    public void recordFailure(String key) {
        Instant now = Instant.now();
        ConcurrentLinkedDeque<Instant> queue = attempts.computeIfAbsent(key, ignored -> new ConcurrentLinkedDeque<>());
        prune(queue, now);
        queue.addLast(now);
    }

    public void clear(String key) {
        attempts.remove(key);
    }

    private void prune(ConcurrentLinkedDeque<Instant> queue, Instant now) {
        Instant cutoff = now.minusSeconds(WINDOW_SECONDS);
        while (true) {
            Instant head = queue.peekFirst();
            if (head == null || !head.isBefore(cutoff)) break;
            queue.pollFirst();
        }
    }
}
