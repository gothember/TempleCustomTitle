package ru.templetitles;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtil {

    // Static fields for localized time units
    private static String daySingular = "day"; // Default to English, no leading/trailing spaces
    private static String dayPlural = "days";
    private static String hourSingular = "hour";
    private static String hourPlural = "hours";
    private static String minuteSingular = "minute";
    private static String minutePlural = "minutes";
    private static String secondSingular = "second";
    private static String secondPlural = "seconds";

    /**
     * Initializes the TimeUtil with localized time unit strings from DataManager.
     * This should be called once when the plugin enables and DataManager is ready.
     * @param dataManager The DataManager instance to fetch configured strings from.
     */
    public static void init(DataManager dataManager) {
        if (dataManager == null) {
            System.err.println("[TimeUtil] Initialization with null DataManager. Using default time units.");
            return;
        }

        // Helper to get string from DM or keep default if DM returns null/empty
        daySingular = getStringOrDefault(dataManager.getTimeUnitDaySingular(), daySingular);
        dayPlural = getStringOrDefault(dataManager.getTimeUnitDayPlural(), dayPlural);
        hourSingular = getStringOrDefault(dataManager.getTimeUnitHourSingular(), hourSingular);
        hourPlural = getStringOrDefault(dataManager.getTimeUnitHourPlural(), hourPlural);
        minuteSingular = getStringOrDefault(dataManager.getTimeUnitMinuteSingular(), minuteSingular);
        minutePlural = getStringOrDefault(dataManager.getTimeUnitMinutePlural(), minutePlural);
        secondSingular = getStringOrDefault(dataManager.getTimeUnitSecondSingular(), secondSingular);
        secondPlural = getStringOrDefault(dataManager.getTimeUnitSecondPlural(), secondPlural);
    }

    private static String getStringOrDefault(String valueFromDataManager, String defaultValue) {
        return (valueFromDataManager != null && !valueFromDataManager.isEmpty()) ? valueFromDataManager : defaultValue;
    }

    // Pattern to match time strings like 1d, 2h, 3m, 4s
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
            return "N/A";
        }
        if (millis == 0) {
            return "0" + " " + secondSingular; // e.g. "0 second"
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
            sb.append(days).append(" ").append(days == 1 ? daySingular : dayPlural);
        }
        if (hours > 0) {
            if (sb.length() > 0) sb.append(" "); // Add space separator
            sb.append(hours).append(" ").append(hours == 1 ? hourSingular : hourPlural);
        }
        if (minutes > 0) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(minutes).append(" ").append(minutes == 1 ? minuteSingular : minutePlural);
        }
        if (seconds > 0 || sb.length() == 0) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(seconds).append(" ").append(seconds == 1 ? secondSingular : secondPlural);
        }
        return sb.toString(); // No trim needed if spaces are managed correctly
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
