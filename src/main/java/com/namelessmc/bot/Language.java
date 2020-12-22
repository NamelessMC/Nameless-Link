package com.namelessmc.bot;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.Validate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.NamelessException;
import com.namelessmc.java_api.NamelessUser;

import lombok.Getter;
import net.dv8tion.jda.api.entities.User;

public class Language {
	
	private static final String[] EMPTY_STRING_ARRAY = new String[] {};
	
	public static enum Term {
		
		COMMANDS,
		HELP,
		
		ERROR_GENERIC,
		ERROR_NOT_SET_UP,
		ERROR_NOT_LINKED,
		ERROR_WEBSITE_CONNECTION,
		ERROR_NOT_OWNER,
		ERROR_READ_ONLY_STORAGE,
		
		INVALID_COMMAND("commands"),
		
		VERIFY_USAGE("command"),
		VERIFY_TOKEN_INVALID,
		VERIFY_NOT_USED,
		VERIFY_SUCCESS,
		
		APIURL_USAGE("command"),
		APIURL_GUILD_INVALID,
		APIURL_URL_MALFORMED,
		APIURL_NOT_OWNER,
		APIURL_FAILED_CONNECTION,
		APIURL_ALREADY_USED("command"),
		APIURL_SUCCESS_UPDATED,
		APIURL_SUCCESS_NEW,
		
		GUILD_JOIN_SUCCESS("command", "guildId"),
		GUILD_JOIN_NEEDS_RENEW("command", "guildId"),
		GUILD_JOIN_WELCOME_BACK("command", "guildId"),
		
		UNUSED_CONNECTION("discordServerName", "command"),
		
		UNLINK_USAGE("command"),
		UNLINK_GUILD_INVALID,
		
		;
		
		@Getter
		private String[] placeholders;
		
		Term() {
			this.placeholders = EMPTY_STRING_ARRAY;
		}
		
		Term(final String... placeholders) {
			this.placeholders = placeholders;
		}
		
		@Override
		public String toString() {
			return this.name().toLowerCase();
		}
		
	}
	
	private static final Map<String, String> NAMELESS_TO_POSIX = new HashMap<>();
	
	static {
		NAMELESS_TO_POSIX.put("Czech", "cz_CZ");
		NAMELESS_TO_POSIX.put("Greek", "el_GR");
		NAMELESS_TO_POSIX.put("EnglishUK", "en_UK");
		NAMELESS_TO_POSIX.put("EnglishUS", "en_US");
		NAMELESS_TO_POSIX.put("Spanish", "es_419");
		NAMELESS_TO_POSIX.put("SpanishES", "es_ES");
		NAMELESS_TO_POSIX.put("French", "fr_FR");
		NAMELESS_TO_POSIX.put("Italian", "it_IT");
		NAMELESS_TO_POSIX.put("Lithuanian", "lt_LT");
		NAMELESS_TO_POSIX.put("Norwegian", "nb_NO");
		NAMELESS_TO_POSIX.put("Dutch", "nl_NL");
		NAMELESS_TO_POSIX.put("Slovak", "sk_SK");
		NAMELESS_TO_POSIX.put("Chinese(Simplified)", "zh_CN");
	}

	public static final Language DEFAULT;
	static {
		try {
			DEFAULT = new Language("en");
		} catch (final LanguageLoadException e) {
			throw new Error(e);
		}
	}

	// Avoid having to instantiate new language objects all the time
	private static final Map<String, Language> LANGUAGE_CACHE = new HashMap<>();

	@Getter
	private final String language;

	private transient JsonObject json;

	private Language(final String language) throws LanguageLoadException {
		this.language = language;
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
		checkReplacements(term, replacements);
		
		String translation;
		if (this.json.has(term.toString())) {
			translation = this.json.get(term.toString()).getAsString();
		} else if (this == DEFAULT) {
			// oh no, cannot fall back to default translation if we are the default translation
			throw new RuntimeException(
					String.format("Term '%s' is missing from default (%s) translation", term, DEFAULT.language));
		} else {
			Main.getLogger().warning(String.format("Language '%s' is missing term '%s', using default (%s) term instead.",
					this.language, term, DEFAULT.language));
			translation = DEFAULT.get(term, replacements);
		}
		
		for (int i = 0; i < replacements.length; i += 2) {
			final String key = (String) replacements[i];
			final String value = replacements[i + 1].toString();
			translation = translation.replace("{" + key + "}", value);
		}

		return translation;
	}
	
	private void checkReplacements(final Term term, final Object... replacements) {
		if (replacements.length == 0) {
			return;
		}
		
		Validate.isTrue(replacements.length % 2 == 0, "Replacements array must have even length");
		
		final String[] required = term.getPlaceholders();
		final boolean[] valid = new boolean[required.length];
		
		for (int i = 0; i < replacements.length; i += 2) {
			Validate.isTrue(replacements[i] instanceof String, "Replacement keys must be strings");
			final String key = (String) replacements[i];
			if (key == required[i/2]) {
				valid[i/2] = true;
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
		try {
			final Optional<NamelessUser> nameless = api.getUserByDiscordId(user.getIdLong());
			if (nameless.isPresent()) {
				return getLanguage(NAMELESS_TO_POSIX.get(nameless.get().getLangage()));
			} else {
				return getLanguage(NAMELESS_TO_POSIX.get(api.getWebsite().getLanguage()));
			}
		} catch (final NamelessException e) {
			// If we can't communicate with the website, fall back to english
			return DEFAULT;
		}
	}

	public static Language getLanguage(final String languageName) {
		if (languageName == null) {
			return DEFAULT;
		}
		
		Language language = LANGUAGE_CACHE.get(languageName);
		if (language != null) {
			return language;
		}

		try {
			language = new Language(languageName);
		} catch (final LanguageLoadException e) {
			System.err.println(
					"Failed to load language '" + languageName + "', falling back to '" + DEFAULT.language + "'.");
			e.printStackTrace();
			language = DEFAULT;
		}
		
		LANGUAGE_CACHE.put(languageName, language);

		return language;
	}

	private class LanguageLoadException extends Exception {

		private static final long serialVersionUID = 1335651150585947607L;

		public LanguageLoadException(final Throwable cause) {
			super("Language failed to load", cause);
		}

		public LanguageLoadException() {
			super("Language failed to load");
		}

	}

}
