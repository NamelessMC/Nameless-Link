package com.namelessmc.bot;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.NamelessException;
import com.namelessmc.java_api.NamelessUser;

import lombok.Getter;
import net.dv8tion.jda.api.entities.User;

public class Language {

	public static final Language DEFAULT;
	static {
		try {
			DEFAULT = new Language("EnglishUK");
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
		try (InputStream stream = Language.class.getResourceAsStream("languages/" + this.language + ".json")) {
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

	public String get(final String term, final Object... replacements) {
        String translation;
    	if (this.json.has(term)) {
        	translation = this.json.get(term).getAsString();
        } else if (this == DEFAULT) {
        	// oh no
        	throw new RuntimeException(String.format("Term '%s' is missing from default (%s) translation", term, DEFAULT.language));
        } else {
        	System.err.println(String.format("Language '%s' is missing term '%s', using default (%s) term instead.", this.language, term, DEFAULT.language));
        	return DEFAULT.get(term, replacements);
        }
    	
    	return String.format(translation, replacements);
	}

	public static Language getDiscordUserLanguage(final NamelessAPI api, final User user) {
    	try {
	        final Optional<NamelessUser> nameless = api.getUserByDiscordId(user.getIdLong());
	        if (nameless.isPresent()){
	        	return getLanguage(nameless.get().getLangage());
	        } else {
	        	return getLanguage(api.getWebsite().getLanguage());
	        }
    	} catch (final NamelessException e) {
    		// If we can't communicate with the website, fall back to english
    		return DEFAULT;
    	}
	}

	public static Language getLanguage(final String languageName) {
		Language language = LANGUAGE_CACHE.get(languageName);
		if (language != null) {
			return language;
		}
		
    	try {
			language = new Language(languageName);
		} catch (final LanguageLoadException e) {
			System.err.println("Failed to load language '" + languageName + "', falling back to '" + DEFAULT.language + "'.");
			e.printStackTrace();
			language = DEFAULT;
		}
    	
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
