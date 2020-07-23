package com.namelessmc.bot.http;

import com.namelessmc.bot.Main;
import com.namelessmc.bot.http.HttpUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public class IncomingRoleChange implements HttpHandler {

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        Map<String, List<String>> params = HttpUtils.getParams(httpExchange.getRequestURI().toString());

        String guild_id = params.get("guild_id").toString();
        Guild guild = Main.getJda().getGuildById(Long.parseLong(guild_id.substring(1, guild_id.length() - 1)));

        String member_id = params.get("id").toString();
        Member member = guild.getMemberById(Long.parseLong(member_id.substring(1, member_id.length() - 1)));

        try {
            String new_role_id = params.get("role").toString();
            Role new_role = guild.getRoleById(new_role_id.substring(1, new_role_id.length() - 1));
            guild.addRoleToMember(member.getId(), new_role).queue();
        } catch (NullPointerException | NumberFormatException ignored) {}

        try {
            String old_role_id = params.get("oldRole").toString();
            Role old_role = guild.getRoleById(old_role_id.substring(1, old_role_id.length() - 1));
            guild.removeRoleFromMember(member.getId(), old_role).queue();
        } catch (NullPointerException | NumberFormatException ignored) {}

        Main.getLogger().info("Processed role update for " + member.getEffectiveName() + " in " + guild.getName());
        OutputStream outputStream = httpExchange.getResponseBody();
        String htmlResponse = "success";
        httpExchange.sendResponseHeaders(200, htmlResponse.length());
        outputStream.write(htmlResponse.getBytes());
        outputStream.flush();
        outputStream.close();
    }
}
