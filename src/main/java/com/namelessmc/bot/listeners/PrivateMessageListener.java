package com.namelessmc.bot.listeners;

import com.namelessmc.bot.Language;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.Queries;
import com.namelessmc.bot.Utils;
import com.namelessmc.bot.commands.Command;
import com.namelessmc.bot.commands.CommandContext;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;

public class PrivateMessageListener extends ListenerAdapter {

    @SneakyThrows
    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
        User user = event.getAuthor();

        if (user.isBot()) return;
        String message = event.getMessage().getContentRaw();
        String[] args = message.split(" ");

        Command command = Command.getCommand(args[0], CommandContext.PRIVATE_MESSAGE);
        if (command != null) command.execute(user, args, event.getChannel());
        else {
            Language language = Queries.getUserLanguage(user.getId());
            if (language == null) language = new Language("EnglishUK");

            Main.getEmbedBuilder().clear().setColor(Color.GREEN).setTitle(language.get("commands")).addField(language.get("help"), language.get("invalid_command"), false);
            Utils.messageUser(user, Main.getEmbedBuilder());
        }
    }
}
