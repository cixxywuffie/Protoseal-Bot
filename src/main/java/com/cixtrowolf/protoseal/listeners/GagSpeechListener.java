package com.cixtrowolf.protoseal.listeners;

import com.cixtrowolf.protoseal.persistence.channel.GuildChannelService;
import com.cixtrowolf.protoseal.persistence.channel.GuildSpeechSettingsService;
import com.cixtrowolf.protoseal.speech.GagSpeechService;
import com.cixtrowolf.protoseal.speech.WebhookProxyService;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.retriever.EntityRetrievalStrategy;
import discord4j.rest.util.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

@Component
public class GagSpeechListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(GagSpeechListener.class);
    private static final long RETENTION_MS = Duration.ofMinutes(10).toMillis();
    private static final int MAX_TRACKED = 10000;
    private final Map<String, Long> seen = new LinkedHashMap<>();
    private final Set<String> busyChannels = new HashSet<>();
    private final Map<String, Long> lastWarning = new HashMap<>();
    private final GatewayDiscordClient client;
    private final GuildSpeechSettingsService settings;
    private final GuildChannelService channels;
    private final GagSpeechService speech;
    private final WebhookProxyService webhooks;

    public GagSpeechListener(GatewayDiscordClient client, GuildSpeechSettingsService settings,
                             GuildChannelService channels, GagSpeechService speech, WebhookProxyService webhooks) {
        this.client = client;
        this.settings = settings;
        this.channels = channels;
        this.speech = speech;
        this.webhooks = webhooks;
        client.on(MessageCreateEvent.class, this::handle).subscribe();
    }

    Mono<Void> handle(MessageCreateEvent event) {
        Message message = event.getMessage();
        if (event.getGuildId().isEmpty() || event.getMember().isEmpty()
                || message.getAuthor().isEmpty() || message.getAuthor().get().isBot()
                || message.getWebhookId().isPresent() || message.getType() != Message.Type.DEFAULT
                || message.getContent().isBlank() || message.isTts()
                || !message.getAttachments().isEmpty() || !message.getEmbeds().isEmpty()
                || !message.getStickersItems().isEmpty() || !message.getComponents().isEmpty()
                || message.getMessageReference().isPresent() || message.getData().poll().isPresent()
                || message.getEditedTimestamp().isPresent()
                || message.getFlags().contains(Message.Flag.HAS_THREAD)
                || message.getFlags().contains(Message.Flag.IS_VOICE_MESSAGE)) {
            return Mono.empty();
        }
        String guildId = event.getGuildId().get().asString();
        String channelId = message.getChannelId().asString();
        String userId = message.getAuthor().get().getId().asString();
        String messageId = message.getId().asString();

        return Mono.fromCallable(() -> settings.isEnabled(guildId) && !channels.isBlocked(guildId, channelId)
                        && speech.hasActiveEffect(guildId, userId))
                .subscribeOn(Schedulers.boundedElastic())
                .filter(Boolean::booleanValue)
                .flatMap(eligible -> Mono.defer(() -> {
                    if (!acquire(messageId, channelId)) return Mono.<Void>empty();
                    return message.getChannel()
                        .ofType(TextChannel.class)
                        .filter(TextChannel::isNsfw)
                        .flatMap(channel -> channel.getEffectivePermissions(client.getSelfId())
                                .flatMap(permissions -> {
                                    if (!permissions.contains(Permission.VIEW_CHANNEL)
                                            || !permissions.contains(Permission.SEND_MESSAGES)
                                            || !permissions.contains(Permission.READ_MESSAGE_HISTORY)
                                            || !permissions.contains(Permission.MANAGE_WEBHOOKS)
                                            || !permissions.contains(Permission.MANAGE_MESSAGES)) {
                                        warn(channelId, "missing channel permissions");
                                        return Mono.empty();
                                    }
                                    return Mono.fromCallable(() -> settings.isEnabled(guildId)
                                                    && !channels.isBlocked(guildId, channelId)
                                                    ? speech.transform(guildId, userId, message.getContent())
                                                    : java.util.Optional.<String>empty())
                                            .subscribeOn(Schedulers.boundedElastic())
                                            .flatMap(transformed -> Mono.justOrEmpty(transformed))
                                            .flatMap(content -> channel.getMessageById(message.getId(), EntityRetrievalStrategy.REST)
                                            .filter(current -> unchanged(message, current))
                                            .flatMap(ignored -> webhooks.publish(channel, client.getSelfId(), event.getMember().get(), content)))
                                            .flatMap(replacement -> {
                                                LOGGER.info("Gag speech proxied guildId={} channelId={} authorId={} originalId={} replacementId={}",
                                                        guildId, channelId, userId, messageId, replacement.getId());
                                                return channel.getMessageById(message.getId(), EntityRetrievalStrategy.REST)
                                                        .flatMap(current -> {
                                                            if (!unchanged(message, current)) {
                                                                LOGGER.warn("Gag speech original changed after publication channelId={} originalId={} replacementId={}",
                                                                        channelId, messageId, replacement.getId());
                                                                return Mono.empty();
                                                            }
                                                            return current.delete();
                                                        })
                                                        .onErrorResume(error -> {
                                                            LOGGER.warn("Gag speech original deletion failed guildId={} channelId={} originalId={} replacementId={} errorType={}",
                                                                    guildId, channelId, messageId, replacement.getId(),
                                                                    error.getClass().getSimpleName());
                                                            return Mono.empty();
                                                        });
                                            });
                                })).then().doFinally(signal -> release(channelId));
                }))
                .onErrorResume(error -> {
                    warn(channelId, "processing failed (" + error.getClass().getSimpleName() + ")");
                    return Mono.empty();
                });
    }

    private boolean unchanged(Message original, Message current) {
        return current.getEditedTimestamp().isEmpty() && original.getContent().equals(current.getContent());
    }

    private synchronized boolean acquire(String messageId, String channelId) {
        long now = System.currentTimeMillis();
        seen.entrySet().removeIf(entry -> now - entry.getValue() > RETENTION_MS);
        lastWarning.entrySet().removeIf(entry -> now - entry.getValue() > RETENTION_MS);
        if (seen.containsKey(messageId) || seen.size() >= MAX_TRACKED || busyChannels.contains(channelId)) {
            return false;
        }
        seen.put(messageId, now);
        busyChannels.add(channelId);
        return true;
    }

    private synchronized void release(String channelId) {
        busyChannels.remove(channelId);
    }

    private synchronized void warn(String channelId, String reason) {
        long now = System.currentTimeMillis();
        if (now - lastWarning.getOrDefault(channelId, 0L) >= Duration.ofMinutes(1).toMillis()) {
            if (lastWarning.size() < MAX_TRACKED) lastWarning.put(channelId, now);
            LOGGER.warn("Gag speech skipped channelId={} reason={}", channelId, reason);
        }
    }
}
