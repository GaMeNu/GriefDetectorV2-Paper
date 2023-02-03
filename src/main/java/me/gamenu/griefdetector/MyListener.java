package me.gamenu.griefdetector;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MyListener implements Listener {

    private static final ConcurrentHashMap<UUID, Integer> playerCache = new ConcurrentHashMap<>();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        Connection conn;
        PreparedStatement statement;

        try {
            //Check if player already exists in DB
            conn = DriverManager.getConnection(EnvVars.DB_URL);
            PreparedStatement checkStmt = conn.prepareStatement("SELECT " + EnvVars.ID_COLUMN + " FROM players WHERE uuid = ?;");
            checkStmt.setString(1, event.getPlayer().getUniqueId().toString());
            ResultSet rs = checkStmt.executeQuery();

            int bwuId;

            if (rs.next()){
                //Player in DB
                bwuId = rs.getInt(EnvVars.ID_COLUMN);
                event.getPlayer().sendMessage(Component.text("Hello there, " + event.getPlayer().getName() +"! (ID:" + bwuId + ")"));
                checkStmt = conn.prepareStatement("SELECT username FROM players WHERE uuid = ?;");
                checkStmt.setString(1, event.getPlayer().getUniqueId().toString());
                rs = checkStmt.executeQuery();

                if (!rs.next()){
                    statement = conn.prepareStatement("INSERT INTO players (username,uuid) VALUES (?,?);");
                    statement.setString(1, event.getPlayer().getName());
                    statement.setString(2, event.getPlayer().getUniqueId().toString());
                    event.getPlayer().getServer().getLogger().info("Update username for player");

                } else if (!rs.getString("username").equals(event.getPlayer().getName())) {
                    statement = conn.prepareStatement("UPDATE players SET username = ? WHERE uuid = ?;");
                    statement.setString(1, event.getPlayer().getName());
                    statement.setString(2, event.getPlayer().getUniqueId().toString());
                    event.getPlayer().getServer().getLogger().info("Update username for player");
                }

            } else {

                //Register player to DB
                statement = conn.prepareStatement("INSERT INTO players (username, uuid) VALUES (?,?);");
                statement.setString(1, event.getPlayer().getName());
                statement.setString(2,event.getPlayer().getUniqueId().toString());
                statement.executeUpdate();
                event.getPlayer().getServer().getLogger().info("Found new player! Registering to Database...");

                //Greet the player
                //Gotta be nice, you know
                checkStmt = conn.prepareStatement("SELECT " + EnvVars.ID_COLUMN + " FROM players WHERE uuid = ?;");
                checkStmt.setString(1, event.getPlayer().getUniqueId().toString());
                bwuId = checkStmt.executeQuery().getInt(EnvVars.ID_COLUMN);


                event.getPlayer().sendMessage(Component.text("Welcome, "+event.getPlayer().getName()+"! (ID:"+bwuId+")"));
            }

            playerCache.put(event.getPlayer().getUniqueId(), bwuId);
            event.getPlayer().getServer().getLogger().info("Player " + event.getPlayer().getName() + " was put to cache");
            conn.close();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


    }

    @EventHandler
    private void playerQuitEvent(PlayerQuitEvent event){
        playerCache.remove(event.getPlayer().getUniqueId());
        event.getPlayer().getServer().getLogger().info("Player " + event.getPlayer().getName() + " was removed from the cache");
    }


    private void updateDBWithBlock(Event event, Player player, Block block){
        if (!(event instanceof BlockPlaceEvent) && !(event instanceof BlockBreakEvent)) {
            return;
        }

        PreparedStatement statement;
        try (Connection conn = DriverManager.getConnection(EnvVars.DB_URL)) {

            //Get the player's BWU ID
            ResultSet rs;
            int bwuId = playerCache.get(player.getUniqueId());

            //Get the record for current key "(x,y,z)" and check if exists
            Location coords = block.getLocation();
            String key = "(" + coords.getBlockX() + "," + coords.getBlockY() + "," + coords.getBlockZ() + ")";
            statement = conn.prepareStatement("SELECT * FROM gd_store WHERE coords = ?;");
            statement.setString(1, key);
            rs = statement.executeQuery();

            PreparedStatement modifyRecord;

            //Check if record exists for key
            if (!rs.next()) {
                //Create new record
                modifyRecord = conn.prepareStatement("INSERT INTO gd_store (coords, " + EnvVars.ID_COLUMN + ") VALUES (?,?);");
                modifyRecord.setString(1, key);
                modifyRecord.setInt(2, bwuId);

            } else {
                //Update existing record
                modifyRecord = conn.prepareStatement("UPDATE gd_store SET bwu_id = ? WHERE coords = ?;");
                modifyRecord.setString(2, key);
                modifyRecord.setInt(1, bwuId);
            }


            modifyRecord.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @EventHandler
    public void onPlayerPlaceBlock(BlockPlaceEvent event){
        updateDBWithBlock(event, event.getPlayer(), event.getBlock());
    }

    @EventHandler
    public void onPlayerBreakBlock(BlockBreakEvent event){
        updateDBWithBlock(event, event.getPlayer(), event.getBlock());
    }
}
