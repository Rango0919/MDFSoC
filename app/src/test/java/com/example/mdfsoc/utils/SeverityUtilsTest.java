package com.example.mdfsoc.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SeverityUtilsTest {

    @Test
    public void mapsWellKnownSeverities() {
        assertEquals("Critical", SeverityUtils.labelFor("critical"));
        assertEquals("Critical", SeverityUtils.labelFor("crit"));
        assertEquals("High", SeverityUtils.labelFor("high"));
        assertEquals("Medium", SeverityUtils.labelFor("medium"));
        assertEquals("Medium", SeverityUtils.labelFor("med"));
        assertEquals("Low", SeverityUtils.labelFor("low"));
        assertEquals("Info", SeverityUtils.labelFor("info"));
    }

    @Test
    public void isCaseInsensitive() {
        assertEquals("High", SeverityUtils.labelFor("HIGH"));
        assertEquals("Medium", SeverityUtils.labelFor("Medium"));
    }

    @Test
    public void fallsBackToLowForNull() {
        assertEquals("Low", SeverityUtils.labelFor(null));
        assertEquals("Low", SeverityUtils.labelFor(""));
    }

    @Test
    public void passesThroughUnknownValues() {
        assertEquals("critical_alerts", SeverityUtils.labelFor("critical_alerts"));
        assertEquals("weird", SeverityUtils.labelFor("weird"));
    }
}