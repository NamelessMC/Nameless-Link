package com.namelessmc.bot;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.security.auth.login.LoginException;

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

public class Main {

    @Getter
    private static JDA jda;
    private static final Logger logger = Logger.getLogger("NamelessLink");
    @Getter
    private static final EmbedBuilder embedBuilder = new EmbedBuilder();
    @Getter
    private static final Gson gson = new GsonBuilder().create();
    @Getter
    private static ConnectionManager connectionManager;
    
    private static boolean debugging = false;

    public static void main(final String[] args) throws IOException {
    	if (!Config.check()) {
    		return;
    	}

        try {
            final File log = new File("./logs/" + (new java.text.SimpleDateFormat("MM-dd-yyyy-H:mm:ss").format(new java.util.Date(System.currentTimeMillis()))) + ".log");
            if (!log.exists()) {
                log.getParentFile().mkdirs();
                log.createNewFile();
            }
            final FileHandler fh = new FileHandler(log.getAbsolutePath(), true);
            logger.addHandler(fh);
            fh.setFormatter(new SimpleFormatter());
        } catch (final SecurityException | IOException e) {
            e.printStackTrace();
            System.out.println("[ERROR] Cannot start logger.");
            System.exit(0);
        }
        
        initializeConnectionManager();
        
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
    
    private static void initializeConnectionManager() throws IOException {
        if (System.getenv("GUILD_ID") != null) {
        	// Configure connection manager in stateless mode
        	// One server is defined using environment variables
        	final long guildId = Long.parseLong(System.getenv("GUILD_ID"));
        	final String apiUrl = System.getenv("API_URL");
        	if (apiUrl == null) {
        		System.err.println("API_URL not specified");
        		System.exit(1);
        	}
        	
        	connectionManager = new ConnectionManager(Optional.empty());
        	connectionManager.createNewConnection(new URL(apiUrl), guildId);
        } else {
        	System.out.println("Environment variables GUILD_ID and API_URL not specified, starting in stateful mode");
        	String path = System.getenv("FILE_PATH");
        	if (path == null) {
        		System.out.println("FILE_PATH not set, using file 'guilds.json'");
        		path = "guilds.json";
        	}
        	
        	final File file = new File(path);
        	if (file.isDirectory()) {
        		System.err.println("Path exists and is a directory");
        		System.exit(1);
        	}
        	
        	file.mkdirs();
        	file.createNewFile();
        	connectionManager = new ConnectionManager(Optional.of(file));
        }
    }

    public static void log(final String message) {
        logger.info("[INFO] " + message);
    }

    public static void debug(final String message) {
        if (debugging) {
			logger.info("[DEBUG] " + message);
		}
    }
}
