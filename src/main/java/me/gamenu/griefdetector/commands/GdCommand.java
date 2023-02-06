package me.gamenu.griefdetector.commands;

import me.gamenu.griefdetector.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;


public class GdCommand implements CommandExecutor {


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)){
            return false;
        }
        sender.sendMessage("[GDv2] Received a GriefDetector item!");
        sender.sendMessage(Component.text("[GDv2] ").color(TextColor.color(255, 255, 255)).append(Component.text("You have been taxed for $100 for using GDv2").color(TextColor.color(255, 0, 0))));
        ItemStack gd = new ItemStack(Utils.getGDItem());

        ((Player) sender).getInventory().addItem(gd);
        return true;
    }
}
