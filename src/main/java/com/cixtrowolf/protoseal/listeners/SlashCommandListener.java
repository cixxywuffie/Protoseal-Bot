package com.cixtrowolf.protoseal.listeners;


import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import com.cixtrowolf.protoseal.commands.SlashCommandInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

@Component
public class SlashCommandListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SlashCommandListener.class);
    private final Collection<SlashCommandInterface> commands;

    public SlashCommandListener(List<SlashCommandInterface> slashCommands, GatewayDiscordClient client) {
        commands = slashCommands;

        client.on(ChatInputInteractionEvent.class, this::handle).subscribe();
    }


    public Mono<Void> handle(ChatInputInteractionEvent event) {
        String commandName = event.getCommandName();
        String actorId = event.getInteraction().getUser().getId().asString();
        String guildId = event.getInteraction().getGuildId()
                .map(snowflake -> snowflake.asString())
                .orElse("DM");
        String interactionId = event.getInteraction().getId().asString();

        //Convert our list to a flux that we can iterate through
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
