package com.namelessmc.bot.commands;

import com.namelessmc.bot.Language;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.NamelessException;
import com.namelessmc.java_api.exception.*;
import com.namelessmc.java_api.integrations.DiscordIntegrationData;
import com.namelessmc.java_api.integrations.IntegrationData;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static com.namelessmc.bot.Language.Term.*;

public class RegisterCommand extends Command {

	RegisterCommand() {
		super("register");
	}

	@Override
	public CommandData getCommandData(Language language) {
		return new CommandData(this.name, language.get(REGISTER_DESCRIPTION))
				.addOption(OptionType.STRING, "username", language.get(REGISTER_OPTION_USERNAME), true)
				.addOption(OptionType.STRING, "email", language.get(REGISTER_OPTION_EMAIL), true);
	}

	@Override
	public void execute(final @NotNull SlashCommandEvent event,
						final @NotNull InteractionHook hook,
						final @NotNull Language language,
						final @NotNull Guild guild,
						final @Nullable NamelessAPI api) {
		final String username = event.getOption("username").getAsString();
		final String email = event.getOption("email").getAsString();
		final long discordId = event.getUser().getIdLong();
		final String discordTag = event.getUser().getAsTag();

		if (api == null) {
			hook.sendMessage(language.get(ERROR_NOT_SET_UP)).queue();
			return;
		}

		IntegrationData integrationData = new DiscordIntegrationData(discordId, discordTag);
		try {
			Optional<String> verificationUrl = api.registerUser(username, email, integrationData);
			if (verificationUrl.isPresent()) {
				hook.sendMessage(language.get(REGISTER_URL, "url", verificationUrl.get())).queue();
			} else {
				hook.sendMessage(language.get(REGISTER_EMAIL)).queue();
			}
		} catch (NamelessException e) {
			hook.sendMessage(language.get(ERROR_WEBSITE_CONNECTION)).queue();
		} catch (InvalidUsernameException e) {
			hook.sendMessage(language.get(ERROR_INVALID_USERNAME)).queue();
		} catch (UsernameAlreadyExistsException e) {
			hook.sendMessage(language.get(ERROR_DUPLICATE_USERNAME)).queue();
		} catch (CannotSendEmailException e) {
			hook.sendMessage(language.get(ERROR_SEND_VERIFICATION_EMAIL)).queue();
		} catch (IntegrationIdentifierInvalidException | IntegrationUsernameInvalidException e) {
			hook.sendMessage(language.get(ERROR_DUPLICATE_DISCORD_INTEGRATION)).queue();
		} catch (InvalidEmailAddressException e) {
			hook.sendMessage(language.get(ERROR_INVALID_EMAIL_ADDRESS)).queue();
		} catch (EmailAlreadyUsedException e) {
			hook.sendMessage(language.get(ERROR_DUPLICATE_EMAIL_ADDRESS)).queue();
		}
	}

}
