package com.namelessmc.bot.http;

import java.io.IOException;
import java.util.Optional;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.connections.BackendStorageException;
import com.namelessmc.bot.listeners.DiscordRoleListener;
import com.namelessmc.java_api.NamelessAPI;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.HierarchyException;

public class RoleChange extends HttpServlet {

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
		try {
			json = (JsonObject) JsonParser.parseReader(request.getReader());
			guildId = json.get("guild_id").getAsLong();
			userId = json.get("user_id").getAsLong();
			apiKey = json.get("api_key").getAsString();
		} catch (JsonSyntaxException | IllegalArgumentException e) {
			response.getWriter().write("badparameter");
			return;
		}
		
		if (json == null || guildId == 0 || apiKey == null) {
			response.getWriter().write("badparameter");
			return;
		}
		
		final Guild guild = Main.getJda().getGuildById(guildId);
		if (guild == null) {
			response.getWriter().write("invguild");
			return;
		}
		
		final Member member = guild.getMemberById(userId);
		
		if (member == null) {
			response.getWriter().write("invuser");
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
			return;
		}
		
		final NamelessAPI api = optApi.get();
		
		if (!timingSafeEquals(apiKey.getBytes(), api.getApiKey().getBytes())) {
			response.getWriter().write("unauthorized");
			return;
		}
		
		boolean hierarchyError = false;
		synchronized(DiscordRoleListener.EVENT_LOCK) {
			DiscordRoleListener.temporarilyDisableEvents(userId);
			
			Boolean a;
			Boolean b;
			try {
				a = changeRoles(json, true, member, guild);
			} catch (final HierarchyException e) {
				a = null;
				hierarchyError = true;
			}
			
			try {
				b = changeRoles(json, false, member, guild);
			} catch (final HierarchyException e) {
				b = null;
				hierarchyError = true;
			}
				
			if ((a != null && !a) || (b != null && !b)) {
				response.getWriter().write("invrole");
				return;
			}
		}
		
		if (hierarchyError) {
			response.getWriter().write("hierarchy");
		} else {
			response.getWriter().write("success");
		}
	}

	private Boolean changeRoles(final JsonObject json, final boolean add, final Member member, final Guild guild) {
		final String memberName = add ? "add_role_id" : "remove_role_id";
		if (!json.has(memberName)) {
			return null;
		}
		
		final long roleId;
		try {
			roleId = json.get(memberName).getAsLong();
		} catch (JsonSyntaxException | IllegalArgumentException e) {
			return false;
		}
		
		final Role role = Main.getJda().getRoleById(roleId);
		if (role == null) {
			return false;
		}
		
		System.out.println((add ? "Add " : "Remove ") + " role " + role.getId() + " member " + member.getId() + " guild " + guild.getId());
		
		if (add) {
			guild.addRoleToMember(member, role).queue();
		} else {
			guild.removeRoleFromMember(member, role).queue();
		}
		
		return true;
	}

}
