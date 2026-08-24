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
public class NameTagCommand implements SlashCommandInterface {
    private final RestraintStateService service;

    public NameTagCommand(RestraintStateService service) {
        this.service = service;
    }

    @Override public String getName() { return "nametag"; }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        var guildId = event.getInteraction().getGuildId();
        if (guildId.isEmpty()) return event.reply("This command can only be used in a server.").withEphemeral(true);
        String label = event.getOption("label").flatMap(ApplicationCommandInteractionOption::getValue)
                .map(value -> value.asString().trim()).orElse("");
        if (label.length() > 64) return event.reply("The name tag can contain at most 64 characters.").withEphemeral(true);
        return userOption(event, "target").flatMap(target -> update(event, target, guildId.get().asString(), label));
    }

    private Mono<Void> update(ChatInputInteractionEvent event, User target, String guildId, String label) {
        int level = label.isBlank() ? 0 : 1;
        String name = level == 0 ? "none" : label;
        User actor = event.getInteraction().getUser();
        boolean selfTarget = actor.getId().equals(target.getId());
        return Mono.fromCallable(() -> service.saveState(guildId, target.getId().asString(), RestraintZone.NAMETAG,
                        level, actor.getId().asString(), name))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(result -> switch (result) {
                    case UPDATED -> event.reply(selfTarget
                            ? selfNameTagMessage(actor, label)
                            : otherNameTagMessage(actor, target, label));
                    case LOCKED -> event.reply("This name tag is locked.").withEphemeral(true);
                    case MITTS_ACTIVE -> event.reply("That user's active mitts prevent this change.").withEphemeral(true);
                    case CONSENT_DENIED -> event.reply("That user's consent settings do not allow this change.").withEphemeral(true);
                });
    }

    private String selfNameTagMessage(User actor, String label) {
        return label.isBlank()
                ? "🏷️ " + actor.getMention() + " slips off their own name tag, leaving themselves unlabelled."
                : "🏷️ " + actor.getMention() + " fastens on their own name tag, proudly displaying **"
                        + label + "** for everyone to see.";
    }

    private String otherNameTagMessage(User actor, User target, String label) {
        return label.isBlank()
                ? "🏷️ " + actor.getMention() + " removes " + target.getMention()
                        + "'s name tag, leaving them unlabelled."
                : "🏷️ " + actor.getMention() + " fastens a name tag onto " + target.getMention()
                        + ", marking them as **" + label + "** for everyone to see.";
    }

    static Mono<User> userOption(ChatInputInteractionEvent event, String name) {
        return event.getOption(name).flatMap(ApplicationCommandInteractionOption::getValue)
                .map(value -> value.asUser())
                .orElseGet(() -> Mono.error(new IllegalArgumentException("Missing user option: " + name)));
    }
}
