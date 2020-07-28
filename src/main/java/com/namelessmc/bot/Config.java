package com.namelessmc.bot;

import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.FileNotFoundException;
import java.io.FileReader;

public class Config {

    public static String get(String subsection, String key) {
        try {
            try {
                return JsonParser.parseReader(new JsonReader(new FileReader("config.json"))).getAsJsonObject().get(subsection).getAsJsonObject().get(key).getAsString();
            } catch (NullPointerException exception) { return null; }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
