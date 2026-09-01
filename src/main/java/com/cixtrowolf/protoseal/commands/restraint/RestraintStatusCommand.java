package com.cixtrowolf.protoseal.commands.restraint;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import com.cixtrowolf.protoseal.commands.SlashCommandInterface;
import com.cixtrowolf.protoseal.persistence.consent.ConsentService;
import com.cixtrowolf.protoseal.persistence.restraint.RestraintState;
import com.cixtrowolf.protoseal.persistence.restraint.RestraintStateService;
import com.cixtrowolf.protoseal.model.restraint.RestraintZone;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.time.Duration;
import java.time.Instant;

@Component
public class RestraintStatusCommand implements SlashCommandInterface {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestraintStatusCommand.class);
    private static final int EMBED_COLOR = 0x9B59B6;

    private final RestraintStateService restraintStateService;
    private final ConsentService consentService;

    public RestraintStatusCommand(RestraintStateService restraintStateService, ConsentService consentService) {
        this.restraintStateService = restraintStateService;
        this.consentService = consentService;
    }

    @Override
    public String getName() {
        return "rdstatus";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        var guildId = event.getInteraction().getGuildId();
        if (guildId.isEmpty()) {
            return event.reply("This command can only be used in a server.").withEphemeral(true);
        }

        Mono<User> target = event.getOption("target")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(value -> value.asUser())
                .orElse(Mono.just(event.getInteraction().getUser()));

        return target.flatMap(user -> Mono.fromCallable(() -> new StatusData(
                        restraintStateService.findActiveStates(guildId.get().asString(), user.getId().asString()),
                        consentService.getConsentStatus(guildId.get().asString(), user.getId().asString())))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(status -> LOGGER.debug(
                        "Restraint status loaded guildId={} requesterId={} targetId={} activeStates={} consentMode={}",
                        guildId.get(), event.getInteraction().getUser().getId(), user.getId(),
                        status.states().size(), status.consent().mode()))
                .flatMap(status -> event.reply()
                        .withEmbeds(createStatusEmbed(user, status.states(), status.consent()))));
    }

    private EmbedCreateSpec createStatusEmbed(User user, List<RestraintState> states,
                                              ConsentService.ConsentStatus consent) {
        var embed = EmbedCreateSpec.builder()
                .color(Color.of(EMBED_COLOR))
                .title("Active restraints")
                .description("Status for " + user.getMention());

        embed.addField("🤝 Consent", formatConsent(consent), false);

        if (states.isEmpty()) {
            return embed.addField("Status", "✅ No active restraints.", false).build();
        }

        states.stream()
                .filter(RestraintState::isLocked)
                .findFirst()
                .ifPresentOrElse(
                        state -> embed.addField(
                                "🔐 Lock status",
                                formatLock(state),
                                false),
                        () -> embed.addField("🔓 Lock status", "Unlocked", false));

        states.forEach(state -> embed.addField(
                state.getZone().getEmoji() + " " + state.getZone().getDisplayName(),
                formatRestraint(state),
                true));

        return embed.build();
    }

    private String formatRestraint(RestraintState state) {
        if (state.getZone() == RestraintZone.LEASH
                && state.getName().startsWith("held by <@")) {
            return "Type " + state.getName();
        }
        return "Type `" + state.getName() + "`";
    }

    private String formatLock(RestraintState state) {
        String value = state.getLockType().getEmoji() + " **" + state.getLockType().getDisplayName()
                + "** by <@" + state.getLockedByUserId() + ">";
        if (state.getLockExpiresAt() == null) return value;
        long seconds = Math.max(0, Duration.between(Instant.now(), state.getLockExpiresAt()).toSeconds());
        long hours = seconds / 3600;
        long minutes = (seconds % 3600 + 59) / 60;
        return value + " — " + (hours > 0 ? hours + "h " : "") + minutes + "m remaining";
    }

    private String formatConsent(ConsentService.ConsentStatus consent) {
        return switch (consent.mode()) {
            case SELF_ONLY -> "🔒 **Self only** — only this user can manage their restraints.";
            case ASK -> "🙋 **Ask** — changes requested by other users require this user's approval.";
            case EXPOSED -> "🌐 **Exposed** — other users may manage their restraints and locks.";
            case OWNER -> "👤 **Owner** — only "
                    + (consent.ownerUserId() == null ? "the selected owner" : "<@" + consent.ownerUserId() + ">")
                    + " may manage their restraints and locks.";
            case DISABLED -> "⛔ **Disabled** — restraint interactions are disabled; `/safeword` remains available.";
        };
    }

    private record StatusData(List<RestraintState> states, ConsentService.ConsentStatus consent) {
    }

}
