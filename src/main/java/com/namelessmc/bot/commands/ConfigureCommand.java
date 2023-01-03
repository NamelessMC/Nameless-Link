package com.namelessmc.bot.commands;

import com.google.common.base.Ascii;
import com.namelessmc.bot.Language;
import com.namelessmc.bot.Main;
import com.namelessmc.bot.connections.BackendStorageException;
import com.namelessmc.bot.connections.ConnectionCache;
import com.namelessmc.bot.listeners.DiscordRoleListener;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.NamelessVersion;
import com.namelessmc.java_api.Website;
import com.namelessmc.java_api.exception.NamelessException;
import com.namelessmc.java_api.exception.UnknownNamelessVersionException;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.namelessmc.bot.Language.Term.*;

public class ConfigureCommand extends Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigureCommand.class);

    ConfigureCommand() {
        super("configure");
    }

    @Override
    public CommandData getCommandData(final Language language) {
        // TODO make all strings translatable
        return Commands.slash(this.name, language.get(CONFIGURE_DESCRIPTION))
                .addSubcommands(
                        new SubcommandData("link", "Link bot to website")
                                .addOption(OptionType.STRING, "api_url", language.get(APIURL_OPTION_URL), true)
                                .addOption(OptionType.STRING, "api_key", language.get(APIURL_OPTION_APIKEY), true),
                        new SubcommandData("unlink", "Disconnect bot from website"),
                        new SubcommandData("test", "Check if website connection is working"),
                        new SubcommandData("username_sync", "Enable or disable syncing usernames")
                                .addOption(OptionType.BOOLEAN, "state", "State", true)
                )
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event, InteractionHook hook, Language language, Guild guild, @Nullable NamelessAPI api) {
        Main.canModifySettings(event.getUser(), guild, canModifySettings -> {
            if (!canModifySettings) {
                hook.sendMessage(language.get(ERROR_NO_PERMISSION)).queue();
                LOGGER.info("User {} does not have permission to modify settings", event.getUser().getIdLong());
                return;
            }

            String subcommand = event.getSubcommandName();
            LOGGER.info("Subcommand: {}", subcommand);
            switch (subcommand) {
                case "link" -> link(event, hook, language, api);
                case "unlink" -> unlink(event, hook, language, api);
                case "test" -> testConnection(event, hook, language, api);
                case "username_sync" -> changeUsernameSync(event, hook, language);
                default -> throw new IllegalArgumentException("Unknown subcommand: " + subcommand);
            }
        });

        LOGGER.info(event.getSubcommandName());
    }
    private void unlink(SlashCommandInteractionEvent event, InteractionHook hook, Language language, @Nullable NamelessAPI oldApi) {
        if (oldApi == null) {
            hook.sendMessage(language.get(ERROR_NOT_SET_UP)).queue();
            LOGGER.info("Cannot unlink, bot was not linked");
            return;
        }

        long guildId = event.getGuild().getIdLong();
        try {
            Main.getConnectionManager().removeConnection(guildId);
            hook.sendMessage(language.get(APIURL_UNLINKED)).queue();
            LOGGER.info("Unlinked from guild {}", guildId);
        } catch (final BackendStorageException e) {
            hook.sendMessage(language.get(ERROR_GENERIC)).queue();
            LOGGER.error("storage backend", e);
        }
    }

    private void link(SlashCommandInteractionEvent event, InteractionHook hook, Language language, @Nullable NamelessAPI oldApi) {
        long guildId = event.getGuild().getIdLong();
        String apiUrlString = event.getOption("api_url").getAsString();
        String apiKey = event.getOption("api_key").getAsString();

        URL apiUrl;
        try {
            apiUrl = new URL(apiUrlString);
        } catch (final MalformedURLException e) {
            hook.sendMessage(language.get(APIURL_URL_MALFORMED)).queue();
            return;
        }

        try {
            LOGGER.info("Checking if API URL works...");

            NamelessAPI api = ConnectionCache.getApiConnection(apiUrl, apiKey);
            long ping = ping(api, language, hook);

            if (ping == -1) {
                // it didn't work, the checkConnection method already send an error message
                return;
            }

            final Optional<Long> optExistingGuildId = Main.getConnectionManager().getGuildIdByApiUrl(apiUrl);

            if (optExistingGuildId.isPresent() && optExistingGuildId.get() != guildId) {
                // We can safely do this, since we have just verified the user knows the secret API key.
                LOGGER.info("URL was already linked to a different guild. It will be unlinked.");
                Main.getConnectionManager().removeConnection(optExistingGuildId.get());
                return;
            }

            LOGGER.info("API URL seems to work. Sending bot settings...");

            try {
                final User botUser = Main.getJdaForGuild(guildId).getSelfUser();
                api.discord().updateBotSettings(Main.getBotUrl(), guildId, botUser.getAsTag(), botUser.getIdLong());

                if (oldApi == null) {
                    // User is setting up new connection
                    Main.getConnectionManager().createConnection(guildId, apiUrl, apiKey);
                    hook.sendMessage(language.get(APIURL_SUCCESS_NEW)).queue();
                    LOGGER.info("Set API URL for guild {} to {}", guildId, apiUrl);
                } else {
                    // User is modifying API URL for existing connection
                    Main.getConnectionManager().updateConnection(guildId, apiUrl, apiKey);
                    hook.sendMessage(language.get(APIURL_SUCCESS_UPDATED)).queue();
                    LOGGER.info("Updated API URL for guild {} from {} to {}", guildId, oldApi, apiUrl);
                }

                DiscordRoleListener.sendRolesAsync(guildId);
            } catch (final NamelessException e) {
                hook.sendMessage("```\n" + Ascii.truncate(e.getMessage(), 1500, "[truncated]") + "\n```").queue();
                hook.sendMessage(language.get(APIURL_FAILED_CONNECTION)).queue();
                Main.logConnectionError(LOGGER, e);
            }
        } catch (final BackendStorageException e) {
            hook.sendMessage(language.get(ERROR_GENERIC)).queue();
            LOGGER.error("storage backend", e);
        }
    }

    void testConnection(SlashCommandInteractionEvent event, InteractionHook hook, Language language, @Nullable NamelessAPI api) {
        if (api == null) {
            hook.sendMessage(language.get(ERROR_NOT_SET_UP)).queue();
            return;
        }

        long ping = ping(api, language, event.getHook());
        if (ping >= 0) {
            hook.sendMessage(language.get(PING_WORKING, "time", ping)).queue();
        }
    }

    private long ping(final NamelessAPI api, final Language language, final InteractionHook hook) {
        URL url = api.apiUrl();
        if (!url.getProtocol().equals("http") && !url.getProtocol().equals("https") ||
                !url.getPath().endsWith("/index.php") ||
                !url.getQuery().equals("route=/api/v2") && !url.getQuery().equals("route=/api/v2/")
        ) {
            LOGGER.info("Invalid URL with protocol '{}' host '{}' path '{}' query '{}'", url.getProtocol(), url.getHost(), url.getPath(), url.getQuery());
            hook.sendMessage(language.get(APIURL_URL_INVALID)).queue();
            return -1;
        }

        String host = url.getHost();
        if (!Main.isLocalAllowed() && (
                host.equals("localhost") ||
                        host.startsWith("127.") ||
                        host.startsWith("192.168.") ||
                        host.startsWith("10.")
                // checking 172.16.0.0/12 is too much work...
        )) {
            LOGGER.info("Local host: '{}'", host);
            hook.sendMessage(language.get(APIURL_URL_LOCAL)).queue();
            return -1;
        }

        try {
            final long start = System.currentTimeMillis();
            LOGGER.info("Making request to info endpoint");
            final Website info = api.website();
            try {
                if (!NamelessVersion.isSupportedByJavaApi(info.parsedVersion())) {
                    hook.sendMessage(language.get(ERROR_WEBSITE_VERSION, "version", info.rawVersion(), "compatibleVersions", supportedVersionsList())).queue();
                    LOGGER.info("Incompatible NamelessMC version");
                    return -1;
                }

                LOGGER.info("Website connection is working");
                return System.currentTimeMillis() - start;
            } catch (UnknownNamelessVersionException e) {
                hook.sendMessage(language.get(ERROR_WEBSITE_VERSION, "version", info.rawVersion(), "compatibleVersions", supportedVersionsList())).queue();
                Main.logConnectionError(LOGGER, "unknown nameless version", e);
                return -1;
            }
        } catch (final NamelessException e) {
            hook.sendMessage("```\n" + Ascii.truncate(e.getMessage(), 1500, "[truncated]") + "\n```").queue();
            hook.sendMessage(language.get(APIURL_FAILED_CONNECTION)).queue();
            Main.logConnectionError(LOGGER, "NamelessException during ping", e);
            return -1;
        }
    }

    public static String supportedVersionsList() {
        return NamelessVersion.supportedVersions().stream().map(NamelessVersion::friendlyName).collect(Collectors.joining(", "));
    }

    private void changeUsernameSync(SlashCommandInteractionEvent event, InteractionHook hook, Language language) {
        boolean state = event.getOption("state").getAsBoolean();
        final String originalNickname = event.getMember().getNickname();

        // Modify nickname to check if permission is working
        try {
            event.getMember().modifyNickname("test_permission").complete();

            // Restore original nickname, if the member running the command was not the server owner
            event.getMember().modifyNickname(originalNickname).queue();
        } catch (HierarchyException ignored) {
            // This is expected, changing the nickname of the owner is never allowed.
        } catch (InsufficientPermissionException e) {
            hook.sendMessage("Missing permission to change nicknames. Please try kicking and re-inviting the bot to ensure it has the 'Manage Nicknames' permission.").queue();
            return;
        }

        try {
            Main.getConnectionManager().setUsernameSyncEnabled(event.getGuild().getIdLong(), state);
        } catch (final BackendStorageException e) {
            hook.sendMessage(language.get(ERROR_GENERIC)).queue();
            LOGGER.error("storage backend", e);
            return;
        }

        if (state) {
            hook.sendMessage("Username sync has been enabled. Please note that Nameless-Link cannot update nicknames for users with a higher role. You should move the Nameless-Link role to the top in role settings. Nameless-Link will never change the server owner's nickname, since that is not allowed by Discord.").queue();
        } else {
            hook.sendMessage("Username sync has been disabled.").queue();
        }


    }

}
