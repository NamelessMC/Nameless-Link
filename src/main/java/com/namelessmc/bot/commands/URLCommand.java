package com.namelessmc.bot.commands;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

import java.util.Collections;

public class URLCommand extends Command {

    public URLCommand() {
        super("!url", Collections.singletonList("!apiurl"), CommandContext.PRIVATE_MESSAGE);
    }

    @Override
    public void execute(User user, String[] args, MessageChannel channel) {

    }
}
