package com.namelessmc.bot.commands;

import lombok.Getter;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public abstract class Command {

    @Getter
    private final String label;
    @Getter
    private final String[] aliases;
    @Getter
    private final CommandContext context;

    @Getter
    private static final HashMap<String, Command> registeredCommands = new HashMap<>();
    private static final List<String> registeredCommandLabels = new ArrayList<>();

    public Command(String label, String[] aliases, CommandContext context) {
        this.label = label;
        this.aliases = aliases;
        this.context = context;

        if (registeredCommandLabels.contains(label)) throw new IllegalStateException("Command already registered");
        for (String alias : aliases) if (registeredCommandLabels.contains(alias)) throw new IllegalStateException("Command already registered");

        registeredCommands.put(label, this);
        for (String alias : aliases) registeredCommands.put(alias, this);

        registeredCommandLabels.add(label);
        registeredCommandLabels.addAll(Arrays.asList(aliases));
    }

    public abstract void execute(User user, String[] args, MessageChannel channel);

    public static Command getCommand(String label, CommandContext context) {
        for (Command command : registeredCommands.values()) {
            if (command.getLabel().equals(label)) {
                if (checkContext(command.getContext(), context)) return command;
            } else {
                for (String alias : command.getAliases()) {
                    if (label.equals(alias)) {
                        if (checkContext(command.getContext(), context)) return command;
                    }
                }
            }
        }
        return null;
    }

    private static boolean checkContext(CommandContext givenContext, CommandContext receivedContext) {
        return givenContext == receivedContext || givenContext == CommandContext.BOTH;
    }
}
