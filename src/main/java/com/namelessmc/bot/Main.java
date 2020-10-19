package com.namelessmc.bot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.namelessmc.bot.commands.LanguageCommand;
import com.namelessmc.bot.commands.URLCommand;
import com.namelessmc.bot.commands.VerifyCommand;
import com.namelessmc.bot.http.HttpMain;
import com.namelessmc.bot.listeners.DiscordRoleListener;
import com.namelessmc.bot.listeners.GuildJoinHandler;
import com.namelessmc.bot.listeners.GuildMessageListener;
import com.namelessmc.bot.listeners.PrivateMessageListener;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Main {

    @Getter
    private static JDA jda;
    private static Connection connection;
    private static final Logger logger = Logger.getLogger("NamelessLink");
    @Getter
    private static final EmbedBuilder embedBuilder = new EmbedBuilder();
    @Getter
    private static final Gson gson = new GsonBuilder().create();

    private static boolean debugging = false;

    public static void main(String[] args) {
    	if (!Config.check()) {
    		return;
    	}

        try {
            File log = new File("./logs/" + (new java.text.SimpleDateFormat("MM-dd-yyyy-H:mm:ss").format(new java.util.Date(System.currentTimeMillis()))) + ".log");
            if (!log.exists()) {
                log.getParentFile().mkdirs();
                log.createNewFile();
            }
            FileHandler fh = new FileHandler(log.getAbsolutePath(), true);
            logger.addHandler(fh);
            fh.setFormatter(new SimpleFormatter());
        } catch (final SecurityException | IOException e) {
            e.printStackTrace();
            System.out.println("[ERROR] Cannot start logger.");
            System.exit(0);
        }

        try {
            final String url = "jdbc:mysql://" + Config.MYSQL_HOSTNAME + "/" + Config.MYSQL_DATABASE + "?failOverReadOnly=false&maxReconnects=10&autoReconnect=true&serverTimezone=UTC";
            connection = DriverManager.getConnection(url, Config.MYSQL_USERNAME, Config.MYSQL_PASSWORD);
            log("Connected to database.");
        } catch (final SQLException e) {
            e.printStackTrace();
            log("[ERROR] Could not connect to database!");
            return;
        }

        try {
            jda = JDABuilder
                    .createDefault(Config.DISCORD_TOKEN)
                    .addEventListeners(new GuildJoinHandler())
                    .addEventListeners(new PrivateMessageListener())
                    .addEventListeners(new GuildMessageListener())
                    .addEventListeners(new DiscordRoleListener())
                    .setChunkingFilter(ChunkingFilter.ALL)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS)
                    .build();
        } catch (final LoginException e) {
            e.printStackTrace();
            return;
        }

        if (args.length >= 1 && args[0].equals("-debug")) {
            log("Debugging enabled");
            debugging = true;
        }

        HttpMain.init();

        // Register commands
        new LanguageCommand();
        new VerifyCommand();
        new URLCommand();
    }

    public static Connection getConnection() {
        try {
            if (connection.isClosed() || !connection.isValid(3)) {
                final String url = "jdbc:mysql://" + Config.MYSQL_HOSTNAME + "/" + Config.MYSQL_DATABASE + "?failOverReadOnly=false&maxReconnects=10&autoReconnect=true&serverTimezone=UTC";
                connection = DriverManager.getConnection(url, Config.MYSQL_USERNAME, Config.MYSQL_PASSWORD);
                debug("Database connection was lost, but re-established.");
            }
            return connection;
        } catch (SQLException e) {
            e.printStackTrace();
            log("[ERROR] Connection is invalid, and cannot recover.");
            return null;
        }
    }

    public static void log(String message) {
        logger.info("[INFO] " + message);
    }

    public static void debug(String message) {
        if (debugging) logger.info("[DEBUG] " + message);
    }
}
