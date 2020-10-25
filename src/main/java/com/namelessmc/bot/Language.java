package com.namelessmc.bot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.NamelessException;
import com.namelessmc.java_api.NamelessUser;

import lombok.Getter;
import net.dv8tion.jda.api.entities.User;

public class Language {

	public static final String DEFAULT_LANGUAGE = "EnglishUK";
	
    @Getter
    private final String language;

    public Language() {
    	this(DEFAULT_LANGUAGE);
    }
    
    public Language(final String language) {
        this.language = language;
    }

    public String get(final String term, final Object... replacements) {
        final String value = get(this.language, term);
        if (value != null) {
            return String.format(value, replacements);
        } else {
            final String backup_value = get("EnglishUK", term);
            if (backup_value != null) {
                return String.format(backup_value, replacements);
            } else {
                return "Term `" + term + "` is not set.";
            }
        }
    }

    @Getter
    private static final List<String> languages = new ArrayList<>();

    static {
        for (final File file : new File("languages/").listFiles()) {
            languages.add(file.getName().replace(".json", ""));
        }
    }

    public static boolean isValid(final String language) {
        return languages.contains(language);
    }
    
    public static Language getDiscordUserLanguage(final NamelessAPI api, final User user) {
    	try {
	        final Optional<NamelessUser> nameless = api.getUserByDiscordId(user.getIdLong());
	        if (nameless.isPresent()){
	        	return new Language(nameless.get().getLangage());
	        } else {
	        	return new Language(api.getWebsite().getLanguage());
	        }
    	} catch (final NamelessException e) {
    		// If we can't communicate with the website, fall back to english
    		return new Language();
    	}
    }

    private String get(final String language, final String term) {
        try {
            try {
                return JsonParser.parseReader(new JsonReader(new FileReader("languages/" + language + ".json"))).getAsJsonObject().get(language).getAsJsonObject().get(term).getAsString();
            } catch (final NullPointerException e) {
                return null;
            }
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
            return "Fatal error getting term: `" + term + "`, using language: `" + language + "`.";
        }
    }

}
