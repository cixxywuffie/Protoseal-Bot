package com.cixtrowolf.protoseal.commands.restraint;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.entity.User;
import com.cixtrowolf.protoseal.commands.SlashCommandInterface;
import com.cixtrowolf.protoseal.model.restraint.RestraintDefinition;
import com.cixtrowolf.protoseal.model.restraint.RestraintLevel;
import com.cixtrowolf.protoseal.persistence.restraint.RestraintStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class RestraintCommand implements SlashCommandInterface {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestraintCommand.class);

    private final RestraintStateService restraintStateService;

    public RestraintCommand(RestraintStateService restraintStateService) {
        this.restraintStateService = restraintStateService;
    }

    @Override
    public String getName() {
        return "restraint";
    }

    @Override
    public boolean supports(String commandName) {
        return RestraintDefinition.fromCommandName(commandName).isPresent();
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        var definition = RestraintDefinition.fromCommandName(event.getCommandName());
        if (definition.isEmpty()) {
            return event.reply("That restraint command is not available.").withEphemeral(true);
        }

        int level = event.getOption("type")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(value -> Math.toIntExact(value.asLong()))
                .orElse(1);
        if (level < 0 || level > definition.get().getMaximumLevel()) {
            LOGGER.warn("Rejected restraint command={} actorId={} invalidLevel={}",
                    event.getCommandName(), event.getInteraction().getUser().getId(), level);
            return event.reply("The level must be between 0 and " + definition.get().getMaximumLevel() + ".")
                    .withEphemeral(true);
        }

        var guildId = event.getInteraction().getGuildId();
        if (guildId.isEmpty()) {
            LOGGER.warn("Rejected restraint command={} actorId={} missingGuild",
                    event.getCommandName(), event.getInteraction().getUser().getId());
            return event.reply("This command can only be used in a server.").withEphemeral(true);
        }

        return event.getOption("target")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(value -> value.asUser())
                .map(target -> target.flatMap(user -> updateRestraint(
                        event, definition.get(), guildId.get().asString(), user, level)))
                .orElseGet(() -> event.reply("You must specify a target user.").withEphemeral(true));
    }

    private Mono<Void> updateRestraint(ChatInputInteractionEvent event, RestraintDefinition definition,
                                       String guildId, User target, int level) {
        var actor = event.getInteraction().getUser();
        boolean selfTarget = target.getId().equals(actor.getId());
        RestraintLevel selectedLevel = definition.getLevel(level);
        String response = selfTarget ? selectedLevel.selfMessage() : selectedLevel.message();

        return Mono.fromCallable(() -> restraintStateService.saveState(
                        guildId, target.getId().asString(), definition.getZone(), level, actor.getId().asString(), selectedLevel.name()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(result -> switch (result) {
                    case UPDATED -> {
                        LOGGER.info("Executed restraint command={} guildId={} actorId={} targetId={} zone={} level={} selfTarget={}",
                                event.getCommandName(), guildId, actor.getId(), target.getId(),
                                definition.getZone(), level, selfTarget);
                        yield event.reply(String.format(response, actor.getMention(), target.getMention()));
                    }
                    case LOCKED -> event.reply("This restraint is locked. Remove the lock before changing it.")
                            .withEphemeral(true);
                    case MITTS_ACTIVE -> event.reply(
                                    "That user cannot change or remove other restraints while their mitts are active. "
                                            + "Remove the mitts first or use `/safeword`.")
                            .withEphemeral(true);
                    case CONSENT_DENIED -> event.reply(
                                    "That user's consent settings do not allow you to manage their restraints.")
                            .withEphemeral(true);
                })
                .doOnError(error -> LOGGER.error(
                        "Failed restraint command={} guildId={} actorId={} targetId={} zone={} level={}",
                        event.getCommandName(), guildId, actor.getId(), target.getId(),
                        definition.getZone(), level, error));
    }
}
