package com.cixtrowolf.protoseal.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import reactor.core.publisher.Mono;

public interface SlashCommandInterface {
    String getName();

    default boolean supports(String commandName) {
        return getName().equals(commandName);
    }

    Mono<Void> handle(ChatInputInteractionEvent event);
}
