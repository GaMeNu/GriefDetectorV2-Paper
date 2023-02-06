package me.gamenu.griefdetector;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.logging.Level;

public class Utils {

    private static final ItemStack GRIEF_DETECTOR_ITEM = new ItemStack(Material.STRUCTURE_VOID);

    public static final TextColor THEME = TextColor.color(50, 93, 244);
    public static final TextColor ANALOG = TextColor.color(50, 185, 249);
    public static final TextColor COMPLEMENT = TextColor.color(245, 155, 61);
    private static boolean gdSet = false;

    public static void initGd(){
        ItemMeta gdMeta = GRIEF_DETECTOR_ITEM.getItemMeta();

        //DISPLAY-NAME:
        //GriefDetector
        Component displayName = Component.text("Grief").color(THEME).decoration(TextDecoration.ITALIC, false).append(Component.text("Detector").color(ANALOG));
        gdMeta.displayName(displayName);

        //LORES:
        //Place or Break a block while
        //holding this item to get the player
        //who last modified that location
        ArrayList<Component> lores = new ArrayList<>();
        lores.add(Component.text("Place ").color(COMPLEMENT).append(Component.text("or ").color(THEME)).append(Component.text("Break ").color(COMPLEMENT)).append(Component.text("a block while").color(THEME)).decoration(TextDecoration.ITALIC, false));
        lores.add(Component.text("holding this item to get the player").color(THEME).decoration(TextDecoration.ITALIC, false));
        lores.add(Component.text("who last modified that location").color(THEME).decoration(TextDecoration.ITALIC, false));

        gdMeta.lore(lores);
        GRIEF_DETECTOR_ITEM.setItemMeta(gdMeta);
    }

    public static ItemStack getGDItem(){
        if (!gdSet){
            initGd();
            gdSet = true;
        }
        return new ItemStack(GRIEF_DETECTOR_ITEM);
    }

    public static void quit(JavaPlugin context, String msg){
        context.getLogger().log(Level.SEVERE, msg);
        context.getLogger().log(Level.SEVERE, "Plugin is committing suicide...");
        context.getLogger().info("Should GM too? Leave your answers in the comments below!");
        context.getServer().getPluginManager().disablePlugin(context);
    }

    public static Component getTag(){
        return Component.text("[GDv2] ");
    }

    public static Component getBlankTag(){
        return Component.text("         ");
    }

}
