package dev.jimi.latency;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoadTestRunnerTest {

    private static final HttpServer SERVER = startServer();

    @AfterAll
    static void stopServer() {
        SERVER.stop(0);
    }

    @Test
    void respectsRateLimitAndCollectsSuccessfulResults() {
        LoadConfig config = new LoadConfig(URI.create("http://127.0.0.1:" + SERVER.getAddress().getPort() + "/test"),
                "GET", null, 12, 4, 10, Duration.ofSeconds(2));
        LoadTestRunner.RunResult result = new LoadTestRunner().run(config);

        assertEquals(12, result.report().completed());
        assertEquals(12, result.report().successful());
        assertEquals(0, result.report().failed());
        assertEquals(12, result.report().statusCounts().get(200));
        assertTrue(result.report().elapsedSeconds() >= 1.0);
        assertTrue(result.report().throughputPerSecond() > 0);
        assertTrue(result.report().latency().maxMs() <= result.report().elapsedSeconds() * 1000 + 100);
    }

    private static HttpServer startServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/test", exchange -> {
                byte[] body = "ok".getBytes();
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return server;
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
