package com.namelessmc.bot.http;

import com.google.common.base.Ascii;
import com.google.gson.*;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.connections.BackendStorageException;
import com.namelessmc.java_api.NamelessAPI;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class ApplyRoleChanges extends HttpHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("ApplyRoleChanges endpoint");

    private static boolean timingSafeEquals(final byte[] a, final byte[] b) {
        if (a.length != b.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    private static void sendJsonResponse(JsonObject jsonObject, Response response) throws IOException {
        response.setContentType("application/json");
        try (JsonWriter writer = new JsonWriter(response.getWriter())) {
            Streams.write(jsonObject, writer);
        }
    }

    @Override
    public void service(Request request, Response response) throws Exception {
        if (request.getMethod() != Method.POST) {
            response.setStatus(HttpStatus.METHOD_NOT_ALLOWED_405);
            return;
        }

        JsonObject responseJson = new JsonObject();

        final JsonObject json;
        final long guildId;
        final String apiKey;
        final JsonArray roles;
        try {
            json = (JsonObject) JsonParser.parseReader(request.getReader());
            guildId = Long.parseLong(json.get("guild_id").getAsString());
            apiKey = json.get("api_key").getAsString();
            roles = json.getAsJsonArray("role_changes");
        } catch (JsonSyntaxException | IllegalArgumentException | ClassCastException e) {
            responseJson.addProperty("status", "bad_request");
            responseJson.addProperty("meta", e.getClass().getSimpleName());
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            sendJsonResponse(responseJson, response);
            LOGGER.warn("Received bad role change request from website: invalid json syntax or missing/invalid guild_id, user_id or api_key");
            return;
        }

        if (apiKey == null || roles == null) {
            responseJson.addProperty("status", "bad_request");
            responseJson.addProperty("meta", "null api key or null roles");
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            sendJsonResponse(responseJson, response);
            LOGGER.warn("Received bad role change request from website: zero guild id, null api key, or null roles");
            return;
        }

        final Optional<NamelessAPI> optApi;
        try {
            optApi = Main.getConnectionManager().getApiConnection(guildId);
        } catch (final BackendStorageException e) {
            response.getWriter().write("error");
            e.printStackTrace();
            return;
        }

        if (optApi.isEmpty()) {
            responseJson.addProperty("status", "not_linked");
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            sendJsonResponse(responseJson, response);
            LOGGER.warn("Received bad role change request from website: website is not linked");
            return;
        }

        final NamelessAPI api = optApi.get();

        if (!timingSafeEquals(apiKey.getBytes(), api.apiKey().getBytes())) {
            responseJson.addProperty("status", "unauthorized");
            responseJson.addProperty("meta", "Invalid API key");
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
            sendJsonResponse(responseJson, response);
            LOGGER.warn("Received bad role change request from website: invalid API key. provided='{}' expected='{}'",
                    Ascii.truncate(apiKey, 100, "..."),
                    Ascii.truncate(api.apiKey(), 100, "..."));
            return;
        }

        final Guild guild = Main.getJdaForGuild(guildId).getGuildById(guildId);
        if (guild == null) {
            responseJson.addProperty("status", "invalid_guild");
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            sendJsonResponse(responseJson, response);
            LOGGER.warn("Received bad role change request from website: invalid guild id, guild id = '{}'", guildId);
            return;
        }

        final JsonArray roleResponses = new JsonArray(roles.size());

        for (final JsonElement roleElem : roles) {
            final JsonObject roleObj = roleElem.getAsJsonObject();

            final long userId = Long.parseLong(roleObj.get("user_id").getAsString());
            final long roleId = Long.parseLong(roleObj.get("role_id").getAsString());
            final String action = roleObj.get("action").getAsString();

            final Member member = guild.getMemberById(userId);
            final Role role = guild.getRoleById(roleId);

            String status;
            if (member == null) {
                status = "invalid_user";
            } else if (role == null) {
                status = "invalid_role";
            } else {
                final List<Role> currentRoles = member.getRoles();
                try {
                    if (action.equals("add")) {
                        if (currentRoles.contains(role)) {
                            status = "none";
                            LOGGER.info("Member '{}' already has role '{}'", member.getUser().getAsTag(), role.getName());
                        } else {
                            LOGGER.info("Adding role '{}' to member '{}'", role.getName(), member.getUser().getAsTag());
                            status = "added";
                            guild.addRoleToMember(member, role).complete();
                        }
                    } else if (action.equals("remove")) {
                        if (!currentRoles.contains(role)) {
                            status = "none";
                            LOGGER.info("Member '{}' already doesn't have role '{}'", member.getUser().getAsTag(), role.getName());
                        } else {
                            LOGGER.info("Removing role '{}' from member '{}'", role.getName(), member.getUser().getAsTag());
                            status = "removed";
                            guild.removeRoleFromMember(member, role).complete();
                        }
                    } else {
                        response.setStatus(HttpStatus.BAD_REQUEST_400);
                        responseJson.addProperty("status", "bad_request");
                        responseJson.addProperty("meta", "invalid role change action: " + action);
                        sendJsonResponse(responseJson, response);
                        return;
                    }
                } catch (final HierarchyException | InsufficientPermissionException e2) {
                    LOGGER.warn("Cannot process role change: {}", e2.getClass().getSimpleName());
                    status = "no_permission";
                }
            }

            final JsonObject roleResponse = new JsonObject();
            roleResponse.addProperty("status", status);
            roleResponse.addProperty("user_id", String.valueOf(userId));
            roleResponse.addProperty("role_id", String.valueOf(roleId));
            roleResponses.add(roleResponse);
        }

        responseJson.addProperty("status", "success");
        responseJson.add("role_changes", roleResponses);
        sendJsonResponse(responseJson, response);
    }
}
