package com.cixtrowolf.protoseal.commands.safeword;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.rest.util.Permission;
import com.cixtrowolf.protoseal.commands.SlashCommandInterface;
import com.cixtrowolf.protoseal.persistence.restraint.RestraintStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class SafewordCommand implements SlashCommandInterface {

    private static final Logger LOGGER = LoggerFactory.getLogger(SafewordCommand.class);

    private final RestraintStateService restraintStateService;
    private final GatewayDiscordClient client;

    public SafewordCommand(RestraintStateService restraintStateService, GatewayDiscordClient client) {
        this.restraintStateService = restraintStateService;
        this.client = client;
    }

    @Override
    public String getName() {
        return "safeword";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        var guild = event.getInteraction().getGuildId();
        var guildId = guild.map(id -> id.asString());
        var actor = event.getInteraction().getUser();
        var actorId = actor.getId();

        if (guildId.isEmpty()) {
            LOGGER.warn("Rejected safeword actorId={} missingGuild", actorId);
            return event.reply("This command can only be used in a server.").withEphemeral(true);
        }

        Mono<Boolean> canResetAnotherUser = client.getMemberById(guild.get(), actorId)
                .flatMap(member -> member.getBasePermissions())
                .map(permissions -> permissions.contains(Permission.ADMINISTRATOR))
                .onErrorResume(error -> {
                    LOGGER.error("Unable to verify safeword permissions guildId={} actorId={}", guildId.get(), actorId, error);
                    return Mono.just(false);
                });

        return event.getOption("target")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(value -> value.asUser())
                .orElse(Mono.just(actor))
                .flatMap(target -> {
                    boolean isSelfTarget = target.getId().equals(actorId);
                    Mono<Boolean> authorized = isSelfTarget ? Mono.just(true) : canResetAnotherUser;

                    return authorized.flatMap(isAuthorized -> {
                        if (!isAuthorized) {
                            LOGGER.warn("Rejected safeword guildId={} actorId={} targetId={} insufficientPermissions",
                                    guildId.get(), actorId, target.getId());
                            return event.reply("Only server administrators can reset another user's restraint states.")
                                    .withEphemeral(true);
                        }

                        return Mono.fromRunnable(() -> restraintStateService.clearStates(guildId.get(), target.getId().asString()))
                                .subscribeOn(Schedulers.boundedElastic())
                                .then(event.reply(isSelfTarget
                                        ? "Safeword accepted. Your restraint states have been reset."
                                        : "Safeword accepted. The user's restraint states have been reset."))
                                .doOnSuccess(ignored -> LOGGER.info(
                                        "Safeword used guildId={} actorId={} targetId={} selfTarget={}",
                                        guildId.get(), actorId, target.getId(), isSelfTarget))
                                .doOnError(error -> LOGGER.error(
                                        "Failed safeword guildId={} actorId={} targetId={}",
                                        guildId.get(), actorId, target.getId(), error));
                    });
                });
    }
}
