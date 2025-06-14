package ru.templetitles;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtil {

    // Pattern to match time strings like 1d, 2h, 3m, 4s, or combinations
    // Supports simple formats like "1d", "2h30m", "10s"
    // More complex parsing like "1d2h30m10s" can be added if needed, for now keep it simple for single units
    private static final Pattern TIME_STRING_PATTERN = Pattern.compile("(\\d+)([smhd])"); // s, m, h, d

    /**
     * Parses a time string like "30s", "10m", "1h", "7d" into milliseconds.
     *
     * @param timeString The time string to parse.
     * @return The duration in milliseconds, or 0 if parsing fails.
     */
    public static long parseDuration(String timeString) {
        if (timeString == null || timeString.isEmpty()) {
            return 0L;
        }
        timeString = timeString.toLowerCase();
        Matcher matcher = TIME_STRING_PATTERN.matcher(timeString);

        if (matcher.matches()) {
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);

            switch (unit) {
                case "s":
                    return TimeUnit.SECONDS.toMillis(value);
                case "m":
                    return TimeUnit.MINUTES.toMillis(value);
                case "h":
                    return TimeUnit.HOURS.toMillis(value);
                case "d":
                    return TimeUnit.DAYS.toMillis(value);
                default:
                    return 0L; // Should not happen with current regex
            }
        }
        return 0L; // Invalid format
    }

    /**
     * Formats a duration in milliseconds into a human-readable string.
     * Example: "1 day 2 hours 30 minutes 15 seconds"
     *
     * @param millis The duration in milliseconds.
     * @return A human-readable string representation of the duration.
     */
    public static String formatDuration(long millis) {
        if (millis < 0) {
            return "N/A"; // Or some error/indicator
        }
        if (millis == 0) {
            return "0 seconds";
        }

        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append(" day").append(days > 1 ? "s" : "").append(" ");
        }
        if (hours > 0) {
            sb.append(hours).append(" hour").append(hours > 1 ? "s" : "").append(" ");
        }
        if (minutes > 0) {
            sb.append(minutes).append(" minute").append(minutes > 1 ? "s" : "").append(" ");
        }
        if (seconds > 0 || sb.length() == 0) { // Always show seconds if nothing else or if it's the only unit
            sb.append(seconds).append(" second").append(seconds > 1 ? "s" : "").append(" ");
        }

        return sb.toString().trim();
    }

    /**
     * Formats a specific epoch millisecond timestamp into a date string using the provided format.
     * @param timestamp The epoch millisecond timestamp.
     * @param dateFormat The date format string (e.g., "dd.MM.yyyy HH:mm:ss").
     * @return The formatted date string.
     */
    public static String formatTimestamp(long timestamp, String dateFormat) {
        if (timestamp <= 0 || dateFormat == null || dateFormat.isEmpty()) {
            return "N/A"; // Or some other appropriate default for an invalid timestamp/format
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
            return sdf.format(new Date(timestamp));
        } catch (IllegalArgumentException e) {
            // Log error or return a fallback format
            // For now, returning N/A for bad format
            return "Invalid Date Format";
        }
    }
}
