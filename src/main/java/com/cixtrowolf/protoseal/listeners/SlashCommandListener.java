package com.cixtrowolf.protoseal.listeners;


import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.channel.TextChannel;
import com.cixtrowolf.protoseal.commands.SlashCommandInterface;
import com.cixtrowolf.protoseal.persistence.channel.GuildChannelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Collection;
import java.util.List;

@Component
public class SlashCommandListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SlashCommandListener.class);
    private final Collection<SlashCommandInterface> commands;
    private final GuildChannelService channelService;

    public SlashCommandListener(List<SlashCommandInterface> slashCommands, GatewayDiscordClient client,
                                GuildChannelService channelService) {
        commands = slashCommands;
        this.channelService = channelService;

        client.on(ChatInputInteractionEvent.class, this::handle).subscribe();
    }


    public Mono<Void> handle(ChatInputInteractionEvent event) {
        String commandName = event.getCommandName();
        String actorId = event.getInteraction().getUser().getId().asString();
        String guildId = event.getInteraction().getGuildId()
                .map(snowflake -> snowflake.asString())
                .orElse("DM");
        String interactionId = event.getInteraction().getId().asString();

        return canRun(event, commandName, guildId)
                .flatMap(allowed -> allowed
                        ? dispatch(event, commandName, guildId, actorId, interactionId)
                        : event.reply("ProtoSeal is not enabled in this channel. Ask a server administrator "
                                        + "to configure `/channelconfig`.")
                                .withEphemeral(true));
    }

    private Mono<Boolean> canRun(ChatInputInteractionEvent event, String commandName, String guildId) {
        // Configuration must remain reachable, and a safety action must never be blocked.
        if ("DM".equals(guildId) || "channelconfig".equals(commandName) || "safeword".equals(commandName)) {
            return Mono.just(true);
        }
        String channelId = event.getInteraction().getChannelId().asString();
        return Mono.fromCallable(() -> channelService.isBlocked(guildId, channelId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(isBlocked -> {
                    if (isBlocked) return Mono.just(false);
                    return event.getInteraction().getChannel()
                            .map(channel -> channel instanceof TextChannel textChannel && textChannel.isNsfw())
                            .onErrorReturn(false)
                            .flatMap(isNsfw -> isNsfw ? Mono.just(true)
                                    : Mono.fromCallable(() -> channelService.isAllowed(guildId, channelId))
                                            .subscribeOn(Schedulers.boundedElastic()));
                });
    }

    private Mono<Void> dispatch(ChatInputInteractionEvent event, String commandName, String guildId,
                                String actorId, String interactionId) {
        return Flux.fromIterable(commands)
                //Filter out all commands that don't match the name this event is for
                .filter(command -> command.supports(commandName))
                //Get the first (and only) item in the flux that matches our filter
                .next()
                //Have our command class handle all logic related to its specific command.
                .flatMap(command -> Mono.defer(() -> {
                    long startedAt = System.nanoTime();
                    LOGGER.debug("Command received command={} guildId={} actorId={} interactionId={}",
                            commandName, guildId, actorId, interactionId);
                    return command.handle(event)
                            .doOnSuccess(ignored -> LOGGER.info(
                                    "Command completed command={} guildId={} actorId={} interactionId={} durationMs={}",
                                    commandName, guildId, actorId, interactionId,
                                    (System.nanoTime() - startedAt) / 1_000_000))
                            .doOnError(error -> LOGGER.error(
                                    "Command failed command={} guildId={} actorId={} interactionId={} durationMs={}",
                                    commandName, guildId, actorId, interactionId,
                                    (System.nanoTime() - startedAt) / 1_000_000, error))
                            .thenReturn(true);
                }))
                .switchIfEmpty(Mono.defer(() -> {
                    LOGGER.warn("Unknown command command={} guildId={} actorId={} interactionId={}",
                            commandName, guildId, actorId, interactionId);
                    return event.reply("That command is not available.")
                            .withEphemeral(true)
                            .thenReturn(false);
                }))
                .then();
    }
}
