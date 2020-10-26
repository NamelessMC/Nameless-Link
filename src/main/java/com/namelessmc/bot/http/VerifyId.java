package com.namelessmc.bot.http;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.namelessmc.bot.Main;
import com.namelessmc.bot.Queries;
import com.namelessmc.bot.Utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

public class VerifyId extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
		final String requestUri = request.getRequestURI();

		final String id = request.getParameter("id"); // TODO Check if param exists

		final User user = Main.getJda().getUserById(id);

		final String token = request.getParameter("token"); // TODO Check if param exists

		final String guild_id = request.getParameter("guild_id"); // TODO Check if param exists
		final Guild guild = Main.getJda().getGuildById(guild_id);

		String role = null;
		try {
			role = request.getParameter("role"); // TODO Check if param exists
			role = role.substring(1, role.length() - 1);
		} catch (final NullPointerException ignored) {
		}
		final String site = request.getParameter("site"); // TODO Check if param exists

		String htmlResponse;

		if (user == null) {
			Main.debug("[ERROR] Invalid Discord ID while processing a web account (" + id + ")");
			htmlResponse = "failure-invalid-id";
		} else if (guild == null) {
			Main.debug("[ERROR] Invalid Guild ID while processing a web account (" + guild_id + ")");
			htmlResponse = "failure-invalid-guild-id";
		} else if (Queries.getPendingVerification(id) != null) {
			Main.debug("[ERROR] Token " + token + " with ID " + id + " already has a pending verification.");
			htmlResponse = "failure-already-pending";
		} else if (Queries.addPendingVerification(id, token, guild_id, role, site)) {
			final EmbedBuilder embedBuilder = new EmbedBuilder();
			embedBuilder.clear().setColor(Color.ORANGE).setTitle(language.get("verification_title"))
					.addField(language.get("pending"), language.get("verify_id_message"), false);
			Utils.messageUser(user, embedBuilder);
			Main.log("Added token " + token + " with ID " + id + " to pend for confirmation");
			htmlResponse = "success";
		} else {
			Main.log("[ERROR] Failed to add token " + token + " with ID " + id + " to pend for confirmation.");
			htmlResponse = "failure-database";
		}

		response.setContentLength(htmlResponse.length());

		try (OutputStream stream = response.getOutputStream()) {
			stream.write(htmlResponse.getBytes());
		}
	}

}
