package com.namelessmc.bot;

import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;

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

    private String get(String language, String term) {
        try {
            JsonParser jsonParser = new JsonParser();
            try {
                return jsonParser.parse(new JsonReader(new FileReader("./language.json"))).getAsJsonObject().get(language).getAsJsonObject().get(term).getAsString();
            } catch (NullPointerException e) {
                return null;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "Fatal error getting term: `" + term + "`, using language: `" + language + "` .";
        }
    }

    public static final List<String> languages = Arrays.asList("EnglishUK", "Chinese-Simplified", "Swedish");

    public static boolean isValid(String language) {
        return languages.contains(language);
    }
}
