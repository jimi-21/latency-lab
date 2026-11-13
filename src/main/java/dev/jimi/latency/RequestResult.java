package dev.jimi.latency;

public record RequestResult(
        int requestId,
        int statusCode,
        long latencyMicros,
        long responseBytes,
        String error) {

    public boolean successful() {
        return error == null && statusCode >= 200 && statusCode < 300;
    }

    public double latencyMillis() {
        return latencyMicros / 1000.0;
    }
}
