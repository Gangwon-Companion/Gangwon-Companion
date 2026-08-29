package com.gangwon.companion.domain.search.service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OperatingHours {
    private static final Pattern ALWAYS_OPEN = Pattern.compile("상시\\s*(?:개방|운영)|24\\s*시간");
    private static final Pattern TIME = Pattern.compile(
            "(?<!\\d)([01]?\\d|2[0-4])(?::([0-5]\\d)|\\s*시(?:\\s*([0-5]?\\d)\\s*분)?)");

    private OperatingHours() {}

    public static Optional<Range> parse(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        if (ALWAYS_OPEN.matcher(raw).find()) {
            return Optional.of(new Range("00:00", "24:00"));
        }
        Matcher matcher = TIME.matcher(raw);
        String opens = next(matcher);
        String closes = next(matcher);
        if (opens == null || closes == null) return Optional.empty();
        return Optional.of(new Range(opens, closes));
    }

    private static String next(Matcher matcher) {
        if (!matcher.find()) return null;
        int hour = Integer.parseInt(matcher.group(1));
        String minuteGroup = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
        int minute = minuteGroup == null ? 0 : Integer.parseInt(minuteGroup);
        if (hour == 24 && minute != 0) return null;
        return "%02d:%02d".formatted(hour, minute);
    }

    public record Range(String opensAt, String closesAt) {}
}
