package com.namelessmc.bot;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.NamelessException;
import com.namelessmc.java_api.NamelessUser;

import net.dv8tion.jda.api.entities.User;

public class Language {

	private static final Logger LOGGER = LoggerFactory.getLogger("Translation");

	public enum Term {

		HELP_TITLE,
		HELP_COMMANDS_TITLE,
		HELP_COMMANDS_CONTENT("commands"),
		HELP_CONTEXT_TITLE,
		HELP_CONTEXT_CONTENT,

		ERROR_GENERIC,
		ERROR_NOT_SET_UP,
		ERROR_NOT_LINKED,
		ERROR_WEBSITE_CONNECTION,
		ERROR_WEBSITE_VERSION("version", "compatibleVersions"),
		ERROR_NO_PERMISSION,
		ERROR_READ_ONLY_STORAGE,
		ERROR_GUILD_ID_INVALID,
		ERROR_GUILD_UNKNOWN,

		VERIFY_USAGE("command"),
		VERIFY_TOKEN_INVALID,
		VERIFY_NOT_USED,
		VERIFY_SUCCESS,

		PING_USAGE("command"),
		PING_WORKING("time"),

		APIURL_USAGE("command"),
		APIURL_URL_INVALID,
		APIURL_URL_MALFORMED,
		APIURL_FAILED_CONNECTION,
		APIURL_ALREADY_USED("command"),
		APIURL_TRY_HTTPS,
		APIURL_SUCCESS_UPDATED,
		APIURL_SUCCESS_NEW,

		GUILD_JOIN_SUCCESS("command", "guildId"),
		GUILD_JOIN_NEEDS_RENEW("command", "guildId"),
		GUILD_JOIN_WELCOME_BACK("command", "guildId"),

		UNLINK_USAGE("command"),
		UNLINK_NOT_LINKED,

		PREFIX_USAGE("command"),
		PREFIX_SUCCESS("newPrefix"),

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

	private static final Map<String, String> NAMELESS_TO_POSIX = new HashMap<>();

	static {
		NAMELESS_TO_POSIX.put("Czech", "cs_CZ");
		NAMELESS_TO_POSIX.put("German", "de_DE");
		NAMELESS_TO_POSIX.put("Greek", "el_GR");
		NAMELESS_TO_POSIX.put("EnglishUK", "en_UK");
		NAMELESS_TO_POSIX.put("EnglishUS", "en_US");
		NAMELESS_TO_POSIX.put("Spanish", "es_419");
		NAMELESS_TO_POSIX.put("SpanishES", "es_ES");
		NAMELESS_TO_POSIX.put("French", "fr_FR");
		NAMELESS_TO_POSIX.put("Hungarian", "hu_HU");
		NAMELESS_TO_POSIX.put("Italian", "it_IT");
		NAMELESS_TO_POSIX.put("Lithuanian", "lt_LT");
		NAMELESS_TO_POSIX.put("Norwegian", "nb_NO");
		NAMELESS_TO_POSIX.put("Dutch", "nl_NL");
		NAMELESS_TO_POSIX.put("Polish", "pl_PL");
		NAMELESS_TO_POSIX.put("Romanian", "ro_RO");
		NAMELESS_TO_POSIX.put("Russian", "ru_RU");
		NAMELESS_TO_POSIX.put("Slovak", "sk_SK");
		NAMELESS_TO_POSIX.put("Turkish", "tr_TR");
		NAMELESS_TO_POSIX.put("Chinese(Simplified)", "zh_CN");
	}

	private static Language defaultLanguage;
	public static Language getDefaultLanguage() { return defaultLanguage; }

	static void setDefaultLanguage(final String languageCode) throws LanguageLoadException {
		defaultLanguage = new Language(languageCode);
	}

	// Avoid having to instantiate new language objects all the time
	private static final Map<String, Language> LANGUAGE_CACHE = new HashMap<>();

	private final String language;
//	public static Language getLanguage() { return language; }

	private transient JsonObject json;

	private Language(final String language) throws LanguageLoadException {
		this.language = Objects.requireNonNull(language, "Language string is null");;
		readFromFile();
	}

	private void readFromFile() throws LanguageLoadException {
		try (InputStream stream = Language.class.getResourceAsStream("/languages/" + this.language + ".json")) {
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
					String.format("Term '%s' is missing from default (%s) translation", term, getDefaultLanguage().language));
		} else {
			LOGGER.warn("Language '{}' is missing term '{}', using default ({}) term instead.",
					this.language, term, getDefaultLanguage().language);
			translation = getDefaultLanguage().get(term, replacements);
		}

		for (int i = 0; i < replacements.length; i += 2) {
			final String key = (String) replacements[i];
			final String value = replacements[i + 1].toString();
			translation = translation.replace("{" + key + "}", value);
		}

		return translation;
	}

	private void checkReplacements(final Term term, final Object... replacements) {
		if (replacements == null || replacements.length == 0) {
			return;
		}

		Validate.isTrue(replacements.length % 2 == 0, "Replacements array must have even length");

		final String[] required = term.getPlaceholders();
		final boolean[] valid = new boolean[required.length];

		for (int i = 0; i < replacements.length; i += 2) {
			Validate.isTrue(replacements[i] instanceof String, "Replacement keys must be strings");
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

	public static Language getDiscordUserLanguage(final NamelessAPI api, final User user) {
		Objects.requireNonNull(api, "API is null");
		Objects.requireNonNull(user, "User is null");
		try {
			final Optional<NamelessUser> nameless = api.getUserByDiscordId(user.getIdLong());
			if (nameless.isPresent()) {
				return getLanguage(NAMELESS_TO_POSIX.get(nameless.get().getLangage()));
			} else {
				return getLanguage(NAMELESS_TO_POSIX.get(api.getWebsite().getLanguage()));
			}
		} catch (final NamelessException e) {
			// If we can't communicate with the website, fall back to english
			return getDefaultLanguage();
		}
	}

	public static Language getLanguage(final String languageName) {
		if (languageName == null) {
			return getDefaultLanguage();
		}

		Language language = LANGUAGE_CACHE.get(languageName);
		if (language != null) {
			return language;
		}

		try {
			language = new Language(languageName);
		} catch (final LanguageLoadException e) {
			LOGGER.error("Failed to load language '{}', falling back to '{}'.", e, languageName, getDefaultLanguage().language);
			language = getDefaultLanguage();
		}

		LANGUAGE_CACHE.put(languageName, language);

		return language;
	}

	public static class LanguageLoadException extends Exception {

		private static final long serialVersionUID = 1335651150585947607L;

		public LanguageLoadException(final Throwable cause) {
			super("Language failed to load", cause);
		}

		public LanguageLoadException() {
			super("Language failed to load");
		}

	}

}
