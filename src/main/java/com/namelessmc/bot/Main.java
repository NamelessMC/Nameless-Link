package com.namelessmc.bot;

import com.google.gson.JsonParser;
import com.namelessmc.bot.commands.LanguageCommand;
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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Main {

    @Getter
    private static JDA jda;
    @Getter
    private static Connection connection;
    @Getter
    private static final JsonParser jsonParser = new JsonParser();
    @Getter
    private static final EmbedBuilder embedBuilder = new EmbedBuilder();

    private static boolean debugging = false;

    public static void main(String[] args) {
        try {
            jda = JDABuilder
                    .createDefault(Config.get("settings", "token"))
                    .addEventListeners(new GuildJoinHandler())
                    .addEventListeners(new PrivateMessageListener())
                    .addEventListeners(new GuildMessageListener())
                    .addEventListeners(new DiscordRoleListener())
                    .setChunkingFilter(ChunkingFilter.ALL)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS)
                    .build();
        } catch (LoginException e) {
            e.printStackTrace();
        }

        try {
            String server = Config.get("mysql", "server");
            String database = Config.get("mysql", "database");
            String username = Config.get("mysql", "username");
            String password = Config.get("mysql", "password");

            String url = "jdbc:mysql://" + server + "/" + database + "?failOverReadOnly=false&maxReconnects=10&autoReconnect=true&serverTimezone=UTC";

            connection = DriverManager.getConnection(url, username, password);
            log("Connected to central database.");
        } catch (SQLException e) {
            e.printStackTrace();
            log("[ERROR] Could not connect to central database!");
            jda.shutdown();
        }

        if (args.length >= 1 && args[0].equals("-debug")) {
            log("Debugging enabled");
            debugging = true;
        }

        HttpMain.init();

        // Register commands
        new LanguageCommand();
    }

    public static void log(String message) {
        System.out.println("[INFO] " + message);
    }

    public static void debug(String message) {
        if (debugging) System.out.println("[DEBUG] " + message);
    }
}
