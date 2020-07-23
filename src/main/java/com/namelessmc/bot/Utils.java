package com.namelessmc.bot;

import com.namelessmc.NamelessAPI.NamelessAPI;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class Utils {

    public static void messageGuildOwner(long guild_id, String message) {
        messageUser(Main.getJda().getGuildById(guild_id).retrieveOwner().complete().getUser(), message);
    }

    public static void messageUser(User user, String message) {
        user.openPrivateChannel().queue((channel) -> {
            channel.sendMessage(message).queue();
        });
    }

    public static void messageUser(User user, EmbedBuilder embed) {
        user.openPrivateChannel().queue((channel) -> {
            channel.sendMessage(embed.build()).queue();
        });
    }

    public static NamelessAPI getApiFromString(String url) {
        return getNamelessAPI(url);
    }

    private static NamelessAPI getNamelessAPI(String url) {
        NamelessAPI api;
        if (url == null) return null;
        else {
            URL apiUrl;
            try {
                apiUrl = new URL(url);
            } catch (final MalformedURLException e) { return null; }

            api = new NamelessAPI(apiUrl, false);
            api.setUserAgent("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");

            if (api.checkWebAPIConnection() != null) return null;
        }
        return api;
    }

    public static String listToString(List<String> list, String delimiter) {
        return String.join(delimiter, list);
    }
}
