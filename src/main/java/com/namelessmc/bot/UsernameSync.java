package com.namelessmc.bot;

import com.namelessmc.bot.connections.BackendStorageException;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.NamelessUser;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.exceptions.PermissionException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class UsernameSync implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsernameSync.class);

    private void updateUsernames(long guildId) throws Exception {
        LOGGER.info("Syncing usernames for guild: {} ", guildId);

        final Guild guild = Main.getGuildById(guildId);

        if (guild == null) {
            LOGGER.warn("Skipping guild with id {}, guild is null", guildId);
            return;
        }

        final NamelessAPI api = Main.getConnectionManager().getApiConnection(guildId);

        if (api == null) {
            LOGGER.info("Guild is not linked to website");
            return;
        }

        final Collection<Member> members = guild.findMembers(member -> !member.isOwner()).get();

        for (final Member member : members) {
            final String name = member.getUser().getName();
            final NamelessUser user = api.userByDiscordId(member.getIdLong());

            if (user == null) {
                LOGGER.info("Member {} has no NamelessMC account", name);
            } else {
                final @Nullable String oldNickname = member.getNickname();
                final String newNickname = user.username();
                if (newNickname.equals(oldNickname)) {
                    LOGGER.info("Nickname for {} already matches their NamelessMC account", name);
                } else {
                    LOGGER.info("Setting nickname for member {} to {}", name, newNickname);
                    try {
                        member.modifyNickname(newNickname).queue();
                    } catch (PermissionException e) {
                        LOGGER.info("Failed to change nickname for member {}: {}", member.getIdLong(), e.toString());
                    }
                }
            }
        }
    }

    public void run() {
        LOGGER.info("Starting username sync");

        final Collection<Long> guildIds;
        try {
            guildIds = Main.getConnectionManager().listGuildsUsernameSyncEnabled();
        } catch (BackendStorageException e) {
            throw new RuntimeException(e);
        }

        for (final long guildId : guildIds) {
            try {
                updateUsernames(guildId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
