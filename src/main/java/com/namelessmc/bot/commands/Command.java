package com.namelessmc.bot.commands;

import lombok.Getter;

import java.util.HashMap;

public class Command {

    @Getter
    private final String label;
    @Getter
    private final String[] aliases;
    @Getter
    private final CommandContext context;

    @Getter
    private static final HashMap<Command, Class<?>> registeredCommands = new HashMap<>();

    public Command(String label, String[] aliases, CommandContext context) {
        this.label = label;
        this.aliases = aliases;
        this.context = context;

        if (getCommand(label, CommandContext.BOTH) != null) registeredCommands.put(this, getClass());
        else throw new IllegalStateException("Command already registered");
    }

    public static Class<?> getCommand(String label, CommandContext context) {
        for (Command command : registeredCommands.keySet()) {
            if (command.getLabel().equals(label)) {
                if (checkContext(command.getContext(), context)) return registeredCommands.get(command);
            } else {
                for (String alias : command.getAliases()) {
                    if (label.equals(alias)) {
                        if (checkContext(command.getContext(), context)) return registeredCommands.get(command);
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
