package com.cixtrowolf.protoseal.commands.restraint;

import com.cixtrowolf.protoseal.commands.SlashCommandInterface;
import com.cixtrowolf.protoseal.model.restraint.RestraintZone;
import com.cixtrowolf.protoseal.persistence.restraint.RestraintStateService;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.entity.User;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class LeashCommand implements SlashCommandInterface {
    private final RestraintStateService service;
    public LeashCommand(RestraintStateService service) { this.service = service; }
    @Override public String getName() { return "leash"; }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        var guildId = event.getInteraction().getGuildId();
        if (guildId.isEmpty()) return event.reply("This command can only be used in a server.").withEphemeral(true);
        boolean remove = event.getOption("action").flatMap(ApplicationCommandInteractionOption::getValue)
                .map(value -> "REMOVE".equals(value.asString())).orElse(false);
        return NameTagCommand.userOption(event, "target")
                .flatMap(target -> update(event, target, guildId.get().asString(), remove));
    }

    private Mono<Void> update(ChatInputInteractionEvent event, User target, String guildId, boolean remove) {
        var actor = event.getInteraction().getUser();
        return Mono.fromCallable(() -> service.saveState(guildId, target.getId().asString(), RestraintZone.LEASH,
                        remove ? 0 : 1, actor.getId().asString(), remove ? "none" : "held by <@" + actor.getId().asString() + ">"))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(result -> switch (result) {
                    case UPDATED -> event.reply(remove
                            ? actor.getMention() + " removes " + target.getMention() + "'s leash."
                            : actor.getMention() + " clips a leash to " + target.getMention() + " and takes hold of it.");
                    case LOCKED -> event.reply("This leash is locked.").withEphemeral(true);
                    case MITTS_ACTIVE -> event.reply("That user's active mitts prevent this change.").withEphemeral(true);
                    case CONSENT_DENIED -> event.reply("That user's consent settings do not allow this change.").withEphemeral(true);
                });
    }
}
