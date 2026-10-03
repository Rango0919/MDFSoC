package com.example.mdfsoc.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PhishingTextParser {

    private static final Pattern HREF = Pattern.compile(
            "href\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

    private static final Pattern URL_LIKE = Pattern.compile(
            "(?:https?|hxxps?)://[^\\s<>\"']+", Pattern.CASE_INSENSITIVE);

    private static final Pattern SCHEME_LESS = Pattern.compile(
            "(?<![\\w.@/-])(?:www\\.)[\\w-]+(?:\\.[\\w-]+)+(?:[:/][^\\s<>\"']*)?");

    private static final Pattern IPV4 = Pattern.compile(
            "(?<![\\d.])(?:(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}"
                    + "(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(?![\\d.])");

    private static final Pattern HEX_GROUP = Pattern.compile("^[0-9A-Fa-f]{1,4}$");

    private static final Pattern IP_LITERAL = Pattern.compile(
            "\\[[ \\t]*(?:IPv6?:)?([0-9A-Fa-f:.%]+)]");

    private static final Pattern IPV6_TOKEN = Pattern.compile(
            "(?<![0-9A-Fa-f.])(?:[0-9A-Fa-f]{0,4}:){2,7}[0-9A-Fa-f]{0,4}(?![:0-9A-Fa-f])");

    private static final Pattern HEADER_START = Pattern.compile(
            "^[ \\t]*(received|return-path|received-spf|authentication-results|"
                    + "x-originating-ip|x-originating-server|x-mailer-ip|client-ip|"
                    + "x-client-ip|reply-to|from|sender|posted-by|mail-server)[ \\t]*:",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern FROM_ADDRESS = Pattern.compile(
            "[\\w.%+-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+", Pattern.CASE_INSENSITIVE);

    private static final Set<String> SKIP_HOSTS = new LinkedHashSet<>(Arrays.asList(
            "example.com", "example.org", "example.net",
            "w3.org", "schema.org", "microsoft.com", "google.com", "gmail.com"));

    private PhishingTextParser() {
    }

    public static final class FoundUrl {
        private final String raw;
        private final String decoded;

        FoundUrl(String raw, String decoded) {
            this.raw = raw;
            this.decoded = decoded;
        }

        public String getRaw() {
            return raw;
        }

        public String getDecoded() {
            return decoded;
        }

        public boolean wasObfuscated() {
            return !raw.equals(decoded);
        }
    }

    public static final class Extraction {
        private final List<FoundUrl> urls = new ArrayList<>();
        private final List<String> headerIps = new ArrayList<>();
        private final List<String> senderDomains = new ArrayList<>();
        private boolean headersDetected;

        public List<FoundUrl> getUrls() {
            return urls;
        }

        public List<String> getHeaderIps() {
            return headerIps;
        }

        public List<String> getSenderDomains() {
            return senderDomains;
        }

        public boolean isHeadersDetected() {
            return headersDetected;
        }

        public boolean isEmpty() {
            return urls.isEmpty() && headerIps.isEmpty() && senderDomains.isEmpty();
        }
    }

    public static Extraction extract(String rawText) {
        Extraction result = new Extraction();
        if (rawText == null || rawText.trim().isEmpty()) {
            return result;
        }
        String text = rawText.replace("\r\n", "\n").replace('\r', '\n');

        Map<String, String> candidates = new LinkedHashMap<>();
        collectHrefs(text, candidates);
        collectInlineUrls(text, candidates);
        for (Map.Entry<String, String> entry : candidates.entrySet()) {
            result.urls.add(new FoundUrl(entry.getValue(), entry.getKey()));
        }

        List<String> lines = Arrays.asList(text.split("\n", -1));
        boolean inHeaderBlock = false;
        for (String line : lines) {
            if (line.isEmpty()) {
                inHeaderBlock = false;
                continue;
            }
            boolean folded = !inHeaderBlock && !line.startsWith(" ") && !line.startsWith("\t");
            if (HEADER_START.matcher(line).find()) {
                result.headersDetected = true;
                inHeaderBlock = true;
                collectHeaderValue(result, line.substring(line.indexOf(':') + 1));
            } else if (inHeaderBlock && isContinuation(line)) {
                collectHeaderValue(result, line);
            } else if (folded) {
                inHeaderBlock = false;
            }
        }

        collectSenderDomains(lines, result);
        return result;
    }

    private static boolean isContinuation(String line) {
        return line.startsWith(" ") || line.startsWith("\t");
    }

    private static void collectHeaderValue(Extraction result, String value) {
        String cleaned = value
                .replaceAll("(?i)\\bipv6\\s*:", " ")
                .replaceAll("(?i)\\bipv4\\s*:", " ");
        Matcher literal = IP_LITERAL.matcher(cleaned);
        while (literal.find()) {
            addIp(result.headerIps, literal.group(1).replace("%", ""));
        }
        Matcher v4 = IPV4.matcher(cleaned);
        while (v4.find()) {
            addIp(result.headerIps, v4.group());
        }
        Matcher v6 = IPV6_TOKEN.matcher(cleaned);
        while (v6.find()) {
            addIp(result.headerIps, v6.group());
        }
    }

    private static void addIp(List<String> target, String candidate) {
        String value = candidate == null ? "" : candidate.trim();
        if (value.isEmpty() || !value.matches(".*\\d.*")) {
            return;
        }
        if (value.contains(":")) {
            if (looksLikeIpv6(value)) {
                addUnique(target, value);
            }
            return;
        }
        if (looksLikeIpv4(value)) {
            addUnique(target, value);
        }
    }

    public static boolean looksLikeIpv4(String value) {
        String[] groups = value.split("\\.", -1);
        if (groups.length != 4) {
            return false;
        }
        for (String group : groups) {
            if (group.isEmpty() || group.length() > 3) {
                return false;
            }
            for (int i = 0; i < group.length(); i++) {
                if (!Character.isDigit(group.charAt(i))) {
                    return false;
                }
            }
            if (Integer.parseInt(group) > 255) {
                return false;
            }
        }
        return true;
    }

    public static boolean looksLikeIpv6(String value) {
        String address = value;
        int zone = address.indexOf('%');
        if (zone >= 0) {
            address = address.substring(0, zone);
        }
        if (address.isEmpty() || !address.contains(":")) {
            return false;
        }
        if (address.contains("::")) {
            String[] halves = address.split("::", -1);
            if (halves.length != 2) {
                return false;
            }
            int count = 0;
            for (String half : halves) {
                if (half.isEmpty()) {
                    continue;
                }
                for (String group : half.split(":", -1)) {
                    if (!HEX_GROUP.matcher(group).matches()) {
                        return false;
                    }
                    count++;
                }
            }
            return count >= 1 && count <= 7;
        }
        String[] groups = address.split(":", -1);
        if (groups.length != 8) {
            return false;
        }
        for (String group : groups) {
            if (!HEX_GROUP.matcher(group).matches()) {
                return false;
            }
        }
        return true;
    }

    private static void collectSenderDomains(List<String> lines, Extraction result) {
        for (String line : lines) {
            if (line.isEmpty()) {
                continue;
            }
            Matcher name = HEADER_START.matcher(line);
            if (!name.find()) {
                continue;
            }
            if (!name.group(1).toLowerCase(Locale.ROOT).matches("(from|sender|reply-to)")) {
                continue;
            }
            Matcher address = FROM_ADDRESS.matcher(deobfuscate(line));
            while (address.find()) {
                int at = address.group().lastIndexOf('@');
                if (at < 0) {
                    continue;
                }
                String domain = address.group().substring(at + 1).toLowerCase(Locale.ROOT);
                if (!isSkipHost(domain)) {
                    addUnique(result.senderDomains, domain);
                }
            }
        }
    }

    private static void addUnique(List<String> target, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        for (String existing : target) {
            if (existing.equalsIgnoreCase(value)) {
                return;
            }
        }
        target.add(value);
    }

    public static String deobfuscate(String input) {
        if (input == null) {
            return "";
        }
        String out = input.trim();

        out = out.replaceAll("(?i)hxxps://", "https://");
        out = out.replaceAll("(?i)hxxp://", "http://");
        out = out.replaceAll("(?i)\\[\\s*(?:\\.|dot|d0t|domn)[\\s]*]", ".");
        out = out.replaceAll("(?i)[\\(\\[\\{]\\s*(?:\\.|dot|d0t|domn)[\\s]*[\\)\\]\\}]", ".");
        out = out.replaceAll("(?i)(?<=[0-9A-Za-z])[\\s._-]*dot[\\s._-]*(?=[0-9A-Za-z])", ".");
        out = out.replace("&#46;", ".");
        out = out.replace("&#x2e;", ".");
        out = out.replace("&#X2E;", ".");
        out = out.replace("&#58;", ":");
        out = out.replace("&#x3a;", ":");
        out = out.replace("&#61;", "=");
        out = out.replace("&#x3d;", "=");
        out = out.replace("&amp;", "&");
        out = out.replace("&quot;", "\"");
        out = out.replace("&#39;", "'");
        out = percentDecode(out);
        return out.trim();
    }

    public static String normalize(String url) {
        String out = deobfuscate(url);
        if (out.isEmpty()) {
            return "";
        }
        out = stripTrailingPunctuation(out);
        if (!out.matches("(?i)^https?://\\S+$")) {
            out = "http://" + out;
        }
        return out;
    }

    private static void collectHrefs(String text, Map<String, String> candidates) {
        Matcher matcher = HREF.matcher(text);
        while (matcher.find()) {
            String href = matcher.group(1);
            if (href.toLowerCase(Locale.ROOT).startsWith("mailto:")) {
                continue;
            }
            addCandidate(candidates, href);
        }
    }

    private static void collectInlineUrls(String text, Map<String, String> candidates) {
        Matcher inline = URL_LIKE.matcher(text);
        while (inline.find()) {
            addCandidate(candidates, inline.group());
        }

        String deobfuscated = deobfuscate(text);
        if (!deobfuscated.equals(text)) {
            Matcher recovered = URL_LIKE.matcher(deobfuscated);
            while (recovered.find()) {
                addCandidate(candidates, recovered.group());
            }
        }

        Matcher bare = SCHEME_LESS.matcher(text);
        while (bare.find()) {
            addCandidate(candidates, bare.group());
        }
    }

    private static void addCandidate(Map<String, String> candidates, String raw) {
        String normalized = normalize(raw);
        if (isInvestigable(normalized)) {
            candidates.putIfAbsent(normalized, raw.trim());
        }
    }

    private static boolean isInvestigable(String url) {
        if (url == null || !url.matches("(?i)^https?://\\S+$")) {
            return false;
        }
        String host = hostOf(url);
        return !host.isEmpty() && !isSkipHost(host) && !host.endsWith(".");
    }

    public static boolean isSkipHost(String host) {
        for (String skip : SKIP_HOSTS) {
            if (host.equals(skip) || host.endsWith("." + skip)) {
                return true;
            }
        }
        return false;
    }

    public static String hostOf(String url) {
        if (url == null) {
            return "";
        }
        String withoutScheme = url.replaceFirst("(?i)^https?://", "");
        int slash = withoutScheme.indexOf('/');
        String authority = slash >= 0 ? withoutScheme.substring(0, slash) : withoutScheme;
        int query = authority.indexOf('?');
        if (query >= 0) {
            authority = authority.substring(0, query);
        }
        int at = authority.lastIndexOf('@');
        if (at >= 0) {
            authority = authority.substring(at + 1);
        }
        if (authority.startsWith("[")) {
            int close = authority.indexOf(']');
            return close > 0 ? authority.substring(1, close).toLowerCase(Locale.ROOT) : "";
        }
        int colon = authority.lastIndexOf(':');
        if (colon > 0 && authority.indexOf(':') == colon) {
            authority = authority.substring(0, colon);
        }
        return authority.trim().toLowerCase(Locale.ROOT);
    }

    private static String percentDecode(String value) {
        if (value.indexOf('%') < 0) {
            return value;
        }
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '%' && i + 2 < value.length()) {
                int hi = Character.digit(value.charAt(i + 1), 16);
                int lo = Character.digit(value.charAt(i + 2), 16);
                if (hi >= 0 && lo >= 0) {
                    out.append((char) ((hi << 4) + lo));
                    i += 2;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    private static String stripTrailingPunctuation(String url) {
        String out = url;
        while (out.length() > 1) {
            char last = out.charAt(out.length() - 1);
            if (last == '.' || last == ',' || last == ';' || last == ':'
                    || last == ')' || last == ']' || last == '>' || last == '"'
                    || last == '\'') {
                out = out.substring(0, out.length() - 1);
            } else {
                break;
            }
        }
        return out;
    }
}