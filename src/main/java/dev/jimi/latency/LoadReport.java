package dev.jimi.latency;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public record LoadReport(
        String url,
        String method,
        int requested,
        int concurrency,
        double requestsPerSecond,
        int completed,
        int successful,
        int failed,
        double elapsedSeconds,
        double throughputPerSecond,
        Instant startedAt,
        Instant finishedAt,
        LatencyStats latency,
        Map<Integer, Integer> statusCounts,
        Map<String, Integer> errorCounts) {

    public static LoadReport from(LoadConfig config, List<RequestResult> results, Instant startedAt,
            Instant finishedAt, long elapsedNanos) {
        Map<Integer, Integer> statuses = new TreeMap<>();
        Map<String, Integer> errors = new LinkedHashMap<>();
        results.forEach(result -> {
            if (result.statusCode() > 0) {
                statuses.merge(result.statusCode(), 1, Integer::sum);
            }
            if (result.error() != null) {
                errors.merge(result.error(), 1, Integer::sum);
            }
        });
        int successful = (int) results.stream().filter(RequestResult::successful).count();
        double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
        return new LoadReport(config.uri().toString(), config.method(), config.requests(), config.concurrency(),
                config.requestsPerSecond(), results.size(), successful, results.size() - successful, elapsedSeconds,
                elapsedSeconds == 0 ? 0 : results.size() / elapsedSeconds, startedAt, finishedAt,
                LatencyStats.from(results), statuses, errors);
    }
}
