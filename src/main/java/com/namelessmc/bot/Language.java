package com.namelessmc.bot;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Language {

    public final String language;

    public Language(String language) {
        this.language = language;
    }

    public String get(String term, Object... replacements) {
        String value = get(language, term);
        if (value != null) {
            return String.format(value, replacements);
        } else {
            String backup_value = get("EnglishUK", term);
            if (backup_value != null) {
                return String.format(backup_value, replacements);
            } else {
                return "Term `" + term + "` is not set.";
            }
        }
    }

    public static final JsonParser jsonParser = new JsonParser();

    private String get(String language, String term) {
        try {
            try {
                return jsonParser.parse(new JsonReader(new FileReader("language.json"))).getAsJsonObject().get(language).getAsJsonObject().get(term).getAsString();
            } catch (NullPointerException e) {
                return null;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "Fatal error getting term: `" + term + "`, using language: `" + language + "` .";
        }
    }

    public static final List<String> languages = new ArrayList<>();

    static {
        try {
            for (Map.Entry<String, JsonElement> lang : jsonParser.parse(new JsonReader(new FileReader("language.json"))).getAsJsonObject().entrySet()) {
                languages.add(lang.getKey());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Main.log("[ERROR] Could not load language file!");
        }
    }

    public static boolean isValid(String language) {
        return languages.contains(language);
    }
}
