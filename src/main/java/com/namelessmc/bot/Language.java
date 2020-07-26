package com.namelessmc.bot;

import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import lombok.Getter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class Language {

    @Getter
    private final String language;

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

    @Getter
    private static final List<String> languages = new ArrayList<>();

    static {
        for (File file : new File("languages/").listFiles()) {
            languages.add(file.getName().replace(".json", ""));
        }
    }

    public static boolean isValid(String language) {
        return languages.contains(language);
    }

    private String get(String language, String term) {
        try {
            try {
                return Main.getJsonParser().parse(new JsonReader(new FileReader("languages/" + language + ".json"))).getAsJsonObject().get(language).getAsJsonObject().get(term).getAsString();
            } catch (NullPointerException e) {
                return null;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "Fatal error getting term: `" + term + "`, using language: `" + language + "`.";
        }
    }
}
