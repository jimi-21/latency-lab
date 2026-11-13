package dev.jimi.latency;

import java.util.List;

public record LatencyStats(double minMs, double meanMs, double p50Ms, double p90Ms, double p95Ms, double p99Ms,
        double maxMs) {

    public static LatencyStats from(List<RequestResult> results) {
        if (results.isEmpty()) {
            return new LatencyStats(0, 0, 0, 0, 0, 0, 0);
        }
        List<Double> sorted = results.stream().map(RequestResult::latencyMillis).sorted().toList();
        double mean = sorted.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        return new LatencyStats(sorted.get(0), mean, percentile(sorted, 50), percentile(sorted, 90),
                percentile(sorted, 95), percentile(sorted, 99), sorted.get(sorted.size() - 1));
    }

    static double percentile(List<Double> sorted, double percentile) {
        if (sorted.isEmpty()) {
            return 0;
        }
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }
}
