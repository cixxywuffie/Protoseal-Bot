package com.cixtrowolf.protoseal.commands.restraint;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.entity.User;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import com.cixtrowolf.protoseal.commands.SlashCommandInterface;
import com.cixtrowolf.protoseal.model.restraint.RestraintDefinition;
import com.cixtrowolf.protoseal.model.restraint.GagIntensity;
import com.cixtrowolf.protoseal.model.restraint.RestraintLevel;
import com.cixtrowolf.protoseal.persistence.restraint.RestraintStateService;
import com.cixtrowolf.protoseal.persistence.consent.ConsentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class RestraintCommand implements SlashCommandInterface {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestraintCommand.class);

    private final RestraintStateService restraintStateService;
    private final ConsentService consentService;

    public RestraintCommand(RestraintStateService restraintStateService, ConsentService consentService) {
        this.restraintStateService = restraintStateService;
        this.consentService = consentService;
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
        GagIntensity intensity = null;
        if (event.getOption("intensity").isPresent()) {
            if (!"gag".equals(event.getCommandName())) {
                return event.reply("Intensity is only available for gags.").withEphemeral(true);
            }
            try {
                intensity = GagIntensity.valueOf(event.getOption("intensity")
                        .flatMap(ApplicationCommandInteractionOption::getValue)
                        .orElseThrow().asString());
            } catch (IllegalArgumentException | java.util.NoSuchElementException ex) {
                return event.reply("Invalid gag intensity.").withEphemeral(true);
            }
        }
        GagIntensity selectedIntensity = intensity;

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
                        event, definition.get(), guildId.get().asString(), user, level, selectedIntensity)))
                .orElseGet(() -> event.reply("You must specify a target user.").withEphemeral(true));
    }

    private Mono<Void> updateRestraint(ChatInputInteractionEvent event, RestraintDefinition definition,
                                       String guildId, User target, int level, GagIntensity intensity) {
        var actor = event.getInteraction().getUser();
        boolean selfTarget = target.getId().equals(actor.getId());
        RestraintLevel selectedLevel = definition.getLevel(level);
        String response = selfTarget ? selectedLevel.selfMessage() : selectedLevel.message();
        String specialEffect = definition == RestraintDefinition.GAG
                && level == RestraintDefinition.REINDEER_MUZZLE_LEVEL
                ? " Reindeer muzzle replaces eligible messages with a reindeer sound regardless of intensity."
                : "";

        if (consentService.requiresRestraintApproval(guildId, target.getId().asString(), actor.getId().asString())) {
            return event.deferReply().withEphemeral(true).then(
                    Mono.fromCallable(() -> consentService.createRestraintRequest(
                                    guildId, target.getId().asString(), actor.getId().asString(),
                                    event.getInteraction().getChannelId().asString(), definition.getZone(),
                                    level, selectedLevel.name(), intensity))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(token -> target.getPrivateChannel()
                                    .flatMap(channel -> channel.createMessage()
                                            .withContent(actor.getMention() + " asks to set your **"
                                                    + definition.getCommandName() + "** to **" + selectedLevel.name()
                                                    + "** in their shared server."
                                                    + (intensity == null ? "" : " Saved gag intensity: **"
                                                            + intensity.name().toLowerCase(java.util.Locale.ROOT) + "**.")
                                                    + specialEffect
                                                    + " This request expires in 5 minutes.")
                                            .withComponents(ActionRow.of(
                                                    Button.success("consent-restraint:accept:" + token, "Accept"),
                                                    Button.danger("consent-restraint:reject:" + token, "Reject"))))
                                    .then(event.editReply("Approval request sent privately to "
                                            + target.getMention() + ".").then())
                                    .onErrorResume(error -> Mono.fromRunnable(
                                                    () -> consentService.cancelRestraintRequest(token))
                                            .subscribeOn(Schedulers.boundedElastic())
                                            .then(event.editReply("I could not send that user a DM. "
                                                    + "The restraint request was cancelled.").then()))));
        }

        return Mono.fromCallable(() -> restraintStateService.saveState(
                        guildId, target.getId().asString(), definition.getZone(), level, actor.getId().asString(),
                        selectedLevel.name(), intensity))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(result -> switch (result) {
                    case UPDATED -> {
                        LOGGER.info("Executed restraint command={} guildId={} actorId={} targetId={} zone={} level={} selfTarget={}",
                                event.getCommandName(), guildId, actor.getId(), target.getId(),
                                definition.getZone(), level, selfTarget);
                        yield event.reply(String.format(response, actor.getMention(), target.getMention())
                                + (intensity == null ? "" : " Saved gag intensity: **"
                                + intensity.name().toLowerCase(java.util.Locale.ROOT) + "**.") + specialEffect);
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
