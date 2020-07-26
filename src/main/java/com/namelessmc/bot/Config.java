package com.namelessmc.bot;

import com.google.gson.stream.JsonReader;

import java.io.FileNotFoundException;
import java.io.FileReader;

public class Config {

    public static String get(String subsection, String key) {
        try {
            try {
                return Main.getJsonParser().parse(new JsonReader(new FileReader("config.json"))).getAsJsonObject().get(subsection).getAsJsonObject().get(key).getAsString();
            } catch (NullPointerException exception) { return null; }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
