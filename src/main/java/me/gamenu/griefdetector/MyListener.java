package me.gamenu.griefdetector;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Listener class for GriefDetector V2, manipulates the gd_storage database
 *
 * @author GaMeNu and ChatGPT
 */
public class MyListener implements Listener {

    //Hashmaps
    //(Java you idiots call them D I C T I O N A R I E S)
    private static final ConcurrentHashMap<UUID, Integer> playerCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Block, Object[]> blockCache = new ConcurrentHashMap<>();

    private static final int QUERY_LIMIT = 16;
    private static final int TOP_QUERY_LIMIT = 32;
    private static int queries;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Connection conn;
        PreparedStatement statement;

        try {
            //Check if player already exists in DB
            conn = DriverManager.getConnection(EnvVars.DB_URL);
            PreparedStatement checkStmt = conn.prepareStatement("SELECT " + EnvVars.ID_COLUMN + " FROM players WHERE uuid = ?;");
            checkStmt.setString(1, event.getPlayer().getUniqueId().toString());
            ResultSet rs = checkStmt.executeQuery();

            int bwuId;
            if (rs.next()) {
                //Player in DB
                bwuId = rs.getInt(EnvVars.ID_COLUMN);
                event.getPlayer().sendMessage(Component.text("Hello there, " + event.getPlayer().getName() + "! (ID:" + bwuId + ")"));
                checkStmt = conn.prepareStatement("SELECT username FROM players WHERE uuid = ?;");
                checkStmt.setString(1, event.getPlayer().getUniqueId().toString());
                rs = checkStmt.executeQuery();

                if (!rs.next()) {
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
                statement.setString(2, event.getPlayer().getUniqueId().toString());
                statement.executeUpdate();
                event.getPlayer().getServer().getLogger().info("Found new player! Registering to Database...");

                //Greet the player
                //Gotta be nice, you know
                checkStmt = conn.prepareStatement("SELECT " + EnvVars.ID_COLUMN + " FROM players WHERE uuid = ?;");
                checkStmt.setString(1, event.getPlayer().getUniqueId().toString());
                bwuId = checkStmt.executeQuery().getInt(EnvVars.ID_COLUMN);


                event.getPlayer().sendMessage(Component.text("Welcome, " + event.getPlayer().getName() + "! (ID:" + bwuId + ")"));
            }

            playerCache.put(event.getPlayer().getUniqueId(), bwuId);
            event.getPlayer().getServer().getLogger().info("Player " + event.getPlayer().getName() + " was put to cache");
            conn.close();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


    }

    @EventHandler
    private void playerQuitEvent(PlayerQuitEvent event) {
        playerCache.remove(event.getPlayer().getUniqueId());
        event.getPlayer().getServer().getLogger().info("Player " + event.getPlayer().getName() + " was removed from the cache");

        //This is here because
        //uhh
        //Cache safety? I guess
        //IDK what I'm doing
        //Please don't crash the server?
        try (Connection conn = DriverManager.getConnection(EnvVars.DB_URL)) {
            dumpBlockCache(conn);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    private void updateDBWithBlock(BlockEvent event, Player player) {

        try (Connection conn = DriverManager.getConnection(EnvVars.DB_URL)) {

            //Get the player's BWU ID
            int bwuId = playerCache.get(player.getUniqueId());

            //Get the record for current key "(x,y,z)" and check if exists
            Location coords = event.getBlock().getLocation();
            String key = "(" + coords.getBlockX() + "," + coords.getBlockY() + "," + coords.getBlockZ() + ")";

            //Add statement to cache, if cache is full dump to database
            blockCache.put(event.getBlock(), new Object[]{key, bwuId});
            if (blockCache.size() > 64) {
                dumpBlockCache(conn);
            }

        } catch (Exception e) {
            //I know what I'm doing!!!
            //(No I don't, almost every SQL line here was generated, at least in part, by ChatGPT)
            throw new RuntimeException(e);
        }

    }

    public void dumpBlockCache(Connection conn) throws SQLException {
        PreparedStatement newBlock = conn.prepareStatement("INSERT INTO gd_store (coords, bwu_id, date_modified) VALUES (?,?,date('now'));");
        PreparedStatement updateBlock = conn.prepareStatement("UPDATE gd_store SET bwu_id = ?, date_modified = date('now') WHERE coords = ?;");
        PreparedStatement queryDB;
        ResultSet rs;
        for (Object[] data : blockCache.values()) {
            queryDB = conn.prepareStatement("SELECT * FROM gd_store WHERE coords = ?;");
            queryDB.setString(1, (String) data[0]);
            rs = queryDB.executeQuery();
            if (!rs.next()) {
                //Create new record
                newBlock.setString(1, (String) data[0]);
                newBlock.setInt(2, (Integer) data[1]);
                newBlock.addBatch();

            } else {
                //Update existing record
                updateBlock.setInt(1, (Integer) data[1]);
                updateBlock.setString(2, (String) data[0]);
                updateBlock.addBatch();
            }
        }
        try {
            newBlock.executeBatch();
            updateBlock.executeBatch();
        } catch (SQLException ignored) {
        }
        //i g n o r e d
        //I'm tired
        //Please send help
        //GM is forcing me to write code for him,
        //I need to pretend like I'm documenting my code
        //Oh god I hear him coming ohfuckfuckfu

        blockCache.clear();
        //Bye-bye cache!!!

    }

    public static void initQueries() {
        MyListener.queries = 0;
    }

    public static void decrementQueries() {
        if (MyListener.queries > 0) MyListener.queries--;
    }

    private void useGD(BlockEvent event, Player player) {

        if (queries < TOP_QUERY_LIMIT) queries++;
        else
            player.getServer().getLogger().log(Level.WARNING, "Warning: Top Query limit (" + TOP_QUERY_LIMIT + ") reached by " + player.getName());

        if (queries >= QUERY_LIMIT) {
            player.sendMessage(Utils.getTag().append(Component.text("Error: Query limit reached! please try again in " + (queries / 2.0) + " seconds.").color(TextColor.color(255, 0, 0))));
            return;
        }
        int blockX = event.getBlock().getX();
        int blockY = event.getBlock().getY();
        int blockZ = event.getBlock().getZ();
        String key = "(" + blockX + "," + blockY + "," + blockZ + ")";

        int bwuId;
        String dateStr;

        Object[] cacheData = blockCache.get(event.getBlock());

        //Check whether the block has been modified recently in the cache
        //Else queries the main DB
        if (cacheData != null) {
            bwuId = (Integer) cacheData[1];
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            java.util.Date now = new java.util.Date();
            dateStr = format.format(now);
        } else {
            try (Connection conn = DriverManager.getConnection(EnvVars.DB_URL)) {
                PreparedStatement getBlock = conn.prepareStatement("SELECT bwu_id, date_modified FROM gd_store WHERE coords = ?;");
                getBlock.setString(1, key);
                ResultSet rs = getBlock.executeQuery();

                if (!rs.next()) {
                    player.sendMessage(Utils.getTag().append(Component.text("There seems to be no player assigned to location ").color(Utils.THEME)).append(Component.text(key).color(Utils.COMPLEMENT)));
                    return;
                } else {
                    bwuId = rs.getInt("bwu_id");
                    dateStr = rs.getString("date_modified");
                }

            } catch (SQLException e) {
                player.sendMessage(Utils.getTag().append(Component.text("Error: failed to retrieve block from the database! Please check the server console for more info.").color(TextColor.color(255, 0, 0))));
                player.getServer().getLogger().log(Level.WARNING, "Failed to retrieve key " + key + " from database:\n" + e);
                return;
            }
        }

        try (Connection conn = DriverManager.getConnection(EnvVars.DB_URL)) {
            PreparedStatement getUserData = conn.prepareStatement("SELECT username, uuid FROM players WHERE " + EnvVars.ID_COLUMN + " = ?;");
            getUserData.setInt(1, bwuId);
            ResultSet rs = getUserData.executeQuery();

            if (!rs.next()) {
                player.sendMessage(Utils.getTag().append(Component.text("Error: failed to retrieve block from the database - received nonexistent bwuID from gd storage .").color(TextColor.color(255, 0, 0))));
            } else {
                String playerUsername = rs.getString("username");
                String playerUUID = rs.getString("uuid");

                player.sendMessage(Utils.getTag().append(Component.text("Here's the data found about block ").color(Utils.THEME)).append(Component.text(key).color(Utils.ANALOG)).append(Component.text(":").color(Utils.THEME)));
                player.sendMessage(Utils.getBlankTag().append(Component.text("-| ").color(Utils.ANALOG)).append(Component.text("Username: ").color(Utils.THEME)).append(Component.text(playerUsername).color(Utils.COMPLEMENT)));
                player.sendMessage(Utils.getBlankTag().append(Component.text("-| ").color(Utils.ANALOG)).append(Component.text("UUID: ").color(Utils.THEME)).append(Component.text(playerUUID).color(Utils.COMPLEMENT)));
                player.sendMessage(Utils.getBlankTag().append(Component.text("-| ").color(Utils.ANALOG)).append(Component.text("Modification date: ").color(Utils.THEME)).append(Component.text(dateStr).color(Utils.COMPLEMENT)));
            }

        } catch (SQLException e) {
            player.sendMessage(Utils.getTag().append(Component.text("Error: failed to retrieve block from the database! Please check the server console for more info.").color(TextColor.color(255, 0, 0))));
            player.getServer().getLogger().log(Level.WARNING, "Failed to retrieve key " + key + " from database:\n" + e);
        }


    }

    @EventHandler
    public void onPlayerPlaceBlock(BlockPlaceEvent event) {
        if (event.getItemInHand().equals(Utils.getGDItem())) {
            useGD(event, event.getPlayer());
            event.setCancelled(true);
        } else updateDBWithBlock(event, event.getPlayer());
    }

    @EventHandler
    public void onPlayerBreakBlock(BlockBreakEvent event) {
        if (event.getPlayer().getInventory().getItemInMainHand().equals(Utils.getGDItem())) {
            useGD(event, event.getPlayer());
            event.setCancelled(true);
        } else updateDBWithBlock(event, event.getPlayer());
    }
}
