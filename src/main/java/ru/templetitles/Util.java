package ru.templetitles;

import org.bukkit.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List; // Added for List
import java.util.ArrayList; // Added for ArrayList

public class Util {

    // Pattern to match &#RRGGBB
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static String translateColors(String textToTranslate) {
        if (textToTranslate == null) return ""; // Return empty string for null input

        Matcher matcher = HEX_PATTERN.matcher(textToTranslate);
        StringBuffer buffer = new StringBuffer(textToTranslate.length() + 4 * 8); // Pre-allocate buffer

        while (matcher.find()) {
            String group = matcher.group(1); // The RRGGBB part
            // Replace &#RRGGBB with §x§R§R§G§G§B§B sequence
            matcher.appendReplacement(buffer, ChatColor.COLOR_CHAR + "x"
                    + ChatColor.COLOR_CHAR + group.charAt(0) + ChatColor.COLOR_CHAR + group.charAt(1)
                    + ChatColor.COLOR_CHAR + group.charAt(2) + ChatColor.COLOR_CHAR + group.charAt(3)
                    + ChatColor.COLOR_CHAR + group.charAt(4) + ChatColor.COLOR_CHAR + group.charAt(5)
            );
        }
        matcher.appendTail(buffer);

        // Now translate legacy & codes
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    // Helper for lists
    public static List<String> translateStringList(List<String> list) {
        if (list == null || list.isEmpty()) return new ArrayList<>(); // Return new empty list if input is null or empty
        List<String> translatedList = new ArrayList<>();
        for (String s : list) {
            // Ensure individual strings in the list are also handled if they are null
            translatedList.add(translateColors(s != null ? s : "")); 
        }
        return translatedList;
    }
}
