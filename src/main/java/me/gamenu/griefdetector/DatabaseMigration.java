package me.gamenu.griefdetector;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseMigration {

    //"JaVa Is A vIoLeNt LaNgUaGe We SuPrEsS eRrOrS !!!1111!! 11!!"
    @SuppressWarnings({"ReassignedVariable", "UnusedAssignment"})
    public static void migrate(JavaPlugin context, int currentVersion){
        if (currentVersion == EnvVars.DATABASE_FORMAT){
            return;
        }
        context.getLogger().info("Migrating database from version " + currentVersion +" to version " + EnvVars.DATABASE_FORMAT);
        if (currentVersion == 1){

            //Add the date_modified column to gd_store and set all values to current date
            try (Connection conn = DriverManager.getConnection(EnvVars.DB_URL)){
                PreparedStatement statement = conn.prepareStatement("ALTER TABLE gd_store ADD COLUMN date_modified DATE;");
                statement.executeUpdate();
                statement = conn.prepareStatement("UPDATE gd_store SET date_modified = date('now');");
                statement.executeUpdate();
            } catch (SQLException e) {
                Utils.quit(context, "Error while migrating database from version 1:\n" + e);
                throw new RuntimeException(e);
            }
            //Warning here about "value of currentVersion is unused" is fine- this is infrastructure for a DB backwards compatibility system that will allow migrating databases to newer versions
            //Basically it migrates from one version to the next until reaching the newest version. Better ways could probably be achieved but...
            //I'm a 15.5 years old kid who barely has formal computer science education. This is the best I can think of right now.
            //Now shut the fuck up.
            currentVersion++;
            context.getLogger().info("Successfully migrated database to version 2");
        }
    }
}
