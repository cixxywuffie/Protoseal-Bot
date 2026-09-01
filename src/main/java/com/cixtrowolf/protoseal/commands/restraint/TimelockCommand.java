package com.cixtrowolf.protoseal.commands.restraint;

import com.cixtrowolf.protoseal.commands.SlashCommandInterface;
import com.cixtrowolf.protoseal.persistence.restraint.RestraintStateService;
import com.cixtrowolf.protoseal.persistence.consent.ConsentService;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.entity.User;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

@Component
public class TimelockCommand implements SlashCommandInterface {
    private final RestraintStateService service;
    private final ConsentService consentService;
    public TimelockCommand(RestraintStateService service, ConsentService consentService) {
        this.service = service;
        this.consentService = consentService;
    }
    @Override public String getName() { return "timelock"; }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        var guildId = event.getInteraction().getGuildId();
        if (guildId.isEmpty()) return event.reply("This command can only be used in a server.").withEphemeral(true);
        long minutes = event.getOption("minutes").flatMap(ApplicationCommandInteractionOption::getValue)
                .map(value -> value.asLong()).orElse(15L);
        return NameTagCommand.userOption(event, "target")
                .flatMap(target -> apply(event, target, guildId.get().asString(), minutes));
    }

    private Mono<Void> apply(ChatInputInteractionEvent event, User target, String guildId, long minutes) {
        String actorId = event.getInteraction().getUser().getId().asString();
        if (consentService.requiresRestraintApproval(guildId, target.getId().asString(), actorId)) {
            return requestApproval(event, target, guildId, minutes, actorId);
        }
        return Mono.fromCallable(() -> service.applyTimelock(guildId, target.getId().asString(),
                        event.getInteraction().getUser().getId().asString(), Duration.ofMinutes(minutes)))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(result -> switch (result) {
                    case APPLIED -> event.reply(event.getInteraction().getUser().getMention() + " timelocks all of "
                            + target.getMention() + "'s active restraints for **" + minutes + " minutes**.");
                    case NO_ACTIVE_RESTRAINT -> event.reply("That user has no active restraints.").withEphemeral(true);
                    case ALREADY_LOCKED -> event.reply("That user's restraints are already locked.").withEphemeral(true);
                    case PERMALOCKED -> event.reply("That user's restraints are permanently locked.").withEphemeral(true);
                    case INVALID_DURATION -> event.reply("The duration must be between 1 minute and 30 days.").withEphemeral(true);
                    case CONSENT_DENIED -> event.reply("That user's consent settings do not allow this timelock.").withEphemeral(true);
                });
    }

    private Mono<Void> requestApproval(ChatInputInteractionEvent event, User target, String guildId,
                                       long minutes, String actorId) {
        return event.deferReply().withEphemeral(true).then(Mono.fromCallable(() -> consentService.createTimelockRequest(
                        guildId, target.getId().asString(), actorId,
                        event.getInteraction().getChannelId().asString(), minutes))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(token -> target.getPrivateChannel()
                        .flatMap(channel -> channel.createMessage()
                                .withContent(event.getInteraction().getUser().getMention()
                                        + " asks to timelock all your active restraints for **" + minutes
                                        + " minutes**. This request expires in 5 minutes.")
                                .withComponents(ActionRow.of(
                                        Button.success("consent-restraint:accept:" + token, "Accept"),
                                        Button.danger("consent-restraint:reject:" + token, "Reject"))))
                        .then(event.editReply("Timelock approval request sent privately to " + target.getMention() + ".").then())
                        .onErrorResume(error -> Mono.fromRunnable(() -> consentService.cancelRestraintRequest(token))
                                .subscribeOn(Schedulers.boundedElastic())
                                .then(event.editReply("I could not send that user a DM. The timelock request was cancelled.").then()))));
    }
}
