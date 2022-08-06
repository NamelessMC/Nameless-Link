package com.namelessmc.bot.http;

import com.google.common.base.Ascii;
import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

public class RoleChange extends HttpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger("RoleChange endpoint");

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

	@Override
	public void service(Request request, Response response) throws IOException {
		Preconditions.checkArgument(request.getMethod() == Method.POST, "Only POST requests allowed");

		response.setContentType("text/plain");

		final JsonObject json;
		final long guildId;
		final long userId;
		final String apiKey;
		final JsonArray roles;
		try {
			json = (JsonObject) JsonParser.parseReader(request.getReader());
			guildId = json.get("guild_id").getAsLong();
			userId = json.get("user_id").getAsLong();
			apiKey = json.get("api_key").getAsString();
			roles = json.getAsJsonArray("roles");
		} catch (JsonSyntaxException | IllegalArgumentException | ClassCastException e) {
			response.getWriter().write("badparameter");
			LOGGER.warn("Received bad role change request from website: invalid json syntax or missing/invalid guild_id, user_id or api_key");
			return;
		}

		if (guildId == 0 || apiKey == null || roles == null) {
			response.getWriter().write("badparameter");
			LOGGER.warn("Received bad role change request from website: zero guild id, null api key, or null roles");
			return;
		}

		Optional<NamelessAPI> optApi;
		try {
			optApi = Main.getConnectionManager().getApiConnection(guildId);
		} catch (final BackendStorageException e) {
			response.getWriter().write("error");
			e.printStackTrace();
			return;
		}

		if (optApi.isEmpty()) {
			response.getWriter().write("notlinked");
			LOGGER.warn("Received bad role change request from website: website is not linked");
			return;
		}

		final NamelessAPI api = optApi.get();

		if (!timingSafeEquals(apiKey.getBytes(), api.apiKey().getBytes())) {
			response.getWriter().write("unauthorized");
			LOGGER.warn("Received bad role change request from website: invalid API key. provided='{}' expected='{}'",
					Ascii.truncate(apiKey, 100, "..."),
					Ascii.truncate(api.apiKey(), 100, "..."));
			return;
		}

		final Guild guild = Main.getJdaForGuild(guildId).getGuildById(guildId);
		if (guild == null) {
			response.getWriter().write("invguild");
			LOGGER.warn("Received bad role change request from website: invalid guild id, guild id = '{}'", guildId);
			return;
		}

		final Member member = guild.retrieveMemberById(userId).complete();

		if (member == null) {
			response.getWriter().write("invuser");
			LOGGER.warn("Received bad role change request from website: invalid user id, guild id = {}, user id = {}", guildId, userId);
			return;
		}

		boolean error = false;

		try {
			for (final JsonElement e : roles) {
				final JsonObject roleObject = e.getAsJsonObject();
				final long roleId = roleObject.get("id").getAsLong();
				final String action = roleObject.get("action").getAsString();
				if (roleId == 0 || action == null) {
					response.getWriter().write("badparameter");
					LOGGER.warn("Received bad role change request from website: missing role id or action");
					return;
				}
				final Role role = guild.getRoleById(roleId);
				if (role == null) {
					error = true;
					continue;
				}
				try {
					if (action.equals("add")) {
						LOGGER.info("Adding role '{}' to member '{}'", role.getName(), member.getUser().getAsTag());
						guild.addRoleToMember(member, role).complete();
					} else if (action.equals("remove")) {
						LOGGER.info("Removing role '{}' from member '{}'", role.getName(), member.getUser().getAsTag());
						guild.removeRoleFromMember(member, role).complete();
					} else {
						LOGGER.warn("Website sent unknown role change action '{}', it was ignored.", action);
					}
				} catch (final HierarchyException | InsufficientPermissionException e2) {
					LOGGER.warn("Cannot process role change: {}", e2.getClass().getSimpleName());
					error = true;
				}
			}
		} catch (JsonSyntaxException | IllegalArgumentException | ClassCastException e) {
			response.getWriter().write("badparameter");
			LOGGER.warn("Received bad role change request from website", e);
			return;
		}

		if (error) {
			response.getWriter().write("partsuccess");
		} else {
			response.getWriter().write("fullsuccess");
		}
	}

}
