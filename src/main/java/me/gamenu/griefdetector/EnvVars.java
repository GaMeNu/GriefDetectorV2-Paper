package me.gamenu.griefdetector;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class EnvVars {
    //Fuck .envs, I make my OWN scuffed env vars thingy!!!
    //Directory name
    private static final String DIR_NAME = "plugins/GMPlugins_res";

    public static final int DATABASE_FORMAT = 2;

    //DB URL and DB directory path
    public static final String DB_URL = "jdbc:sqlite:" + new File("").getAbsolutePath() + "/"+DIR_NAME+"/gd_db.db";

    public static final Path DATA_PATH = Paths.get(new File("").getAbsolutePath() + "/"+DIR_NAME + "/format.json");
    public static final Path DB_DIR_PATH = Paths.get(new File("").getAbsolutePath() + "/"+DIR_NAME);

    //Column names in DB
    public static final String ID_COLUMN = "bwu_id";


}
