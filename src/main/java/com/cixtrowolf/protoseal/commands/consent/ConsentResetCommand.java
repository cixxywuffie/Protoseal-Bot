package com.cixtrowolf.protoseal.commands.consent;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.rest.util.Permission;
import com.cixtrowolf.protoseal.commands.SlashCommandInterface;
import com.cixtrowolf.protoseal.persistence.consent.ConsentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class ConsentResetCommand implements SlashCommandInterface {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsentResetCommand.class);

    private final ConsentService consentService;
    private final GatewayDiscordClient client;

    public ConsentResetCommand(ConsentService consentService, GatewayDiscordClient client) {
        this.consentService = consentService;
        this.client = client;
    }

    @Override
    public String getName() {
        return "consentreset";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        var guildId = event.getInteraction().getGuildId();
        var actor = event.getInteraction().getUser();
        if (guildId.isEmpty()) {
            return event.reply("This command can only be used in a server.").withEphemeral(true);
        }

        return client.getMemberById(guildId.get(), actor.getId())
                .flatMap(member -> member.getBasePermissions())
                .map(permissions -> permissions.contains(Permission.ADMINISTRATOR))
                .onErrorReturn(false)
                .flatMap(isAdministrator -> {
                    if (!isAdministrator) {
                        return event.reply("Only server administrators can reset consent.").withEphemeral(true);
                    }

                    return event.getOption("target")
                            .flatMap(ApplicationCommandInteractionOption::getValue)
                            .map(value -> value.asUser())
                            .map(target -> target.flatMap(user -> Mono.fromRunnable(() -> consentService.resetConsent(
                                            guildId.get().asString(), user.getId().asString()))
                                    .subscribeOn(Schedulers.boundedElastic())
                                    .then(event.reply(actor.getMention() + " reset " + user.getMention()
                                            + "'s consent to **self only**."))
                                    .doOnSuccess(ignored -> LOGGER.info(
                                            "Consent reset guildId={} administratorId={} targetId={}",
                                            guildId.get(), actor.getId(), user.getId()))))
                            .orElseGet(() -> event.reply("You must specify a target user.").withEphemeral(true));
                });
    }
}
