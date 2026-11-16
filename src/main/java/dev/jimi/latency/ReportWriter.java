package dev.jimi.latency;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportWriter {

    public String console(LoadReport report) {
        return String.format(Locale.ROOT, """
                Latency Lab · HTTP load test
                ============================
                %s %s
                completed: %d | success: %d | failed: %d
                elapsed: %.3fs | throughput: %.2f req/s
                latency ms: mean %.2f | p50 %.2f | p95 %.2f | p99 %.2f | max %.2f
                """, report.method(), report.url(), report.completed(), report.successful(), report.failed(),
                report.elapsedSeconds(), report.throughputPerSecond(), report.latency().meanMs(),
                report.latency().p50Ms(), report.latency().p95Ms(), report.latency().p99Ms(), report.latency().maxMs());
    }

    public void writeJson(LoadReport report, Path path) {
        StringBuilder json = new StringBuilder("{\n");
        json.append(field("url", report.url(), true)).append(",\n")
                .append(field("method", report.method(), true)).append(",\n")
                .append(field("completed", Integer.toString(report.completed()), false)).append(",\n")
                .append(field("successful", Integer.toString(report.successful()), false)).append(",\n")
                .append(field("failed", Integer.toString(report.failed()), false)).append(",\n")
                .append(field("throughputPerSecond", Double.toString(report.throughputPerSecond()), false)).append(",\n")
                .append(field("meanMs", Double.toString(report.latency().meanMs()), false)).append(",\n")
                .append(field("p50Ms", Double.toString(report.latency().p50Ms()), false)).append(",\n")
                .append(field("p95Ms", Double.toString(report.latency().p95Ms()), false)).append(",\n")
                .append(field("p99Ms", Double.toString(report.latency().p99Ms()), false)).append(",\n")
                .append(field("maxMs", Double.toString(report.latency().maxMs()), false)).append(",\n")
                .append("  \"statusCounts\": {");
        appendMap(json, report.statusCounts());
        json.append("},\n  \"errorCounts\": {");
        appendMap(json, report.errorCounts());
        json.append("}\n}\n");
        write(path, json.toString());
    }

    private void appendMap(StringBuilder json, Map<?, Integer> map) {
        boolean first = true;
        for (var entry : map.entrySet()) {
            if (!first) {
                json.append(", ");
            }
            json.append("\"").append(escape(entry.getKey().toString())).append("\": ").append(entry.getValue());
            first = false;
        }
    }

    private String field(String name, String value, boolean quoted) {
        return "  \"" + name + "\": " + (quoted ? "\"" + escape(value) + "\"" : value);
    }

    public void writeCsv(List<RequestResult> results, Path path) {
        StringBuilder csv = new StringBuilder("requestId,statusCode,latencyMs,responseBytes,error\n");
        results.forEach(result -> csv.append(result.requestId()).append(',')
                .append(result.statusCode()).append(',')
                .append(String.format(Locale.ROOT, "%.3f", result.latencyMillis())).append(',')
                .append(result.responseBytes()).append(',')
                .append(result.error() == null ? "" : "\"" + result.error().replace("\"", "\"\"") + "\"").append('\n'));
        write(path, csv.toString());
    }

    public void writeHtml(LoadReport report, Path path) {
        double successRate = report.completed() == 0 ? 0 : 100.0 * report.successful() / report.completed();
        StringBuilder html = new StringBuilder("""
                <!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Latency Lab Report</title>
                <style>
                :root{color-scheme:dark;--bg:#07131f;--panel:#0f2030;--line:#244054;--text:#e9f2f7;--muted:#8fa8ba;--teal:#38d7bd;--red:#ff7e89}*{box-sizing:border-box}body{margin:0;background:radial-gradient(circle at 90% 0,#153d4c,transparent 38%),var(--bg);color:var(--text);font-family:Inter,Segoe UI,sans-serif}.wrap{max-width:1180px;margin:auto;padding:40px 24px}.eyebrow{color:var(--teal);font-size:12px;font-weight:800;letter-spacing:2px}h1{margin:10px 0;font-size:36px}.sub,.note{color:var(--muted);line-height:1.7;word-break:break-all}.cards{display:grid;grid-template-columns:repeat(4,1fr);gap:14px;margin:28px 0}.card,.panel{background:#0f2030ee;border:1px solid var(--line);border-radius:16px}.card{padding:20px}.label{color:var(--muted);font-size:12px}.value{font-size:28px;font-weight:750;margin-top:8px}.good{color:var(--teal)}.bad{color:var(--red)}.panel{overflow:hidden;margin-bottom:20px}.panel h2{padding:20px;margin:0;font-size:17px;border-bottom:1px solid var(--line)}table{width:100%;border-collapse:collapse}th,td{text-align:left;padding:14px 20px;border-bottom:1px solid #1d3345}th{color:var(--muted);font-size:11px}td{font-family:Consolas,monospace;font-size:13px}.bar-row{display:grid;grid-template-columns:90px 1fr 60px;gap:12px;align-items:center;padding:12px 20px}.bar{height:12px;background:#07131f;border-radius:10px;overflow:hidden}.fill{height:100%;background:linear-gradient(90deg,var(--teal),#68a7ff)}.footer{margin-top:22px;font-size:12px;color:var(--muted)}@media(max-width:720px){.cards{grid-template-columns:repeat(2,1fr)}.panel{overflow-x:auto}h1{font-size:28px}}
                </style></head><body><main class="wrap"><div class="eyebrow">LATENCY LAB / HTTP LOAD TEST</div><h1>Performance report</h1><div class="sub">
                """);
        html.append(escape(report.method())).append(' ').append(escape(report.url())).append("</div><section class=\"cards\">");
        html.append(card("SUCCESS RATE", String.format(Locale.ROOT, "%.1f%%", successRate), successRate == 100 ? "good" : "bad"))
                .append(card("THROUGHPUT", String.format(Locale.ROOT, "%.2f/s", report.throughputPerSecond()), ""))
                .append(card("P95 LATENCY", String.format(Locale.ROOT, "%.2f ms", report.latency().p95Ms()), ""))
                .append(card("FAILED", Integer.toString(report.failed()), report.failed() == 0 ? "good" : "bad"));
        html.append("</section><section class=\"panel\"><h2>Latency percentiles</h2><table><tbody>");
        String[][] metrics = {{"MIN", Double.toString(report.latency().minMs())}, {"MEAN", Double.toString(report.latency().meanMs())},
                {"P50", Double.toString(report.latency().p50Ms())}, {"P90", Double.toString(report.latency().p90Ms())},
                {"P95", Double.toString(report.latency().p95Ms())}, {"P99", Double.toString(report.latency().p99Ms())},
                {"MAX", Double.toString(report.latency().maxMs())}};
        for (String[] metric : metrics) {
            html.append("<tr><th>").append(metric[0]).append("</th><td>").append(metric[1]).append(" ms</td></tr>");
        }
        html.append("</tbody></table></section><section class=\"panel\"><h2>Status codes</h2>");
        int maxCount = report.statusCounts().values().stream().mapToInt(Integer::intValue).max().orElse(1);
        for (var entry : report.statusCounts().entrySet()) {
            int width = (int) (100.0 * entry.getValue() / maxCount);
            html.append("<div class=\"bar-row\"><span>").append(entry.getKey()).append("</span><div class=\"bar\"><div class=\"fill\" style=\"width:")
                    .append(width).append("%\"></div></div><span>").append(entry.getValue()).append("</span></div>");
        }
        html.append("</section><section class=\"panel\"><h2>Run configuration</h2><table><tbody>")
                .append("<tr><th>REQUESTED</th><td>").append(report.requested()).append("</td></tr>")
                .append("<tr><th>CONCURRENCY</th><td>").append(report.concurrency()).append("</td></tr>")
                .append("<tr><th>RATE LIMIT</th><td>").append(report.requestsPerSecond()).append(" req/s</td></tr>")
                .append("<tr><th>ELAPSED</th><td>").append(String.format(Locale.ROOT, "%.3f", report.elapsedSeconds())).append(" s</td></tr>")
                .append("<tr><th>STARTED</th><td>").append(report.startedAt()).append("</td></tr>")
                .append("<tr><th>FINISHED</th><td>").append(report.finishedAt()).append("</td></tr>")
                .append("</tbody></table></section><div class=\"footer\">Generated by latency-lab 1.0.0. Run load tests only against services you own or are authorized to test.</div></main></body></html>");
        write(path, html.toString());
    }

    private String card(String label, String value, String cssClass) {
        return "<div class=\"card\"><div class=\"label\">" + label + "</div><div class=\"value " + cssClass + "\">"
                + escape(value) + "</div></div>";
    }

    private String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private void write(Path path, String content) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, content);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Could not write report: " + path, exception);
        }
    }
}
