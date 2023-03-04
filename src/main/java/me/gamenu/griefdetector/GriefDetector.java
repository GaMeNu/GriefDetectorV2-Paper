package me.gamenu.griefdetector;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;


import me.gamenu.griefdetector.commands.GdCommand;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public final class GriefDetector extends JavaPlugin {

    MyListener listener;

    @Override
    public void onEnable() {
        // Plugin startup logic
        listener = new MyListener();
        getServer().getPluginManager().registerEvents(listener, this);

        final Logger log = getLogger();
        log.info("GDv2 is now online!");
        String url = EnvVars.DB_URL;
        MyListener.initQueries();
        getServer().getScheduler().scheduleSyncRepeatingTask(this, MyListener::decrementQueries, 0L, 10L);

        //Register gd command
        PluginCommand cmd = getCommand("gd");
        if (cmd == null){
            log.log(Level.WARNING, "An error has occurred while trying to load \"gd\" command: command returned null value");
        } else {
            cmd.setExecutor(new GdCommand());
        }

        //SQLite shit starts here
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
            //If files not found
            log.log(Level.SEVERE, e.getClass().getName() + ": " + e.getMessage() + "\nCaused by: " + e.getCause());
            log.info("Attempting to create new files...");
            try {

                //Attempt to create the directory
                Files.createDirectory(EnvVars.DB_DIR_PATH);

                log.info("Directory attempt- successfully created new directory? " + Files.exists(EnvVars.DB_DIR_PATH) + "\n" + EnvVars.DB_DIR_PATH);

                conn = DriverManager.getConnection(url);
            } catch (Exception ex) {
                //Failed to create the directory
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

        int format;

        if (!Files.exists(EnvVars.DATA_PATH)){
            format = 1;
            JSONObject data = new JSONObject();

            //Help how remove error
            data.put("format",EnvVars.DATABASE_FORMAT);
            try {
                FileWriter fw = new FileWriter(EnvVars.DATA_PATH.toString());
                fw.write(data.toJSONString());
                fw.close();

            } catch (IOException e) {
                Utils.quit(this, "I have no fucking idea how we got here, but here's the error for your convenience: (Scared of the word \"fuck\"? Well fuuuck you than! (Sorry, that really wasn't nice, please forgive me :( ))\n" + e);
                throw new RuntimeException(e);
            }


        } else {
            JSONObject data;
            try {
                data = (JSONObject) new JSONParser().parse(new FileReader(EnvVars.DATA_PATH.toString()));
            } catch (IOException | ParseException e) {
                Utils.quit(this, e.toString());
                throw new RuntimeException(e);
            }

            //Please fucking kill me, casting is pain
            format = Math.toIntExact((long) data.get("format"));
        }

        DatabaseMigration.migrate(this, format);
        //This quits the plugin, JetBrains you idiots
        if (conn == null)
            Utils.quit(this, "");

        try {
            statement = conn.createStatement();
            ResultSet rs = conn.getMetaData().getTables(null, null, "players", null);
            if (!rs.next()) {
                statement.executeUpdate("CREATE TABLE players (bwu_id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, uuid TEXT);");
                log.info("Created new table \"players\"");
            }

            statement = conn.createStatement();
            rs = conn.getMetaData().getTables(null, null, "gd_store", null);
            if (!rs.next()){
                statement.executeUpdate("CREATE TABLE gd_store (coords TEXT PRIMARY KEY, bwu_id INTEGER, date_modified DATE, FOREIGN KEY (bwu_id) REFERENCES players(bwu_id));");
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

        //Task for looping deleting all dates earlier than 2 weeks ago
        this.getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            this.getServer().getLogger().info("Deleting old database entries...");

            //Ask the DB to delete all days earlier than 2 weeks ago
            try (Connection conn1 = DriverManager.getConnection(EnvVars.DB_URL)){
                PreparedStatement deleteOld = conn1.prepareStatement("DELETE FROM gd_store WHERE date(date_modified) < date('now','-14 days');");
                int rows = deleteOld.executeUpdate();
                this.getLogger().info("Successfully deleted " + rows + " records.");

            } catch (SQLException e) {

                Utils.quit(this, "Failed to clear old database entries:\n" + e);
            }
        }, 0L, 1728000L);


    }




    @Override
    public void onDisable() {
        // Plugin shutdown logic
        try(Connection conn = DriverManager.getConnection(EnvVars.DB_URL)) {
            listener.dumpBlockCache(conn);
        } catch (SQLException e) {
            Utils.quit(this, "Failed to dump block cache at shutdown:\n" + e);
        }
    }
}
