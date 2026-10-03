package com.example.mdfsoc.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;

import java.lang.reflect.Type;

/**
 * Guards the Android <-> PHP contract: parses real backend envelope JSON so any
 * model-field or envelope mismatch fails CI immediately.
 */
public class ApiEnvelopeParsingTest {

    private static final Gson GSON = new Gson();
    private static final Type DASHBOARD_TYPE = new TypeToken<ApiEnvelope<DashboardData>>() {}.getType();
    private static final Type ALERTS_TYPE = new TypeToken<ApiEnvelope<AlertsPage>>() {}.getType();
    private static final Type ALERT_TYPE = new TypeToken<ApiEnvelope<Alert>>() {}.getType();
    private static final Type LOGIN_TYPE = new TypeToken<ApiEnvelope<LoginResponse>>() {}.getType();

    @Test
    public void parsesDashboardEnvelope() {
        ApiEnvelope<DashboardData> envelope = GSON.fromJson(
                "{\"success\":true,\"data\":{\"total_alerts\":247,\"critical_alerts\":12,"
                        + "\"high_alerts\":38,\"medium_alerts\":91,\"low_alerts\":106,"
                        + "\"recent_alerts\":[{\"id\":125,\"severity\":\"critical\","
                        + "\"title\":\"Brute Force Attack\",\"source_ip\":\"192.168.1.45\","
                        + "\"timestamp\":\"2026-09-18 09:15:22\",\"status\":\"active\"}]},"
                        + "\"warnings\":[],\"error\":null}",
                DASHBOARD_TYPE);

        assertTrue(envelope.isSuccess());
        assertNull(envelope.getError());
        DashboardData data = envelope.getData();
        assertNotNull(data);
        assertEquals(247, data.getTotalAlerts());
        assertEquals(12, data.getCriticalAlerts());
        assertEquals(91, data.getMediumAlerts());
        assertEquals(1, data.getRecentAlerts().size());
        RecentAlert recent = data.getRecentAlerts().get(0);
        assertEquals(125, recent.getId());
        assertEquals("critical", recent.getSeverity());
        assertEquals("192.168.1.45", recent.getSourceIp());
    }

    @Test
    public void parsesAlertsPageWithMitre() {
        ApiEnvelope<AlertsPage> envelope = GSON.fromJson(
                "{\"success\":true,\"data\":{\"alerts\":[{\"id\":122,\"severity\":\"critical\","
                        + "\"title\":\"Malware Detected\",\"source_ip\":\"192.168.1.100\","
                        + "\"dest_ip\":\"10.0.0.1\",\"timestamp\":\"2026-09-18 07:45:03\","
                        + "\"description\":\"Signature match against known malware family.\","
                        + "\"status\":\"active\",\"rule_id\":\"87103\",\"rule_level\":15,"
                        + "\"agent\":\"agent-03\",\"investigation_status\":\"active\","
                        + "\"mitre\":{\"technique_id\":\"T1204\",\"technique_name\":\"User Execution\","
                        + "\"tactic\":\"Execution\",\"link\":\"https://attack.mitre.org/techniques/T1204/\"}}],"
                        + "\"total\":1,\"page\":0,\"limit\":10},\"warnings\":[],\"error\":null}",
                ALERTS_TYPE);

        assertTrue(envelope.isSuccess());
        Alert alert = envelope.getData().getAlerts().get(0);
        assertEquals(122, alert.getId());
        assertEquals(15, alert.getRuleLevel());
        MitreInfo mitre = alert.getMitre();
        assertNotNull(mitre);
        assertEquals("T1204", mitre.getTechniqueId());
        assertEquals("T1204 \u2014 User Execution", alert.getMitreTechnique());
    }

    @Test
    public void parsesAlertDetailEnvelope() {
        ApiEnvelope<Alert> envelope = GSON.fromJson(
                "{\"success\":true,\"data\":{\"id\":124,\"severity\":\"high\","
                        + "\"title\":\"SSH New User Connecting\",\"source_ip\":\"10.0.0.23\","
                        + "\"dest_ip\":\"10.0.0.1\",\"timestamp\":\"2026-09-18 08:58:10\","
                        + "\"description\":\"A new user has successfully connected over SSH.\","
                        + "\"status\":\"active\",\"rule_id\":\"5753\",\"rule_level\":7,"
                        + "\"agent\":\"agent-01\",\"investigation_status\":\"active\","
                        + "\"mitre\":null},\"warnings\":[],\"error\":null}",
                ALERT_TYPE);

        assertTrue(envelope.isSuccess());
        Alert alert = envelope.getData();
        assertEquals("high", alert.getSeverity());
        assertNull(alert.getMitre());
        assertNull(alert.getMitreTechnique());
    }

    @Test
    public void parsesLoginEnvelope() {
        ApiEnvelope<LoginResponse> envelope = GSON.fromJson(
                "{\"success\":true,\"data\":{\"token\":\"eyJU.gzsig\","
                        + "\"expires_at\":\"2026-09-25T12:11:24+00:00\"},"
                        + "\"warnings\":[],\"error\":null}",
                LOGIN_TYPE);

        assertTrue(envelope.isSuccess());
        assertEquals("eyJU.gzsig", envelope.getData().getToken());
        assertEquals("2026-09-25T12:11:24+00:00", envelope.getData().getExpiresAt());
    }

    @Test
    public void parsesErrorEnvelope() {
        ApiEnvelope<DashboardData> envelope = GSON.fromJson(
                "{\"success\":false,\"data\":null,\"warnings\":[],"
                        + "\"error\":{\"code\":\"auth_error\",\"message\":\"Invalid or expired access token\","
                        + "\"source\":\"backend\"}}",
                DASHBOARD_TYPE);

        assertFalse(envelope.isSuccess());
        assertNull(envelope.getData());
        assertNotNull(envelope.getError());
        assertEquals("auth_error", envelope.getError().getCode());
        assertEquals("Invalid or expired access token", envelope.getError().getMessage());
        assertEquals("backend", envelope.getError().getSource());
    }
}