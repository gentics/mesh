package io.gentics.mesh.mcp.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricsTest {

    @Test
    void renders_promFormatWithCounters() {
        Metrics m = new Metrics();
        m.incrementHttpRequests();
        m.incrementHttpRequests();
        m.incrementHttpAuthFailed();
        m.recordToolCall("mesh_get_node", 123L, false);
        m.recordToolCall("mesh_get_node", 42L, true);

        String txt = m.renderPrometheus();
        assertTrue(txt.contains("mcp_uptime_seconds"), "must expose uptime");
        assertTrue(txt.contains("mcp_http_requests_total 2"), "must count HTTP requests");
        assertTrue(txt.contains("mcp_http_auth_failed_total 1"), "must count auth failures");
        assertTrue(txt.contains("mcp_tool_calls_total{tool=\"mesh_get_node\"} 2"),
                "must count tool calls per tool");
        assertTrue(txt.contains("mcp_tool_errors_total{tool=\"mesh_get_node\"} 1"),
                "must count tool errors per tool");
        assertTrue(txt.contains("mcp_tool_duration_ms_total{tool=\"mesh_get_node\"} 165"),
                "must sum durations per tool");
    }
}
