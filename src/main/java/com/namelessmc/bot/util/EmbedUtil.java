package com.namelessmc.bot.util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.Color;

public class EmbedUtil {

    private static final Color EMBED_COLOR = Color.decode("#2F3136");

    /**
     * Creates a base embed with the bot's avatar and display name in the footer.
     */
    public static EmbedBuilder base(final JDA jda) {
        final String botName = jda.getSelfUser().getEffectiveName();
        final String botAvatar = jda.getSelfUser().getEffectiveAvatarUrl();
        return new EmbedBuilder()
                .setColor(EMBED_COLOR)
                .setFooter(botName, botAvatar);
    }

    /**
     * Creates a simple embed with description text and bot footer.
     */
    public static MessageEmbed message(final JDA jda, final String description) {
        return base(jda).setDescription(description).build();
    }

}
