package ru.templetitles;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.Arrays;
import java.util.List;

public class Util {

    /**
     * Creates an ItemStack for a GUI with a given material, display name, and lore lines.
     *
     * @param material    The Material for the ItemStack.
     * @param displayName The display name for the ItemStack.
     * @param loreLines   Variable arguments for lore lines.
     * @return The created ItemStack.
     */
    public static ItemStack createGuiItem(Material material, String displayName, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { // ItemMeta can be null for some materials, though unlikely for common GUI items
            meta.setDisplayName(displayName);
            if (loreLines != null && loreLines.length > 0) {
                meta.setLore(Arrays.asList(loreLines));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Creates an ItemStack for a GUI with a given material, display name, and a list of lore lines.
     *
     * @param material    The Material for the ItemStack.
     * @param displayName The display name for the ItemStack.
     * @param lore        A List of Strings for the lore.
     * @return The created ItemStack.
     */
    public static ItemStack createGuiItem(Material material, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            if (lore != null) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
