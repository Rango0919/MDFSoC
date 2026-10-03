package com.example.mdfsoc.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class PhishingTextParserTest {

    @Test
    public void deobfuscatesBracketedDotsAndHxxpScheme() {
        assertEquals("https://paypa1-secure.com/verify",
                PhishingTextParser.deobfuscate("hxxps://paypa1-secure[.]com/verify"));
        assertEquals("http://evil.example/login",
                PhishingTextParser.deobfuscate("hxxp://evil[.]example/login"));
    }

    @Test
    public void deobfuscatesParenthesisAndEntityForms() {
        assertEquals("paypal.com",
                PhishingTextParser.deobfuscate("paypal(dot)com"));
        assertEquals("paypal.com", PhishingTextParser.deobfuscate("paypal&#46;com"));
        assertEquals("http://paypal.com", PhishingTextParser.deobfuscate("http&#58;//paypal.com"));
    }

    @Test
    public void deobfuscatesSpacedDotForm() {
        assertEquals("paypa1-secure.com",
                PhishingTextParser.deobfuscate("paypa1-secure dot com"));
    }

    @Test
    public void percentDecodingKeepsPlusSign() {
        assertEquals("https://x.com/a+b?q=1",
                PhishingTextParser.deobfuscate("https://x.com/a%2Bb?q=1"));
    }

    @Test
    public void normalizeStripsTrailingPunctuation() {
        assertEquals("http://evil.example/path",
                PhishingTextParser.normalize("http://evil.example/path."));
        assertEquals("http://evil.example/path",
                PhishingTextParser.normalize("http://evil.example/path),"));
    }

    @Test
    public void normalizeAddsSchemeToBareDomain() {
        assertEquals("http://evil.example/x", PhishingTextParser.normalize("evil.example/x"));
    }

    @Test
    public void extractsObfuscatedUrlFromPlainText() {
        PhishingTextParser.Extraction result = PhishingTextParser.extract(
                "Confirm here: hxxps://paypa1-secure[.]com/verify?session=8f21ac");

        List<PhishingTextParser.FoundUrl> urls = result.getUrls();
        assertEquals(1, urls.size());
        assertEquals("https://paypa1-secure.com/verify?session=8f21ac", urls.get(0).getDecoded());
        assertTrue(urls.get(0).wasObfuscated());
        assertEquals("hxxps://paypa1-secure[.]com/verify?session=8f21ac", urls.get(0).getRaw());
    }

    @Test
    public void extractsHtmlHrefAndKeepsVisibleTextLink() {
        PhishingTextParser.Extraction result = PhishingTextParser.extract(
                "<a href=\"http://bit.ly/3xAmple\">click</a>");

        assertEquals(1, result.getUrls().size());
        assertEquals("http://bit.ly/3xAmple", result.getUrls().get(0).getDecoded());
    }

    @Test
    public void skipsMailtoHref() {
        PhishingTextParser.Extraction result = PhishingTextParser.extract(
                "<a href=\"mailto:no-reply@evil.example\">mail us</a>");

        assertTrue(result.getUrls().isEmpty());
    }

    @Test
    public void deduplicatesSameDecodedUrl() {
        PhishingTextParser.Extraction result = PhishingTextParser.extract(
                "hxxps://evil[.]example/a and https://evil.example/a");

        assertEquals(1, result.getUrls().size());
    }

    @Test
    public void extractsHeaderIpsIncludingBracketedAndBare() {
        PhishingTextParser.Extraction result = PhishingTextParser.extract(
                "Return-Path: <bounce@relay.example.org>\n"
                        + "Received: from mail.sender-net.example (unknown [45.137.22.198])\n"
                        + "\tby mx1.corp.example with ESMTPS id abc\n"
                        + "Received: from 10.20.30.40 by mx2.corp.example\n"
                        + "Subject: hello\n"
                        + "\n"
                        + "Body mentions 203.0.113.77 for no reason.\n");

        assertTrue(result.isHeadersDetected());
        assertTrue(result.getHeaderIps().contains("45.137.22.198"));
        assertTrue(result.getHeaderIps().contains("10.20.30.40"));
        assertFalse("body IP must not be treated as a sender IP",
                result.getHeaderIps().contains("203.0.113.77"));
    }

    @Test
    public void dateHeaderTimestampIsNotAnIpv6() {
        PhishingTextParser.Extraction result = PhishingTextParser.extract(
                "Received: from mail.example (unknown [45.137.22.198])\n"
                        + "\tfor <user@corp.example>; Thu, 02 Oct 2026 09:14:22 +0000\n");

        assertEquals(1, result.getHeaderIps().size());
        assertEquals("45.137.22.198", result.getHeaderIps().get(0));
    }

    @Test
    public void bracketedDotInFromHeaderIsNotAnIp() {
        PhishingTextParser.Extraction result = PhishingTextParser.extract(
                "From: \"Account Verification\" <no-reply@paypa1-secure[.]com>\n");

        assertTrue(result.getHeaderIps().isEmpty());
        assertTrue(result.getSenderDomains().contains("paypa1-secure.com"));
    }

    @Test
    public void acceptsRealIpv6InHeaders() {
        PhishingTextParser.Extraction result = PhishingTextParser.extract(
                "Received: from mail.example ([2001:db8::1])\n"
                        + "Received: from v6.example (IPv6:2001:4860:4860::8888)\n");

        assertTrue(result.getHeaderIps().contains("2001:db8::1"));
        assertTrue(result.getHeaderIps().contains("2001:4860:4860::8888"));
    }

    @Test
    public void ipv4AndIpv6ShapesAreValidated() {
        assertTrue(PhishingTextParser.looksLikeIpv4("45.137.22.198"));
        assertFalse(PhishingTextParser.looksLikeIpv4("45.137.22"));
        assertFalse(PhishingTextParser.looksLikeIpv4("999.1.1.1"));
        assertFalse(PhishingTextParser.looksLikeIpv4("1.2.3.4.5"));

        assertTrue(PhishingTextParser.looksLikeIpv6("2001:db8::1"));
        assertTrue(PhishingTextParser.looksLikeIpv6("2001:0db8:0000:0000:0000:0000:0000:0001"));
        assertTrue(PhishingTextParser.looksLikeIpv6("fe80::1%eth0"));
        assertFalse(PhishingTextParser.looksLikeIpv6("09:14:22"));
        assertFalse(PhishingTextParser.looksLikeIpv6("2001:db8:::1"));
        assertFalse(PhishingTextParser.looksLikeIpv6("2001:12345::1"));
        assertFalse(PhishingTextParser.looksLikeIpv6("."));
    }

    @Test
    public void doesNotCollectIpsWithoutHeaders() {
        PhishingTextParser.Extraction result = PhishingTextParser.extract(
                "Ping 203.0.113.77 now.");

        assertFalse(result.isHeadersDetected());
        assertTrue(result.getHeaderIps().isEmpty());
    }

    @Test
    public void extractsSenderDomainsFromFromHeader() {
        PhishingTextParser.Extraction result = PhishingTextParser.extract(
                "From: \"Account Verification\" <no-reply@paypa1-secure.com>\n"
                        + "Subject: Verify\n");

        assertTrue(result.getSenderDomains().contains("paypa1-secure.com"));
    }

    @Test
    public void hostOfStripsPortAndCredentials() {
        assertEquals("evil.example", PhishingTextParser.hostOf("https://user:pw@evil.example:8443/x"));
        assertEquals("evil.example", PhishingTextParser.hostOf("http://evil.example"));
    }

    @Test
    public void emptyInputYieldsEmptyExtraction() {
        assertTrue(PhishingTextParser.extract(null).isEmpty());
        assertTrue(PhishingTextParser.extract("   ").isEmpty());
    }

    @Test
    public void benignSenderDomainIsNotFlagged() {
        PhishingTextParser.Extraction result = PhishingTextParser.extract(
                "From: IT Helpdesk <helpdesk@corp.example.com>\n");

        assertTrue(result.isEmpty());
    }
}