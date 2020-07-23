package com.namelessmc.bot;

import com.namelessmc.bot.http.HttpMain;
import com.namelessmc.bot.listeners.DiscordRoleListener;
import com.namelessmc.bot.listeners.GuildJoinHandler;
import com.namelessmc.bot.listeners.PrivateMessageListener;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

public class Main {

    @Getter
    private static final Logger logger = Logger.getLogger("Nameless-Bot");
    @Getter
    private static JDA jda;
    @Getter
    private static Connection connection;

    public static void main(String[] args) {
        try {
            HttpMain.init();
            jda = JDABuilder
                    .createDefault(Config.get("settings", "token"))
                    .addEventListeners(new GuildJoinHandler())
                    .addEventListeners(new PrivateMessageListener())
                    .addEventListeners(new DiscordRoleListener())
                    .setChunkingFilter(ChunkingFilter.ALL)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS)
                    .build();
        } catch (LoginException | IOException e) {
            e.printStackTrace();
        }

        try {
            String server = Config.get("mysql", "server");
            String database = Config.get("mysql", "database");
            String username = Config.get("mysql", "username");
            String password = Config.get("mysql", "password");

            String url = "jdbc:mysql://" + server + "/" + database;

            connection = DriverManager.getConnection(url, username, password);
            getLogger().info("Connected to central database");
        } catch (SQLException e) {
            e.printStackTrace();
            getLogger().info("Could not connect to central database");
        }
    }
}
