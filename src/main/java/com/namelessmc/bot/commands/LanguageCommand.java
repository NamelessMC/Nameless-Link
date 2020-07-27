package com.namelessmc.bot.commands;

import com.namelessmc.bot.Language;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.Queries;
import com.namelessmc.bot.Utils;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

import java.awt.Color;

public class LanguageCommand extends Command {

    public LanguageCommand() {
        super("!language", new String[]{"!lang"}, CommandContext.PRIVATE_MESSAGE);
    }

    public static void execute(User user, String[] args, MessageChannel channel) {
        Language language = Queries.getUserLanguage(user.getId());
        if (language == null) language = new Language("EnglishUK");

        if (args.length == 3) {
            if (args[1].equals("set")) {
                if (Language.isValid(args[2])) {
                    if (Queries.setUserLanguage(user.getId(), args[2])) {
                        language = new Language(args[2]);
                        Main.getEmbedBuilder().clear().setColor(Color.GREEN).setTitle(language.get("language_title")).addField(language.get("success"), language.get("language_update_success", args[2]), false);
                        Main.log("Updated " + user.getName() + "'s language to: " + args[2]);
                    } else {
                        Main.getEmbedBuilder().clear().setColor(Color.RED).setTitle(language.get("language_title")).addField(language.get("failed"), language.get("language_update_failed_db"), false);
                        Main.log("[ERROR] Failed to update language for " + user.getName() + " to: " + args[2] + ". Could not remove from DB");
                    }
                } else Main.getEmbedBuilder().clear().setColor(Color.RED).setTitle(language.get("language_title")).addField(language.get("failed"), language.get("language_update_failed_invalid", args[2], Utils.listToString(Language.getLanguages(), ", ")), false);
            } else Main.getEmbedBuilder().clear().setColor(Color.RED).setTitle(language.get("language_title")).addField(language.get("failed"), language.get("language_invalid_argument", args[1]), false);
        } else if (args.length == 2) {
            if (args[1].equals("set") || args[1].equals("list")) {
                Main.getEmbedBuilder().clear().setColor(Color.ORANGE).setTitle(language.get("language_title")).addField(language.get("settings"), language.get("language_list", Utils.listToString(Language.getLanguages(), ", ")), false);
            } else Main.getEmbedBuilder().clear().setColor(Color.RED).setTitle(language.get("language_title")).addField(language.get("failed"), language.get("language_invalid_argument", args[1]), false);
        } else if (args.length == 1) {
            if (Queries.getUserLanguage(user.getId()) != null) {
                Main.getEmbedBuilder().clear().setColor(Color.GREEN).setTitle(language.get("language_title")).addField(language.get("settings"), language.get("language_get", language.getLanguage()), false);
            } else {
                Main.getEmbedBuilder().clear().setColor(Color.RED).setTitle(language.get("language_title")).addField(language.get("failed"), language.get("language_get_failed_db"), false);
                Main.log("[ERROR] Failed to get language for " + user.getName() + ". Could not find from DB");
            }
        } else {
            Main.getEmbedBuilder().clear().setColor(Color.ORANGE).setThumbnail(language.get("language_title")).addField(language.get("settings"), language.get("language_subcommands"), false);
        }
        channel.sendMessage(Main.getEmbedBuilder().build()).queue();
    }
}
