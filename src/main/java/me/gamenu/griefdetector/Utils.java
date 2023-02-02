package me.gamenu.griefdetector;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Utils {
    public static void quit(JavaPlugin context, String msg){
        context.getLogger().log(Level.SEVERE, msg);
        context.getLogger().log(Level.SEVERE, "Plugin is committing suicide...");
        context.getLogger().info("Should GM too? Leave your answers in the comments below!");
        context.getServer().getPluginManager().disablePlugin(context);
    }

    public static int[] parseLocation(String loc){
        loc = loc.substring(1, loc.length()-1);
        int lastIndex = 0;
        String x_str=null, y_str=null, z_str=null;
        for (int i = 0; i < loc.length(); i++) {
            if (loc.charAt(i) == ','){
                if (x_str == null) x_str = loc.substring(lastIndex, i);
                if (y_str == null) y_str = loc.substring(lastIndex, i);
                lastIndex = i+1;
            }

        }

        z_str = loc.substring(lastIndex);

        return new int[] {Integer.parseInt(x_str), Integer.parseInt(y_str), Integer.parseInt(z_str)};
    }
}
