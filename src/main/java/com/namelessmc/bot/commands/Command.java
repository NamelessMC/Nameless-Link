package com.namelessmc.bot.commands;

import lombok.Getter;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class Command {

    @Getter
    private final String label;
    @Getter
    private final List<String> aliases;
    @Getter
    private final CommandContext context;

    @Getter
    private static final HashMap<String, Command> registeredCommands = new HashMap<>();
    private static final List<String> registeredCommandLabels = new ArrayList<>();

    public Command(String label, List<String> aliases, CommandContext context) {
        this.label = label;
        this.aliases = aliases;
        this.context = context;

        // check for duplicate labels or aliases
        if (registeredCommandLabels.contains(label)) throw new IllegalStateException("Command already registered. Label: " + label);
        if (registeredCommandLabels.stream().anyMatch(aliases::contains)) throw new IllegalStateException("Command already registered. Label: " + label);

        // add these labels and aliases to check for duplication next time
        registeredCommandLabels.add(label);
        registeredCommandLabels.addAll(aliases);

        // register the command and aliases
        registeredCommands.put(label, this);
        for (String alias : aliases) registeredCommands.put(alias, this);
    }

    public abstract void execute(User user, String[] args, MessageChannel channel);

    public static Command getCommand(String label, CommandContext context) {
        for (Command command : registeredCommands.values()) {
            if (command.getLabel().equals(label)) {
                if (checkContext(command.getContext(), context)) return command;
            } else {
                if (command.getAliases().contains(label)) {
                    if (checkContext(command.getContext(), context)) return command;
                }
            }
        }
        return null;
    }

    private static boolean checkContext(CommandContext givenContext, CommandContext receivedContext) {
        return givenContext == receivedContext || givenContext == CommandContext.BOTH;
    }
}
