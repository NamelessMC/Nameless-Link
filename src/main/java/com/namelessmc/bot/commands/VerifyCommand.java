package com.namelessmc.bot.commands;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

import java.util.Collections;

public class VerifyCommand extends Command {

    public VerifyCommand() {
        super("!verify", Collections.emptyList(), CommandContext.PRIVATE_MESSAGE);
    }

    @Override
    public void execute(User user, String[] args, MessageChannel channel) {

    }
}
