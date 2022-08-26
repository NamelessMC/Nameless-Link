package com.namelessmc.bot.commands;

import com.google.common.base.Ascii;
import com.namelessmc.bot.Language;
import com.namelessmc.bot.Main;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.NamelessVersion;
import com.namelessmc.java_api.Website;
import com.namelessmc.java_api.exception.NamelessException;
import com.namelessmc.java_api.exception.UnknownNamelessVersionException;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.stream.Collectors;

import static com.namelessmc.bot.Language.Term.*;

public class PingCommand extends Command {

	private static final Logger LOGGER = LoggerFactory.getLogger("Ping command");

	PingCommand() {
		super("ping");
	}

	@Override
	public CommandData getCommandData(final Language language) {
		return new CommandDataImpl(this.name, language.get(PING_DESCRIPTION));
	}

	@Override
	public void execute(final @NotNull SlashCommandInteractionEvent event,
						final @NotNull InteractionHook hook,
						final @NotNull Language language,
						final @NotNull Guild guild,
						final @Nullable NamelessAPI api) {
		Main.canModifySettings(event.getUser(), guild, (canModifySettings) -> {
			if (!canModifySettings) {
				hook.sendMessage(language.get(ERROR_NO_PERMISSION)).queue();
				return;
			}

			if (api == null) {
				hook.sendMessage(language.get(ERROR_NOT_SET_UP)).queue();
				return;
			}

			long ping = checkConnection(api, LOGGER, language, event.getHook());
			if (ping > 0) {
				hook.sendMessage(language.get(PING_WORKING, "time", ping)).queue();
			}
		});
	}

	static long checkConnection(final NamelessAPI api, Logger logger, final Language language, final InteractionHook hook) {
		URL url = api.apiUrl();
		if (!url.getProtocol().equals("http") && !url.getProtocol().equals("https") ||
				!url.getPath().endsWith("/index.php") ||
				!url.getQuery().equals("route=/api/v2") && !url.getQuery().equals("route=/api/v2/")
		) {
			logger.info("Invalid URL with protocol '{}' host '{}' path '{}' query '{}'", url.getProtocol(), url.getHost(), url.getPath(), url.getQuery());
			hook.sendMessage(language.get(APIURL_URL_INVALID)).queue();
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
			logger.info("Local host: '{}'", host);
			hook.sendMessage(language.get(APIURL_URL_LOCAL)).queue();
			return -1;
		}

		try {
			final long start = System.currentTimeMillis();
			logger.info("Making request to info endpoint");
			final Website info = api.website();
			try {
				if (!NamelessVersion.isSupportedByJavaApi(info.parsedVersion())) {
					hook.sendMessage(language.get(ERROR_WEBSITE_VERSION, "version", info.rawVersion(), "compatibleVersions", supportedVersionsList())).queue();
					logger.info("Incompatible NamelessMC version");
					return -1;
				}

				logger.info("Website connection is working");
				return System.currentTimeMillis() - start;
			} catch (UnknownNamelessVersionException e) {
				hook.sendMessage(language.get(ERROR_WEBSITE_VERSION, "version", info.rawVersion(), "compatibleVersions", supportedVersionsList())).queue();
				Main.logConnectionError(logger, "unknown nameless version", e);
				return -1;
			}


		} catch (final NamelessException e) {
			hook.sendMessage("```\n" + Ascii.truncate(e.getMessage(), 1500, "[truncated]") + "\n```").queue();
			hook.sendMessage(language.get(APIURL_FAILED_CONNECTION)).queue();
			Main.logConnectionError(logger, "NamelessException during ping", e);
			return -1;
		}
	}

	public static String supportedVersionsList() {
		return NamelessVersion.supportedVersions().stream().map(NamelessVersion::friendlyName).collect(Collectors.joining(", "));
	}

}
