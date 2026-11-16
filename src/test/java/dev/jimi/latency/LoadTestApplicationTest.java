package dev.jimi.latency;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class LoadTestApplicationTest {

    @TempDir
    Path tempDir;

    @Test
    void writesJsonCsvAndHtmlReportsFromRealHttpServer() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/test", exchange -> {
            byte[] body = "ok".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            Path json = tempDir.resolve("report.json");
            Path csv = tempDir.resolve("report.csv");
            Path html = tempDir.resolve("report.html");
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/test";
            int exitCode = new LoadTestApplication().run(new String[]{"--url", url, "--requests", "10",
                    "--concurrency", "3", "--json", json.toString(), "--csv", csv.toString(), "--html", html.toString()});
            assertEquals(0, exitCode);
            var report = new ObjectMapper()
                    .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readTree(Files.readString(json));
            assertEquals(10, report.get("successful").asInt());
            assertEquals(10, report.get("statusCounts").get("200").asInt());
            assertEquals(11, Files.readAllLines(csv).size());
            assertTrue(Files.readString(html).contains("Performance report"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsInvalidArguments() {
        assertEquals(2, new LoadTestApplication().run(new String[]{"--requests", "10"}));
        assertEquals(0, new LoadTestApplication().run(new String[]{"--help"}));
    }
}
