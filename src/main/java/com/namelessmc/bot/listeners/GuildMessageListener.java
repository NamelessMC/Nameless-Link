package com.namelessmc.bot.listeners;

import com.namelessmc.bot.commands.Command;
import com.namelessmc.bot.commands.CommandContext;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class GuildMessageListener extends ListenerAdapter {

    @SneakyThrows
    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        User user = event.getAuthor();

        if (user.isBot()) return;

        String message = event.getMessage().getContentRaw();
        String[] args = message.split(" ");

        Class<?> clazz = Command.getCommand(args[0], CommandContext.GUILD_MESSAGE);
        if (clazz != null) clazz.getDeclaredMethod("execute", User.class, String[].class, MessageChannel.class).invoke(null, user, args, event.getChannel());
    }
}
