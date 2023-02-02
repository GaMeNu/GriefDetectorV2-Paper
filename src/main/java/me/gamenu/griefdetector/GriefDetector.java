package me.gamenu.griefdetector;

import java.nio.file.Files;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.bukkit.plugin.java.JavaPlugin;



public final class GriefDetector extends JavaPlugin {


    @Override
    public void onEnable() {
        // Plugin startup logic

        getServer().getPluginManager().registerEvents(new MyListener(), this);

        final Logger log = getLogger();
        log.info("GDv2 is now online!");
        String url = EnvVars.DB_URL;




        Connection conn = null;
        Statement statement;

        try {
            //get SQLite DB
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(url);

            //make sure file exists and is directory
            if (!Files.exists(EnvVars.DB_DIR_PATH) || !Files.isDirectory(EnvVars.DB_DIR_PATH)) {
                log.info("File" + EnvVars.DB_DIR_PATH + "doesn't exist. Creating...");
                throw new Exception("File" + EnvVars.DB_DIR_PATH + "doesn't exist");
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage() + "\nCaused by: " + e.getCause());
            log.info("Attempting to create new files...");
            try {

                Files.createDirectory(EnvVars.DB_DIR_PATH);

                log.info("Directory attempt: " + Files.exists(EnvVars.DB_DIR_PATH) + "\n" + EnvVars.DB_DIR_PATH);

                conn = DriverManager.getConnection(url);
            } catch (Exception ex) {
                Utils.quit(this, e.getClass().getName() + ": " + e.getMessage() + "\nCaused by: " + e.getCause());
            }

            log.info("File created successfully! Trying to get SQLite db...");
            try {
                Class.forName("org.sqlite.JDBC");
                conn = DriverManager.getConnection(url);
            } catch (Exception ex) {
                Utils.quit(this, "Cannot find DB file...");
            }


        }
        if (conn == null) {
            Utils.quit(this, "");
        }

        try {
            statement = conn.createStatement();
            ResultSet rs = conn.getMetaData().getTables(null, null, "players", null);
            if (!rs.next()) {
                statement.executeUpdate("CREATE TABLE players ("+EnvVars.ID_COLUMN +" INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, uuid TEXT);");
                log.info("Created new table \"players\"");
            }

            statement = conn.createStatement();
            rs = conn.getMetaData().getTables(null, null, "gd_store", null);
            if (!rs.next()){
                statement.executeUpdate("CREATE TABLE gd_store (coords TEXT PRIMARY KEY, bwu_id INTEGER, FOREIGN KEY (bwu_id) REFERENCES players(bwu_id));");
                log.info("Created new table \"gd_store\"");
            }

        } catch (SQLException e) {
            Utils.quit(this, e.getMessage());
        }

        try {
            conn.close();
        } catch (SQLException e){
            Utils.quit(this, "Could not close connection.");
        }


    }




    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
