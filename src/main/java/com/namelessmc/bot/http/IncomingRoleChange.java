package com.namelessmc.bot.http;

import com.namelessmc.bot.Main;
import com.namelessmc.bot.Queries;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IncomingRoleChange implements HttpHandler {

    @Getter
    private static final HashMap<Member, Role> recentChanges = new HashMap<>();

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        Map<String, List<String>> params = HttpUtils.getParams(httpExchange.getRequestURI().toString());

        String guild_id = params.get("guild_id").toString();
        Guild guild = Main.getJda().getGuildById(Long.parseLong(guild_id.substring(1, guild_id.length() - 1)));
        String member_id = params.get("id").toString();
        Member member = guild.getMemberById(Long.parseLong(member_id.substring(1, member_id.length() - 1)));
        String api_url = httpExchange.getRequestURI().toString().substring(httpExchange.getRequestURI().toString().indexOf("&api_url=") + 9);

        OutputStream outputStream = httpExchange.getResponseBody();
        String htmlResponse;

        if (!guild.getMemberById(Main.getJda().getSelfUser().getId()).canInteract(member)) {
            Main.log("Cannot interact with " + member.getEffectiveName() + " in " + guild.getName());
            htmlResponse = "failure-cannot-interact";
        } else if (!api_url.equals(Queries.getGuildApiUrl(guild.getId()))) {
            Main.log("Invalid Guild API URL sent for " + member.getEffectiveName() + " in " + guild.getName());
            htmlResponse = "failure-invalid-api-url";
        } else {
            try {
                String new_role_id = params.get("role").toString();
                Role new_role = guild.getRoleById(new_role_id.substring(1, new_role_id.length() - 1));
                if (new_role != null) {
                    guild.addRoleToMember(member.getId(), new_role).complete();
                    recentChanges.put(member, new_role);
                }
            } catch (NullPointerException | NumberFormatException ignored) {}
            try {
                String old_role_id = params.get("oldRole").toString();
                Role old_role = guild.getRoleById(old_role_id.substring(1, old_role_id.length() - 1));
                if (old_role != null) {
                    guild.removeRoleFromMember(member.getId(), old_role).complete();
                    recentChanges.put(member, old_role);
                }
            } catch (NullPointerException | NumberFormatException ignored) {}
            Main.log("Processed role update (Website -> Discord) for " + member.getEffectiveName() + ".");
            htmlResponse = "success";
        }

        httpExchange.sendResponseHeaders(200, htmlResponse.length());
        outputStream.write(htmlResponse.getBytes());
        outputStream.flush();
        outputStream.close();
    }
}
