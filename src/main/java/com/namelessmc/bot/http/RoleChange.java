package com.namelessmc.bot.http;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.connections.BackendStorageException;
import com.namelessmc.java_api.NamelessAPI;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

public class RoleChange extends HttpServlet {

	private static final Logger LOGGER = LoggerFactory.getLogger("RoleChange endpoint");

	private static final long serialVersionUID = 1L;

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
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
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
			LOGGER.warn("Received bad role change request from website");
			return;
		}

		if (guildId == 0 || apiKey == null || roles == null) {
			response.getWriter().write("badparameter");
			LOGGER.warn("Received bad role change request from website");
			return;
		}

		Optional<NamelessAPI> optApi;
		try {
			optApi = Main.getConnectionManager().getApi(guildId);
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

		if (!timingSafeEquals(apiKey.getBytes(), api.getApiKey().getBytes())) {
			response.getWriter().write("unauthorized");
			LOGGER.warn("Received bad role change request from website: invalid API key");
			return;
		}

		final Guild guild = Main.getJda().getGuildById(guildId);
		if (guild == null) {
			response.getWriter().write("invguild");
			LOGGER.warn("Received bad role change request from website: invalid guild id, guild id = '{}'", guildId);
			return;
		}

		guild.retrieveMemberById(userId).queue(member -> {
			try {
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
							LOGGER.warn("Received bad role change request from website");
							return;
						}
						final Role role = guild.getRoleById(roleId);
						if (role == null) {
							error = true;
							continue;
						}
						try {
							if (action.equals("add")) {
								guild.addRoleToMember(member, role).complete();
							} else if (action.equals("remove")) {
								guild.removeRoleFromMember(member, role).complete();
							} else {
								LOGGER.warn("Website sent unknown role change action '{}', it was ignored.", action);
							}
						} catch (final HierarchyException | InsufficientPermissionException ignored) {
							LOGGER.warn("Cannot process role change: {}", ignored.getClass().getSimpleName());
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
					LOGGER.warn("Role change request from website processed partly successfully.");
				} else {
					response.getWriter().write("fullsuccess");
					LOGGER.info("Role change request from website processed successfully.");
				}
			} catch (final IOException exception) {
				// An IOException at getWriter normally indicates an internal server error.
				response.setStatus(500);
				LOGGER.error("IOException", exception);
			}
		});
	}

}
