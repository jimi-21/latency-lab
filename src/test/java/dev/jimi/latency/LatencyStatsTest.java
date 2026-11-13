package dev.jimi.latency;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LatencyStatsTest {

    @Test
    void calculatesNearestRankPercentiles() {
        List<RequestResult> results = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            results.add(new RequestResult(i, 200, i * 1000L, 0, null));
        }
        LatencyStats stats = LatencyStats.from(results);
        assertEquals(1.0, stats.minMs());
        assertEquals(50.5, stats.meanMs());
        assertEquals(50.0, stats.p50Ms());
        assertEquals(90.0, stats.p90Ms());
        assertEquals(95.0, stats.p95Ms());
        assertEquals(99.0, stats.p99Ms());
        assertEquals(100.0, stats.maxMs());
    }

    @Test
    void validatesConfiguration() {
        assertThrows(IllegalArgumentException.class,
                () -> new LoadConfig(java.net.URI.create("file:///tmp/test"), "GET", null, 1, 1, 0,
                        java.time.Duration.ofSeconds(1)));
    }
}
