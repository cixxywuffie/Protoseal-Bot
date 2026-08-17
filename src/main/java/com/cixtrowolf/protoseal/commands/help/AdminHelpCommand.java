package com.cixtrowolf.protoseal.commands.help;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.rest.util.Permission;
import com.cixtrowolf.protoseal.commands.SlashCommandInterface;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class AdminHelpCommand implements SlashCommandInterface {

    private final GatewayDiscordClient client;

    public AdminHelpCommand(GatewayDiscordClient client) {
        this.client = client;
    }

    @Override
    public String getName() {
        return "adminhelp";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        var guildId = event.getInteraction().getGuildId();
        var actorId = event.getInteraction().getUser().getId();
        if (guildId.isEmpty()) {
            return event.reply("This command can only be used in a server.").withEphemeral(true);
        }

        return client.getMemberById(guildId.get(), actorId)
                .flatMap(member -> member.getBasePermissions())
                .map(permissions -> permissions.contains(Permission.ADMINISTRATOR))
                .onErrorReturn(false)
                .flatMap(isAdministrator -> isAdministrator
                        ? event.reply("**Administrator commands**\n"
                                        + "`/consentreset target` — resets a user's consent to `self_only` "
                                        + "and removes their owner relationship.\n"
                                        + "`/safeword target` — removes another user's active restraints and locks "
                                        + "in an emergency.\n\n"
                                        + "Administrator access never bypasses a user's normal consent settings.")
                                .withEphemeral(true)
                        : event.reply("Only server administrators can use this command.").withEphemeral(true));
    }
}
