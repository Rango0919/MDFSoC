package com.example.mdfsoc.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class IpAddressValidatorTest {

    @Test
    public void acceptsValidIpv4() {
        assertTrue(IpAddressValidator.isValid("8.8.8.8"));
        assertTrue(IpAddressValidator.isValid("192.168.1.45"));
        assertTrue(IpAddressValidator.isValid("255.255.255.255"));
        assertTrue(IpAddressValidator.isValid("0.0.0.0"));
        assertTrue(IpAddressValidator.isValid(" 10.0.0.23  "));
    }

    @Test
    public void acceptsValidIpv6() {
        assertTrue(IpAddressValidator.isValid("::1"));
        assertTrue(IpAddressValidator.isValid("2001:db8::1"));
        assertTrue(IpAddressValidator.isValid("fe80::a00:27ff:fe0e:8a1e"));
    }

    @Test
    public void rejectsInvalidIpv4() {
        assertFalse(IpAddressValidator.isValid("999.1.1.1"));
        assertFalse(IpAddressValidator.isValid("256.255.255.255"));
        assertFalse(IpAddressValidator.isValid("1.2.3"));
        assertFalse(IpAddressValidator.isValid("1.2.3.4.5"));
        assertFalse(IpAddressValidator.isValid("192.168.1.a"));
    }

    @Test
    public void rejectsInvalidIpv6() {
        assertFalse(IpAddressValidator.isValid("gggg::1"));
        assertFalse(IpAddressValidator.isValid("2001:db8::zzzz"));
        assertFalse(IpAddressValidator.isValid("::1::2::3"));
    }

    @Test
    public void rejectsNullAndEmpty() {
        assertFalse(IpAddressValidator.isValid(null));
        assertFalse(IpAddressValidator.isValid(""));
        assertFalse(IpAddressValidator.isValid("   "));
        assertFalse(IpAddressValidator.isValid("not-an-ip"));
    }
}