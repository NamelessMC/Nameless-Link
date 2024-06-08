package com.namelessmc.bot.http;

import java.util.Objects;

import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.util.Util;
import com.namelessmc.java_api.NamelessAPI;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;

public class SendDirectMessage extends HttpHandler {

	@Override
	public void service(Request request, Response response) throws Exception {
		response.setHeader("Content-Type", "text/plain");
		
        if (request.getMethod() != Method.POST) {
            response.setStatus(HttpStatus.METHOD_NOT_ALLOWED_405);
            return;
        }
        
        final JsonObject requestJson = (JsonObject) JsonParser.parseReader(request.getReader());
		final long guildId = requestJson.get("guild_id").getAsLong();
		final long userId = requestJson.get("user_id").getAsLong();
		final String apiKey = requestJson.get("api_key").getAsString();
		final String message = requestJson.get("message").getAsString();
		Objects.requireNonNull(apiKey);
		
		final NamelessAPI api = Main.getConnectionManager().getApiConnection(guildId);
		
		if (api == null) {
			response.setStatus(HttpStatus.UNAUTHORIZED_401);
			response.getWriter().write("Bot is not linked");
			return;
		}
		
		if (!Util.timingSafeEquals(api.apiKey().getBytes(), apiKey.getBytes())) {
			response.setStatus(HttpStatus.UNAUTHORIZED_401);
			response.getWriter().write("Invalid API key");
			return;
		}
		
		final JDA jda = Main.getJdaForGuild(guildId);
		final User targetUser = jda.retrieveUserById(userId).complete();
		if (targetUser == null) {
			response.setStatus(HttpStatus.BAD_REQUEST_400);
			response.getWriter().write("User does not exist");
			return;
		}
		
		final PrivateChannel channel = targetUser.openPrivateChannel().complete();
		channel.sendMessage(message);
		response.setStatus(HttpStatus.OK_200);
		response.getWriter().write("Message sent");
	}

}
