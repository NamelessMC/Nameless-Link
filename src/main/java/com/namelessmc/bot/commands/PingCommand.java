package com.namelessmc.bot.commands;

import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.namelessmc.bot.Language;
import com.namelessmc.bot.Language.Term;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.connections.BackendStorageException;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.NamelessException;
import com.namelessmc.java_api.NamelessVersion;
import com.namelessmc.java_api.Website;
import com.namelessmc.java_api.exception.UnknownNamelessVersionException;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public class PingCommand extends Command {

	private static final Logger LOGGER = LoggerFactory.getLogger("Ping command");

	PingCommand() {
		super("ping");
	}

	@Override
	public CommandData getCommandData(final Language language) {
		return new CommandData(this.name, language.get(Term.PING_DESCRIPTION));
	}

	@Override
	public void execute(final SlashCommandEvent event) {
		final Guild guild = event.getGuild();
		final Language language = Language.getGuildLanguage(guild);

		Main.canModifySettings(event.getUser(), guild, (canModifySettings) -> {
			if (!canModifySettings) {
				event.reply(language.get(Term.ERROR_NO_PERMISSION)).setEphemeral(true).queue();
				return;
			}

			Main.getExecutorService().execute(() -> {
				// Check if API URL works
				Optional<NamelessAPI> optApi;
				try {
					optApi = Main.getConnectionManager().getApi(guild.getIdLong());
				} catch (final BackendStorageException e) {
					event.reply(language.get(Term.ERROR_GENERIC)).setEphemeral(true).queue();
					LOGGER.error("storage backend", e);
					return;
				}

				if (optApi.isEmpty()) {
					event.reply(language.get(Term.ERROR_NOT_SET_UP)).setEphemeral(true).queue();
					return;
				}

				// Now that we actually need to connect to the API, it may take a while
				event.deferReply().setEphemeral(true).queue();

				final NamelessAPI api = optApi.get();

				try {
					final long start = System.currentTimeMillis();
					final Website info = api.getWebsite();
					try {
						if (!Main.SUPPORTED_WEBSITE_VERSIONS.contains(info.getParsedVersion())) {
							final String supportedVersions = Main.SUPPORTED_WEBSITE_VERSIONS.stream().map(NamelessVersion::getName).collect(Collectors.joining(", "));
							event.getHook().sendMessage(language.get(Term.ERROR_WEBSITE_VERSION, "version", info.getVersion(), "compatibleVersions", supportedVersions)).queue();
							return;
						}
					} catch (final UnknownNamelessVersionException e) {
						// API doesn't recognize this version, but we can still display the unparsed name
						final String supportedVersions = Main.SUPPORTED_WEBSITE_VERSIONS.stream().map(NamelessVersion::getName).collect(Collectors.joining(", "));
						event.getHook().sendMessage(language.get(Term.ERROR_WEBSITE_VERSION, "version", info.getVersion(), "compatibleVersions", supportedVersions)).queue();
						return;
					}
					final long time = System.currentTimeMillis() - start;
					event.getHook().sendMessage(language.get(Term.PING_WORKING, "time", time)).queue();
				} catch (final NamelessException e) {
					event.getHook().sendMessage(new MessageBuilder().appendCodeBlock(StringUtils.truncate(e.getMessage(), 1500), "txt").build()).queue();
					event.getHook().sendMessage(language.get(Term.APIURL_FAILED_CONNECTION)).queue();
					Main.logConnectionError(LOGGER, "NamelessException during ping", e);
				}
			});
		});
	}

}
