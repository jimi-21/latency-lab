package dev.jimi.latency;

import java.net.URI;
import java.time.Duration;

public record LoadConfig(
        URI uri,
        String method,
        String body,
        int requests,
        int concurrency,
        double requestsPerSecond,
        Duration timeout) {

    public LoadConfig {
        if (uri == null || !uri.isAbsolute() || uri.getScheme() == null
                || !(uri.getScheme().equals("http") || uri.getScheme().equals("https"))) {
            throw new IllegalArgumentException("A valid absolute HTTP(S) URL is required");
        }
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("HTTP method is required");
        }
        method = method.toUpperCase();
        if (requests < 1 || requests > 1_000_000) {
            throw new IllegalArgumentException("requests must be between 1 and 1000000");
        }
        if (concurrency < 1 || concurrency > 10_000) {
            throw new IllegalArgumentException("concurrency must be between 1 and 10000");
        }
        if (requestsPerSecond < 0 || Double.isNaN(requestsPerSecond) || Double.isInfinite(requestsPerSecond)) {
            throw new IllegalArgumentException("rate must be zero or a positive finite number");
        }
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
    }
}
