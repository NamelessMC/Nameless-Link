package com.namelessmc.bot.util;

import java.io.IOException;

import org.glassfish.grizzly.http.server.Response;

import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;

public class Util {
	
    public static boolean timingSafeEquals(final byte[] a, final byte[] b) {
        if (a.length != b.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    public static void sendJsonResponse(JsonObject jsonObject, Response response) throws IOException {
        response.setContentType("application/json");
        try (JsonWriter writer = new JsonWriter(response.getWriter())) {
            Streams.write(jsonObject, writer);
        }
    }
    
}
