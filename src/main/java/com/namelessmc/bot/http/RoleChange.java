package com.namelessmc.bot.http;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.namelessmc.bot.Main;
import com.namelessmc.bot.connections.BackendStorageException;
import com.namelessmc.bot.listeners.DiscordRoleListener;
import com.namelessmc.java_api.NamelessAPI;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public class RoleChange extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
		// TODO Check if parameters exist
		// TODO Catch long parse exceptions
		final long guildId = Long.parseLong(request.getParameter("guild_id"));
		final Guild guild = Main.getJda().getGuildById(guildId);
		final Member member = guild.getMemberById(Long.parseLong(request.getParameter("id")));
		DiscordRoleListener.getRecentlyEdited().add(member);
		final String apiUrl = request.getParameter("api_url");

		String htmlResponse;

		Optional<NamelessAPI> api;
		try {
			api = Main.getConnectionManager().getApi(guildId);
		} catch (final BackendStorageException e) {
			e.printStackTrace(); // TODO handle
			return;
		}

		if (!guild.getMemberById(Main.getJda().getSelfUser().getId()).canInteract(member) || api.isEmpty()) {
			Main.log("Cannot interact with " + member.getEffectiveName() + " in " + guild.getName());
			htmlResponse = "failure-cannot-interact";
		} else if (!apiUrl.equals(api.get().getApiUrl().toString())) {
			// TODO Why this check?
			Main.log("Invalid Guild API URL sent for " + member.getEffectiveName() + " in " + guild.getName());
			htmlResponse = "failure-invalid-api-url";
		} else {
			try {
				final String newRoleId = request.getParameter("role").toString();
				final Role newRole = guild.getRoleById(newRoleId);
				if (newRole != null) {
					guild.addRoleToMember(member.getId(), newRole).complete();
				}
			} catch (NullPointerException | NumberFormatException ignored) {
			} // TODO don't ignore
			try {
				final String oldRoleId = request.getParameter("oldRole").toString();
				final Role oldRole = guild.getRoleById(oldRoleId);
				if (oldRole != null) {
					guild.removeRoleFromMember(member.getId(), oldRole).complete();
				}
			} catch (NullPointerException | NumberFormatException ignored) {
			} // TODO don't ignore
			Main.log("Processed role update (Website -> Discord) for " + member.getEffectiveName() + ".");
			htmlResponse = "success";
		}

		response.setContentLength(htmlResponse.length());
		response.setContentType("text/plain");
		try (OutputStream stream = response.getOutputStream()) {
			stream.write(htmlResponse.getBytes());
		}

		DiscordRoleListener.getRecentlyEdited().remove(member);
	}

}
