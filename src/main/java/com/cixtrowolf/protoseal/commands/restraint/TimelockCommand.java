package com.cixtrowolf.protoseal.commands.restraint;

import com.cixtrowolf.protoseal.commands.SlashCommandInterface;
import com.cixtrowolf.protoseal.persistence.restraint.RestraintStateService;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.entity.User;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

@Component
public class TimelockCommand implements SlashCommandInterface {
    private final RestraintStateService service;
    public TimelockCommand(RestraintStateService service) { this.service = service; }
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
}
