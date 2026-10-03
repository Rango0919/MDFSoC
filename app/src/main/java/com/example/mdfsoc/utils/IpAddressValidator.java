package com.example.mdfsoc.utils;

import java.util.regex.Pattern;

public final class IpAddressValidator {

    private static final Pattern IPV4 =
            Pattern.compile("^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$");

    private IpAddressValidator() {
    }

    public static boolean isValid(String ip) {
        if (ip == null) {
            return false;
        }
        String trimmed = ip.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        if (IPV4.matcher(trimmed).matches()) {
            return true;
        }
        return isLikelyIpv6(trimmed);
    }

    private static boolean isLikelyIpv6(String candidate) {
        if (!candidate.contains(":")) {
            return false;
        }
        if (candidate.split("::", -1).length - 1 > 1) {
            return false;
        }
        int colonGroups = candidate.split(":").length - 1;
        if (colonGroups < 2 || colonGroups > 8) {
            return false;
        }
        for (String group : candidate.split(":")) {
            if (!group.isEmpty() && !group.matches("^[0-9A-Fa-f]{1,4}$")) {
                return false;
            }
        }
        return true;
    }
}