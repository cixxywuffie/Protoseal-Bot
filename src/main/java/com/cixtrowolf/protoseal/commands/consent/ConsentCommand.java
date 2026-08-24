package com.cixtrowolf.protoseal.commands.consent;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import com.cixtrowolf.protoseal.commands.SlashCommandInterface;
import com.cixtrowolf.protoseal.persistence.consent.ConsentMode;
import com.cixtrowolf.protoseal.persistence.consent.ConsentService;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Locale;

@Component
public class ConsentCommand implements SlashCommandInterface {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsentCommand.class);
    private static final int INVITATION_COLOR = 0x5865F2;
    private final ConsentService consentService;

    public ConsentCommand(ConsentService consentService) {
        this.consentService = consentService;
    }

    @Override
    public String getName() {
        return "consent";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        var guildId = event.getInteraction().getGuildId();
        if (guildId.isEmpty()) {
            return event.reply("This command can only be used in a server.").withEphemeral(true);
        }

        var mode = event.getOption("mode")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(value -> ConsentMode.valueOf(value.asString().toUpperCase(Locale.ROOT)));
        if (mode.isEmpty()) {
            return event.reply("You must select a consent mode.").withEphemeral(true);
        }

        User actor = event.getInteraction().getUser();
        return event.deferReply()
                .withEphemeral(true)
                .then(Mono.defer(() -> {
                    if (mode.get() != ConsentMode.OWNER) {
                        return saveConsent(event, guildId.get().asString(), actor.getId().asString(), mode.get(), null);
                    }

                    return event.getOption("owner")
                            .flatMap(ApplicationCommandInteractionOption::getValue)
                            .map(value -> value.asUser())
                            .map(owner -> owner.flatMap(user -> {
                                if (user.getId().equals(actor.getId())) {
                                    return editReply(event, "You cannot select yourself as your owner.");
                                }
                                if (user.isBot()) {
                                    return editReply(event, "A bot cannot be selected as an owner.");
                                }
                                return sendOwnerInvitation(event, user, guildId.get().asString(), actor);
                            }))
                            .orElseGet(() -> editReply(event, "Owner mode requires an owner user."));
                }));
    }

    private Mono<Void> saveConsent(ChatInputInteractionEvent event, String guildId, String userId,
                                   ConsentMode mode, String ownerUserId) {
        return Mono.fromRunnable(() -> consentService.updateConsent(guildId, userId, mode, ownerUserId))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(ignored -> LOGGER.info(
                        "Consent updated guildId={} userId={} mode={} ownerId={}",
                        guildId, userId, mode, ownerUserId))
                .doOnError(error -> LOGGER.error(
                        "Consent update failed guildId={} userId={} mode={}", guildId, userId, mode, error))
                .then(editReply(event, consentMessage(mode, ownerUserId)));
    }

    private Mono<Void> sendOwnerInvitation(ChatInputInteractionEvent event, User owner,
                                           String guildId, User requester) {
        return Mono.fromCallable(() -> consentService.createOwnerRequest(
                        guildId, requester.getId().asString(), owner.getId().asString()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(token -> {
                    Mono<Void> sendDm = event.getInteraction().getGuild()
                        .map(guild -> guild.getName())
                        .onErrorReturn("Unknown server")
                        .defaultIfEmpty("Unknown server")
                        .flatMap(serverName -> owner.getPrivateChannel()
                            .flatMap(channel -> channel.createMessage()
                                .withEmbeds(ownerInvitationEmbed(requester, serverName))
                                .withComponents(ActionRow.of(
                                        Button.success("consent-owner:accept:" + token, "Accept"),
                                        Button.danger("consent-owner:reject:" + token, "Reject")))))
                        .then();

                    return sendDm
                        .thenReturn(true)
                        .onErrorResume(error -> Mono.fromRunnable(() -> consentService.cancelOwnerRequest(
                                        guildId, requester.getId().asString()))
                                .subscribeOn(Schedulers.boundedElastic())
                                .doOnSuccess(ignored -> LOGGER.warn(
                                        "Owner invitation cancelled after DM failure guildId={} requesterId={} ownerId={}",
                                        guildId, requester.getId(), owner.getId(), error))
                                .then(editReply(event,
                                        "I could not send that user a DM. Your consent was not changed."))
                                .thenReturn(false))
                        .flatMap(sent -> {
                            if (!sent) {
                                return Mono.empty();
                            }
                            LOGGER.info("Owner invitation sent guildId={} requesterId={} ownerId={}",
                                    guildId, requester.getId(), owner.getId());
                            return editReply(event, "Owner invitation sent to " + owner.getMention()
                                    + ". Your current consent mode will remain unchanged until they accept.");
                        });
                });
    }

    private EmbedCreateSpec ownerInvitationEmbed(User requester, String serverName) {
        return EmbedCreateSpec.builder()
                .color(Color.of(INVITATION_COLOR))
                .title("Owner consent invitation")
                .description("**" + requester.getUsername() + "** is inviting you to become their owner for "
                        + "restraint roleplay.\n\nAccepting will allow you to manage their restraints and locks "
                        + "under their current consent settings. They can revoke this relationship or use "
                        + "`/safeword` at any time.\n\nOnly accept if you recognize the user and server and agree "
                        + "to participate.")
                .addField("Requested by", requester.getMention(), true)
                .addField("Server", "**" + serverName + "**", true)
                .footer("This invitation expires in 24 hours.", null)
                .build();
    }

    private Mono<Void> editReply(ChatInputInteractionEvent event, String message) {
        return event.editReply(message).then();
    }

    private String consentMessage(ConsentMode mode, String ownerUserId) {
        return switch (mode) {
            case SELF_ONLY -> "Consent updated to **self only**. Only you can manage your restraints.";
            case EXPOSED -> "Consent updated to **exposed**. Other users can manage your restraints and locks.";
            case OWNER -> "Consent updated to **owner**. Only <@" + ownerUserId
                    + "> can manage your restraints and locks.";
            case DISABLED -> "Consent **disabled**. No restraints or locks can be managed; `/safeword` remains available.";
        };
    }
}
