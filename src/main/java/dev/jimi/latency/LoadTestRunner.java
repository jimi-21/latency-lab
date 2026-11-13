package dev.jimi.latency;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class LoadTestRunner {

    public RunResult run(LoadConfig config) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(config.timeout()).build();
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(config.concurrency());
        ConcurrentLinkedQueue<RequestResult> results = new ConcurrentLinkedQueue<>();
        Instant startedAt = Instant.now();
        long startedNanos = System.nanoTime();
        try {
            for (int requestId = 1; requestId <= config.requests(); requestId++) {
                int currentRequestId = requestId;
                Runnable task = () -> results.add(execute(client, config, currentRequestId));
                if (config.requestsPerSecond() > 0) {
                    long delay = (long) ((requestId - 1) * 1_000_000_000.0 / config.requestsPerSecond());
                    executor.schedule(task, delay, TimeUnit.NANOSECONDS);
                } else {
                    executor.submit(task);
                }
            }
            executor.shutdown();
            if (!executor.awaitTermination(24, TimeUnit.HOURS)) {
                throw new IllegalStateException("Load test did not finish within 24 hours");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
            throw new IllegalStateException("Load test interrupted", exception);
        } finally {
            executor.shutdown();
        }
        Instant finishedAt = Instant.now();
        long elapsed = System.nanoTime() - startedNanos;
        List<RequestResult> ordered = new ArrayList<>(results);
        ordered.sort(Comparator.comparingInt(RequestResult::requestId));
        return new RunResult(LoadReport.from(config, ordered, startedAt, finishedAt, elapsed), List.copyOf(ordered));
    }

    private RequestResult execute(HttpClient client, LoadConfig config, int requestId) {
        long started = System.nanoTime();
        try {
            HttpRequest.BodyPublisher publisher = config.body() == null
                    ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(config.body());
            HttpRequest request = HttpRequest.newBuilder(config.uri())
                    .timeout(config.timeout())
                    .header("User-Agent", "latency-lab/1.0")
                    .header("X-Load-Request-Id", Integer.toString(requestId))
                    .method(config.method(), publisher)
                    .build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            return new RequestResult(requestId, response.statusCode(), (System.nanoTime() - started) / 1000,
                    response.body().length, null);
        } catch (Exception exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return new RequestResult(requestId, 0, (System.nanoTime() - started) / 1000, 0,
                    exception.getClass().getSimpleName());
        }
    }

    public record RunResult(LoadReport report, List<RequestResult> results) {
    }
}
