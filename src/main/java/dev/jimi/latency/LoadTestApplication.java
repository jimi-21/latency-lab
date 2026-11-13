package dev.jimi.latency;

import java.net.URI;
import java.time.Duration;
import java.nio.file.Path;

public class LoadTestApplication {

    public static void main(String[] args) {
        System.exit(new LoadTestApplication().run(args));
    }

    public int run(String[] args) {
        if (args.length == 1 && (args[0].equals("--help") || args[0].equals("-h"))) {
            System.out.println(usage());
            return 0;
        }
        try {
            CliOptions options = CliOptions.parse(args);
            if (options.url == null) {
                throw new IllegalArgumentException("--url is required");
            }
            LoadConfig config = new LoadConfig(URI.create(options.url), options.method, options.body,
                    options.requests, options.concurrency, options.rate, options.timeout);
            LoadTestRunner.RunResult result = new LoadTestRunner().run(config);
            ReportWriter writer = new ReportWriter();
            System.out.print(writer.console(result.report()));
            if (options.json != null) {
                writer.writeJson(result.report(), options.json);
            }
            if (options.csv != null) {
                writer.writeCsv(result.results(), options.csv);
            }
            if (options.html != null) {
                writer.writeHtml(result.report(), options.html);
            }
            return result.report().failed() == 0 ? 0 : 1;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            System.err.println("latency-lab: " + exception.getMessage());
            System.err.println(usage());
            return 2;
        }
    }

    public String usage() {
        return "Usage: java -jar latency-lab.jar --url <url> [--method GET] [--requests 100] "
                + "[--concurrency 10] [--rate 20] [--timeout 2s] [--body text] [--json out.json] [--csv out.csv] [--html out.html]";
    }

    static final class CliOptions {
        String url;
        String method = "GET";
        String body;
        int requests = 100;
        int concurrency = 10;
        double rate;
        Duration timeout = Duration.ofSeconds(2);
        Path json;
        Path csv;
        Path html;

        static CliOptions parse(String[] args) {
            CliOptions options = new CliOptions();
            for (int i = 0; i < args.length; i++) {
                String key = args[i];
                if (++i >= args.length) {
                    throw new IllegalArgumentException("Missing value after " + key);
                }
                String value = args[i];
                switch (key) {
                    case "--url" -> options.url = value;
                    case "--method" -> options.method = value;
                    case "--body" -> options.body = value;
                    case "--requests", "-n" -> options.requests = parsePositiveInt(value, key);
                    case "--concurrency", "-c" -> options.concurrency = parsePositiveInt(value, key);
                    case "--rate" -> options.rate = Double.parseDouble(value);
                    case "--timeout" -> options.timeout = parseDuration(value);
                    case "--json" -> options.json = Path.of(value);
                    case "--csv" -> options.csv = Path.of(value);
                    case "--html" -> options.html = Path.of(value);
                    default -> throw new IllegalArgumentException("Unknown option: " + key);
                }
            }
            return options;
        }

        private static int parsePositiveInt(String value, String key) {
            int parsed = Integer.parseInt(value);
            if (parsed < 1) {
                throw new IllegalArgumentException(key + " must be positive");
            }
            return parsed;
        }

        private static Duration parseDuration(String value) {
            if (value.endsWith("ms")) {
                return Duration.ofMillis(Long.parseLong(value.substring(0, value.length() - 2)));
            }
            if (value.endsWith("s")) {
                return Duration.ofSeconds(Long.parseLong(value.substring(0, value.length() - 1)));
            }
            if (value.endsWith("m")) {
                return Duration.ofMinutes(Long.parseLong(value.substring(0, value.length() - 1)));
            }
            throw new IllegalArgumentException("Duration must use ms, s, or m: " + value);
        }
    }
}
