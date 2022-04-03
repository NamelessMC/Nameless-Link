package com.namelessmc.bot.commands;

import com.namelessmc.bot.Language;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.connections.BackendStorageException;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.NamelessException;
import com.namelessmc.java_api.exception.*;
import com.namelessmc.java_api.integrations.DiscordIntegrationData;
import com.namelessmc.java_api.integrations.IntegrationData;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.Optional;

public class RegisterCommand extends Command {

	RegisterCommand() {
		super("register");
	}

	@Override
	public CommandData getCommandData(Language language) {
		return new CommandData(this.name, language.get(Language.Term.REGISTER_DESCRIPTION));
	}

	@Override
	public void execute(SlashCommandEvent event) {
		final Guild guild = event.getGuild();
		final String username = event.getOption("username").getAsString();
		final String email = event.getOption("email").getAsString();
		final long discordId = event.getUser().getIdLong();
		final String discordTag = event.getUser().getAsTag();

		event.deferReply().queue(hook -> {
			hook.setEphemeral(true);
			Main.getExecutorService().execute(() -> {
				final Language language = Language.getGuildLanguage(guild);

				Optional<NamelessAPI> apiOptional;
				try {
					apiOptional = Main.getConnectionManager().getApiConnection(guild.getIdLong());
				} catch (BackendStorageException e) {
					event.reply(language.get(Language.Term.ERROR_GENERIC)).setEphemeral(true).queue();
					e.printStackTrace();
					return;
				}

				if (apiOptional.isEmpty()) {
					hook.sendMessage(language.get(Language.Term.ERROR_NOT_SET_UP)).queue();
					return;
				}

				NamelessAPI api = apiOptional.get();

				IntegrationData integrationData = new DiscordIntegrationData(discordId, discordTag);
				try {
					Optional<String> verificationUrl = api.registerUser(username, email, integrationData);
					if (verificationUrl.isPresent()) {
						hook.sendMessage(language.get(Language.Term.REGISTER_URL, "url", verificationUrl.get())).queue();
					} else {
						hook.sendMessage(language.get(Language.Term.REGISTER_EMAIL)).queue();
					}
				} catch (NamelessException e) {
					hook.sendMessage(language.get(Language.Term.ERROR_WEBSITE_CONNECTION)).queue();
				} catch (InvalidUsernameException e) {
					hook.sendMessage(language.get(Language.Term.ERROR_INVALID_USERNAME)).queue();
				} catch (UsernameAlreadyExistsException e) {
					hook.sendMessage(language.get(Language.Term.ERROR_DUPLICATE_USERNAME)).queue();
				} catch (CannotSendEmailException e) {
					hook.sendMessage(language.get(Language.Term.ERROR_SEND_VERIFICATION_EMAIL)).queue();
				} catch (IntegrationIdAlreadyExistsException | IntegrationUsernameAlreadyExistsException e) {
					hook.sendMessage(language.get(Language.Term.ERROR_DUPLICATE_DISCORD_INTEGRATION)).queue();
				} catch (InvalidEmailAddressException e) {
					hook.sendMessage(language.get(Language.Term.ERROR_INVALID_EMAIL_ADDRESS)).queue();
				} catch (EmailAlreadyUsedException e) {
					hook.sendMessage(language.get(Language.Term.ERROR_DUPLICATE_EMAIL_ADDRESS)).queue();
				}
			});
		});
	}

}
