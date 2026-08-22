package com.cixtrowolf.protoseal.commands.restraint;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cixtrowolf.protoseal.commands.SlashCommandInterface;
import com.cixtrowolf.protoseal.model.restraint.RestraintLockType;
import com.cixtrowolf.protoseal.persistence.restraint.RestraintStateService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Locale;

@Component
public class LockCommand implements SlashCommandInterface {

    private static final Logger LOGGER = LoggerFactory.getLogger(LockCommand.class);
    private static final int WARNING_COLOR = 0xE67E22;
    private final RestraintStateService restraintStateService;

    public LockCommand(RestraintStateService restraintStateService) {
        this.restraintStateService = restraintStateService;
    }

    @Override
    public String getName() {
        return "lock";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        var guildId = event.getInteraction().getGuildId();
        if (guildId.isEmpty()) {
            return event.reply("This command can only be used in a server.").withEphemeral(true);
        }

        String requestedType = event.getOption("type")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(value -> value.asString().toUpperCase(Locale.ROOT))
                .orElse("PADLOCK");
        RestraintLockType lockType = "REMOVE".equals(requestedType)
                ? null
                : RestraintLockType.valueOf(requestedType);

        return event.getOption("target")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(value -> value.asUser())
                .map(target -> target.flatMap(user -> lockType == RestraintLockType.PERMALOCK
                        ? requestPermalockConfirmation(event, user, guildId.get().asString())
                        : Mono.fromCallable(() -> restraintStateService.updateLocks(
                                guildId.get().asString(), user.getId().asString(), lockType,
                                event.getInteraction().getUser().getId().asString()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .flatMap(result -> replyForResult(event, user.getMention(), lockType, result))))
                .orElseGet(() -> event.reply("You must specify a target user.").withEphemeral(true));
    }

    private Mono<Void> requestPermalockConfirmation(ChatInputInteractionEvent event, User target, String guildId) {
        String actorId = event.getInteraction().getUser().getId().asString();
        LOGGER.info("Permalock confirmation requested guildId={} actorId={} targetId={}",
                guildId, actorId, target.getId());
        var confirm = new PermalockConfirmation(
                PermalockConfirmation.Action.CONFIRM, guildId, target.getId().asString(), actorId);
        var cancel = new PermalockConfirmation(
                PermalockConfirmation.Action.CANCEL, guildId, target.getId().asString(), actorId);
        var warning = EmbedCreateSpec.builder()
                .color(Color.of(WARNING_COLOR))
                .title("⚠️ Confirm permanent lock")
                .description("You are about to permanently lock every active restraint of " + target.getMention()
                        + ".\n\nOnce confirmed, **nobody can remove or replace this lock with `/lock`**. "
                        + "Only `/safeword` can clear the permalock and restraint states.")
                .addField("Action required", "Confirm only if everyone involved understands this consequence.", false)
                .build();

        return event.reply()
                .withEphemeral(true)
                .withEmbeds(warning)
                .withComponents(ActionRow.of(
                        Button.danger(confirm.customId(), "Confirm permalock"),
                        Button.secondary(cancel.customId(), "Cancel")));
    }

    private Mono<Void> replyForResult(ChatInputInteractionEvent event, String targetMention,
                                      RestraintLockType lockType,
                                      RestraintStateService.LockResult result) {
        LOGGER.info("Lock command result guildId={} actorId={} lockType={} result={}",
                event.getInteraction().getGuildId().map(id -> id.asString()).orElse("DM"),
                event.getInteraction().getUser().getId(), lockType == null ? "REMOVE" : lockType, result);
        return switch (result) {
            case APPLIED -> lockType == RestraintLockType.PERMALOCK
                    ? event.reply(String.format(
                            "⚠️ %s applied %s **Permalock** to all of %s's active restraints. "
                                    + "This cannot be removed or replaced with `/lock`; only `/safeword` can clear it.",
                            event.getInteraction().getUser().getMention(), lockType.getEmoji(), targetMention))
                    : event.reply(String.format("%s applied %s **%s** to all of %s's active restraints.",
                            event.getInteraction().getUser().getMention(), lockType.getEmoji(),
                            lockType.getDisplayName(), targetMention));
            case REMOVED -> event.reply(String.format("%s removed the locks from all of %s's active restraints.",
                    event.getInteraction().getUser().getMention(), targetMention));
            case NO_ACTIVE_RESTRAINT -> event.reply("That user has no active restraints.")
                    .withEphemeral(true);
            case NOT_LOCKED -> event.reply("That user's active restraints are not locked.").withEphemeral(true);
            case PERMALOCKED -> event.reply(
                            "That user's restraints are permanently locked. Only `/safeword` can clear them.")
                    .withEphemeral(true);
            case TIMELOCKED -> event.reply(
                            "That user's restraints are timelocked until the displayed expiry. Only `/safeword` can clear them early.")
                    .withEphemeral(true);
            case LOCKED_BY_ANOTHER_USER -> event.reply("Only the user who applied these locks can change or remove them.")
                    .withEphemeral(true);
            case CONSENT_DENIED -> event.reply(
                    "That user's consent settings do not allow you to manage their locks.")
                    .withEphemeral(true);
        };
    }
}
