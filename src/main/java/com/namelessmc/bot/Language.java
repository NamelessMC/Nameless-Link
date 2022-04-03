package com.namelessmc.bot;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.namelessmc.bot.connections.BackendStorageException;
import com.namelessmc.java_api.*;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class Language {

	private static final Logger LOGGER = LoggerFactory.getLogger("Translation");

	public enum Term {

		/*
		 * After editing this enum please make the corresponding change in resources/languages/en_UK.json (not US!)
		 * Do not modify any other files, missing entries are handled by the language system until they are
		 * translated by contributors.
		 */

		ERROR_GENERIC,
		ERROR_NOT_SET_UP,
		ERROR_NOT_LINKED,
		ERROR_WEBSITE_CONNECTION,
		ERROR_WEBSITE_VERSION("version", "compatibleVersions"),
		ERROR_NO_PERMISSION,
		ERROR_READ_ONLY_STORAGE,
		ERROR_INVALID_USERNAME,
		ERROR_DUPLICATE_USERNAME,
		ERROR_INVALID_EMAIL_ADDRESS,
		ERROR_DUPLICATE_EMAIL_ADDRESS,
		ERROR_SEND_VERIFICATION_EMAIL,
		ERROR_DUPLICATE_DISCORD_INTEGRATION,

		VERIFY_DESCRIPTION,
		VERIFY_OPTION_TOKEN,
		VERIFY_TOKEN_INVALID,
		VERIFY_SUCCESS,

		PING_DESCRIPTION,
		PING_WORKING("time"),

		REGISTER_DESCRIPTION,
		REGISTER_EMAIL,
		REGISTER_URL("url"),

		APIURL_DESCRIPTION,
		APIURL_OPTION_URL,
		APIURL_OPTION_APIKEY,
		APIURL_URL_INVALID,
		APIURL_URL_MALFORMED,
		APIURL_URL_LOCAL,
		APIURL_FAILED_CONNECTION,
		APIURL_ALREADY_USED("command"),
		APIURL_SUCCESS_UPDATED,
		APIURL_SUCCESS_NEW,
		APIURL_UNLINKED,

		GUILD_JOIN_SUCCESS("command"),
		GUILD_JOIN_NEEDS_RENEW("command"),
		GUILD_JOIN_WELCOME_BACK("command"),

		UPDATEUSERNAME_DESCRIPTION,
		UPDATEUSERNAME_SUCCESS,

		;

		private final String[] placeholders;

		Term(final String... placeholders) {
			this.placeholders = placeholders;
		}

		public String[] getPlaceholders() {
			return this.placeholders;
		}

		@Override
		public String toString() {
			return this.name().toLowerCase();
		}

	}

	private static Language defaultLanguage;
	public static Language getDefaultLanguage() { return defaultLanguage; }

	static void setDefaultLanguage(final String languageCode) throws LanguageLoadException {
		defaultLanguage = new Language(languageCode);
	}

	// Avoid having to instantiate new language objects all the time
	private static final Map<String, Language> LANGUAGE_CACHE = new HashMap<>();

	private final String languageCode;
//	public static Language getLanguage() { return language; }

	private transient JsonObject json;

	private Language(final String language) throws LanguageLoadException {
		this.languageCode = Objects.requireNonNull(language, "Language string is null");
		readFromFile();
	}

	private void readFromFile() throws LanguageLoadException {
		try (InputStream stream = Language.class.getResourceAsStream("/languages/" + this.languageCode + ".json")) {
			if (stream == null) {
				throw new LanguageLoadException();
			}

			try (Reader reader = new InputStreamReader(stream)) {
				this.json = JsonParser.parseReader(reader).getAsJsonObject();
			}
		} catch (final IOException e) {
			throw new LanguageLoadException(e);
		}
	}

	public String get(final Term term, final Object... replacements) {
		Objects.requireNonNull(term, "Term is null");
		checkReplacements(term, replacements);

		String translation;
		if (this.json.has(term.toString())) {
			translation = this.json.get(term.toString()).getAsString();
		} else if (this == getDefaultLanguage()) {
			// oh no, cannot fall back to default translation if we are the default translation
			throw new RuntimeException(
					String.format("Term '%s' is missing from default (%s) translation", term, getDefaultLanguage().languageCode));
		} else {
			LOGGER.warn("Language '{}' is missing term '{}', using default ({}) term instead.",
					this.languageCode, term, getDefaultLanguage().languageCode);
			translation = getDefaultLanguage().get(term, replacements);
		}

		for (int i = 0; i < replacements.length; i += 2) {
			final String key = (String) replacements[i];
			final String value = replacements[i + 1].toString();
			translation = translation.replace("{" + key + "}", value);
		}

		if (!checkLength(term, translation.length())) {
			translation = "message too long (bug)";
			LOGGER.warn("Message '{}' too long for language {}", term, this.languageCode);
		}

		return translation;
	}

	private void checkReplacements(final Term term, final Object... replacements) {
		if (replacements == null || replacements.length == 0) {
			return;
		}

		Preconditions.checkArgument(replacements.length % 2 == 0, "Replacements array must have even length");

		final String[] required = term.getPlaceholders();
		final boolean[] valid = new boolean[required.length];

		for (int i = 0; i < replacements.length; i += 2) {
			Preconditions.checkArgument(replacements[i] instanceof String, "Replacement keys must be strings");
			final String key = (String) replacements[i];
			if (Objects.equals(key, required[i / 2])) {
				valid[i / 2] = true;
			} else {
				throw new IllegalArgumentException("Invalid replacement key '" + key + "'");
			}
		}

		for (int i = 0; i < required.length; i++) {
			if (!valid[i]) {
				throw new IllegalArgumentException("Missing replacement key '" + required[i] + "'");
			}
		}
	}

	private boolean checkLength(Term term, int length) {
		int maxLength = switch (term) {
			case VERIFY_DESCRIPTION, APIURL_DESCRIPTION, PING_DESCRIPTION, UPDATEUSERNAME_DESCRIPTION -> 100;
			default -> Integer.MAX_VALUE;
		};
		return length < maxLength;
	}

	public static Language getGuildLanguage(final Guild guild) {
		final Optional<NamelessAPI> api;
		try {
			api = Main.getConnectionManager().getApiConnection(guild.getIdLong());
		} catch (final BackendStorageException e) {
			e.printStackTrace();
			return getDefaultLanguage();
		}

		if (api.isPresent()) {
			try {
				return getLanguage(api.get().getWebsite());
			} catch (final NamelessException e) {
				LOGGER.warn("Cannot retrieve language for guild {}, falling back to default language.", guild.getIdLong());
				return getDefaultLanguage();
			}
		} else {
			return getDefaultLanguage();
		}
	}

	@Deprecated
	public static Language getDiscordUserLanguage(final NamelessAPI api, final User user) {
		Objects.requireNonNull(api, "API is null");
		Objects.requireNonNull(user, "User is null");
		try {
			final Optional<NamelessUser> websiteUser = api.getUserByDiscordId(user.getIdLong());
			if (websiteUser.isPresent()) {
				return getLanguage(websiteUser.get());
			} else {
				Website website = api.getWebsite();
				return getLanguage(website);
			}
		} catch (NamelessException e) {
			Main.logConnectionError(LOGGER, "API request for language failed, using default language", e);
			return getDefaultLanguage();
		}
	}

	public static Language getLanguage(final LanguageEntity languageEntity) throws NamelessException {
		String languageCode;

		languageCode = languageEntity.getLanguagePosix();
		if (languageCode == null) {
			LOGGER.warn("Language code unknown for language '{}', using default language.", languageEntity.getLanguage());
			return getDefaultLanguage();
		}

		Language language = LANGUAGE_CACHE.get(languageCode);
		if (language != null) {
			return language;
		}

		try {
			language = new Language(languageCode);
		} catch (final LanguageLoadException e) {
			LOGGER.error("Failed to load language '{}', falling back to '{}'.", languageCode, getDefaultLanguage().languageCode);
			LOGGER.error("Error loading language", e);
			language = getDefaultLanguage();
		}

		LANGUAGE_CACHE.put(languageCode, language);

		return language;
	}

	public static class LanguageLoadException extends Exception {

		@Serial
		private static final long serialVersionUID = 1335651150585947607L;

		public LanguageLoadException(final Throwable cause) {
			super("Language failed to load", cause);
		}

		public LanguageLoadException() {
			super("Language failed to load");
		}

	}

}
