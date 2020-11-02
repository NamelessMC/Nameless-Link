package com.namelessmc.bot;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import javax.security.auth.login.LoginException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.namelessmc.bot.commands.VerifyCommand;
import com.namelessmc.bot.connections.ConnectionManager;
import com.namelessmc.bot.connections.StorageInitializer;
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
    @Getter
    private static URL botUrl;
    @Getter
    private static int webserverPort;
    
    private static boolean debugging = false;

    public static void main(final String[] args) throws IOException {
//        try {
//            final File log = new File("./logs/" + (new java.text.SimpleDateFormat("MM-dd-yyyy-H:mm:ss").format(new java.util.Date(System.currentTimeMillis()))) + ".log");
//            if (!log.exists()) {
//                log.getParentFile().mkdirs();
//                log.createNewFile();
//            }
//            final FileHandler fh = new FileHandler(log.getAbsolutePath(), true);
//            logger.addHandler(fh);
//            fh.setFormatter(new SimpleFormatter());
//        } catch (final SecurityException | IOException e) {
//            e.printStackTrace();
//            System.out.println("[ERROR] Cannot start logger.");
//            System.exit(0);
//        }
        
        initializeConnectionManager();
        
    	final String botUrlStr = System.getenv("BOT_URL");
    	if (botUrlStr == null) {
    		System.err.println("Environment variable BOT_URL not specified");
    		System.exit(1);
    	}
    	
    	try {
    		botUrl = new URL(botUrlStr);
    	} catch (final MalformedURLException e) {
    		System.err.println("Environment variable BOT_URL is not a valid URL");
    		System.exit(1);
    	}
    	
    	final String webserverPortStr = System.getenv("WEBSERVER_PORT");
    	if (webserverPortStr == null) {
    		System.err.println("Environment variable BOT_URL not specified");
    		System.exit(1);
    	}
    	
    	try {
    		webserverPort = Integer.parseInt(webserverPortStr);
    	} catch (final NumberFormatException e) {
    		System.err.println("Environment variable BOT_URL is not a valid number");
    		System.exit(1);
    	}

        try {
        	final String token = System.getenv("DISCORD_TOKEN");
        	if (token == null) {
        		System.err.println("Environment variable DISCORD_TOKEN not specified");
        		System.exit(1);
        	}
            jda = JDABuilder
                    .createDefault(token)
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
        new VerifyCommand();
    }
    
    private static void initializeConnectionManager() throws IOException {
    	String storageType = System.getenv("STORAGE_TYPE");
    	if (storageType == null) {
    		System.out.println("STORAGE_TYPE not specified, assuming STORAGE_TYPE=stateless");
    		storageType = "stateless";
    	}
    	
    	final StorageInitializer<? extends ConnectionManager> init = StorageInitializer.getByName(storageType);
    	if (init == null) {
    		System.err.println("The chosen STORAGE_TYPE is not available, please choose from " + String.join(", ", StorageInitializer.getAvailableNames()));
    		System.exit(1);
    	}
    	
    	connectionManager = init.get();
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
