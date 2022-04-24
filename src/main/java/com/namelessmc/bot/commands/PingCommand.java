package com.namelessmc.bot.commands;

import com.google.common.base.Ascii;
import com.namelessmc.bot.Language;
import com.namelessmc.bot.Language.Term;
import com.namelessmc.bot.Main;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.NamelessException;
import com.namelessmc.java_api.NamelessVersion;
import com.namelessmc.java_api.Website;
import com.namelessmc.java_api.exception.UnknownNamelessVersionException;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.stream.Collectors;

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
	public void execute(final @NotNull SlashCommandEvent event,
						final @NotNull InteractionHook hook,
						final @NotNull Language language,
						final @NotNull Guild guild,
						final @Nullable NamelessAPI api) {
		Main.canModifySettings(event.getUser(), guild, (canModifySettings) -> {
			if (!canModifySettings) {
				hook.sendMessage(language.get(Term.ERROR_NO_PERMISSION)).queue();
				return;
			}

			if (api == null) {
				hook.sendMessage(language.get(Term.ERROR_NOT_SET_UP)).queue();
				return;
			}

			long ping = checkConnection(api, LOGGER, language, event.getHook());
			if (ping > 0) {
				hook.sendMessage(language.get(Term.PING_WORKING, "time", ping)).queue();
			}
		});
	}

	static long checkConnection(final NamelessAPI api, Logger logger, final Language language, final InteractionHook hook) {
		URL url = api.getApiUrl();
		if (!url.getProtocol().equals("http") && !url.getProtocol().equals("https") ||
				!url.getPath().endsWith("/index.php") ||
				!url.getQuery().equals("route=/api/v2")
		) {
			LOGGER.info("Invalid URL with protocol '{}' host '{}' path '{}' query '{}'", url.getProtocol(), url.getHost(), url.getPath(), url.getQuery());
			hook.sendMessage(language.get(Term.APIURL_URL_INVALID)).queue();
			return -1;
		}

		String host = url.getHost();
		if (!Main.isLocalAllowed() && (
				host.equals("localhost") ||
				host.startsWith("127.") ||
				host.startsWith("192.168.") ||
				host.startsWith("10.")
				// checking 172.16.0.0/12 is too much work...
			)) {
			LOGGER.info("Local host: '{}'", host);
			hook.sendMessage(language.get(Term.APIURL_URL_LOCAL)).queue();
			return -1;
		}

		try {
			final long start = System.currentTimeMillis();
			final Website info = api.getWebsite();
			try {
				if (!NamelessVersion.isSupportedByJavaApi(info.getParsedVersion())) {
					hook.sendMessage(language.get(Term.ERROR_WEBSITE_VERSION, "version", info.getVersion(), "compatibleVersions", supportedVersionsList())).queue();
					LOGGER.info("Incompatible NamelessMC version");
					return -1;
				}
			} catch (final UnknownNamelessVersionException e) {
				// API doesn't recognize this version, but we can still display the unparsed name
				hook.sendMessage(language.get(Term.ERROR_WEBSITE_VERSION, "version", info.getVersion(), "compatibleVersions", supportedVersionsList())).queue();
				LOGGER.info("Unknown NamelessMC version");
				return -1;
			}
			return System.currentTimeMillis() - start;
		} catch (final NamelessException e) {
			hook.sendMessage(new MessageBuilder().appendCodeBlock(Ascii.truncate(e.getMessage(), 1500, "[truncated]"), "txt").build()).queue();
			hook.sendMessage(language.get(Term.APIURL_FAILED_CONNECTION)).queue();
			Main.logConnectionError(logger, "NamelessException during ping", e);
			return -1;
		}
	}

	public static String supportedVersionsList() {
		return NamelessVersion.getSupportedVersions().stream().map(NamelessVersion::getName).collect(Collectors.joining(", "));
	}

}
