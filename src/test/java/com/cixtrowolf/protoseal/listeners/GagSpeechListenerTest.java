package com.cixtrowolf.protoseal.listeners;

import com.cixtrowolf.protoseal.persistence.channel.GuildChannelService;
import com.cixtrowolf.protoseal.persistence.channel.GuildSpeechSettingsService;
import com.cixtrowolf.protoseal.speech.GagSpeechService;
import com.cixtrowolf.protoseal.speech.WebhookProxyService;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.retriever.EntityRetrievalStrategy;
import discord4j.discordjson.json.MessageData;
import discord4j.discordjson.json.PollData;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GagSpeechListenerTest {
    private final GatewayDiscordClient client = mock(GatewayDiscordClient.class);
    private final GuildSpeechSettingsService settings = mock(GuildSpeechSettingsService.class);
    private final GuildChannelService channels = mock(GuildChannelService.class);
    private final GagSpeechService speech = mock(GagSpeechService.class);
    private final WebhookProxyService webhooks = mock(WebhookProxyService.class);
    private final MessageCreateEvent event = mock(MessageCreateEvent.class);
    private final Message message = mock(Message.class);
    private final MessageData data = mock(MessageData.class);
    private final Member member = mock(Member.class);
    private final User author = mock(User.class);
    private final TextChannel channel = mock(TextChannel.class);
    private final Snowflake guildId = Snowflake.of(1);
    private final Snowflake channelId = Snowflake.of(2);
    private final Snowflake userId = Snowflake.of(3);
    private final Snowflake messageId = Snowflake.of(4);
    private final Snowflake botId = Snowflake.of(5);
    private GagSpeechListener listener;

    @BeforeEach
    void setUp() {
        when(client.on(eq(MessageCreateEvent.class), any())).thenReturn(Flux.empty());
        listener = new GagSpeechListener(client, settings, channels, speech, webhooks);
        when(event.getMessage()).thenReturn(message);
        when(event.getGuildId()).thenReturn(Optional.of(guildId));
        when(event.getMember()).thenReturn(Optional.of(member));
        when(message.getAuthor()).thenReturn(Optional.of(author));
        when(author.getId()).thenReturn(userId);
        when(message.getType()).thenReturn(Message.Type.DEFAULT);
        when(message.getContent()).thenReturn("Hello there");
        when(message.getData()).thenReturn(data);
        when(data.poll()).thenReturn(Possible.absent());
        when(message.getFlags()).thenReturn(EnumSet.noneOf(Message.Flag.class));
        when(message.getChannelId()).thenReturn(channelId);
        when(message.getId()).thenReturn(messageId);
    }

    private void enablePipeline() {
        when(settings.isEnabled("1")).thenReturn(true);
        when(speech.transform("1", "3", "Hello there")).thenReturn(Optional.of("Mmmh"));
        when(message.getChannel()).thenReturn(Mono.just(channel));
        when(channel.isNsfw()).thenReturn(true);
        when(client.getSelfId()).thenReturn(botId);
        when(channel.getEffectivePermissions(botId)).thenReturn(Mono.just(PermissionSet.of(
                Permission.VIEW_CHANNEL, Permission.SEND_MESSAGES, Permission.READ_MESSAGE_HISTORY, Permission.MANAGE_WEBHOOKS,
                Permission.MANAGE_MESSAGES)));
        when(channel.getMessageById(messageId, EntityRetrievalStrategy.REST)).thenReturn(Mono.just(message));
    }

    @Test
    void publishesBeforeDeletingAndDeduplicates() {
        enablePipeline();
        var replacement = mock(Message.class);
        when(replacement.getId()).thenReturn(Snowflake.of(6));
        when(webhooks.publish(channel, botId, member, "Mmmh")).thenReturn(Mono.just(replacement));
        when(message.delete()).thenReturn(Mono.empty());

        listener.handle(event).block(Duration.ofSeconds(5));
        listener.handle(event).block(Duration.ofSeconds(5));

        var order = inOrder(webhooks, message);
        order.verify(webhooks).publish(channel, botId, member, "Mmmh");
        order.verify(message).delete();
        verify(webhooks, times(1)).publish(any(), any(), any(), anyString());
    }

    @Test
    void publicationFailureNeverDeletesOriginal() {
        enablePipeline();
        when(webhooks.publish(channel, botId, member, "Mmmh")).thenReturn(Mono.error(new IllegalStateException()));
        listener.handle(event).block(Duration.ofSeconds(5));
        verify(message, never()).delete();
    }

    @Test
    void deletionFailureDoesNotPublishTwice() {
        enablePipeline();
        var replacement = mock(Message.class);
        when(replacement.getId()).thenReturn(Snowflake.of(6));
        when(webhooks.publish(channel, botId, member, "Mmmh")).thenReturn(Mono.just(replacement));
        when(message.delete()).thenReturn(Mono.error(new IllegalStateException()));
        listener.handle(event).block(Duration.ofSeconds(5));
        listener.handle(event).block(Duration.ofSeconds(5));
        verify(webhooks, times(1)).publish(any(), any(), any(), anyString());
        verify(message, times(1)).delete();
    }

    @Test
    void aSafewordBeforePublicationPreventsReplacement() {
        enablePipeline();
        when(speech.transform("1", "3", "Hello there"))
                .thenReturn(Optional.of("Mmmh"), Optional.empty());
        listener.handle(event).block(Duration.ofSeconds(5));
        verifyNoInteractions(webhooks);
        verify(message, never()).delete();
    }

    @Test
    void editedMessageIsNotDeletedAfterPublication() {
        enablePipeline();
        var edited = mock(Message.class);
        when(edited.getEditedTimestamp()).thenReturn(Optional.of(java.time.Instant.now()));
        when(channel.getMessageById(messageId, EntityRetrievalStrategy.REST))
                .thenReturn(Mono.just(message), Mono.just(edited));
        var replacement = mock(Message.class);
        when(replacement.getId()).thenReturn(Snowflake.of(6));
        when(webhooks.publish(channel, botId, member, "Mmmh")).thenReturn(Mono.just(replacement));
        listener.handle(event).block(Duration.ofSeconds(5));
        verify(message, never()).delete();
        verify(edited, never()).delete();
    }

    @Test
    void disabledServerLeavesMessageUntouched() {
        listener.handle(event).block(Duration.ofSeconds(5));
        verifyNoInteractions(webhooks);
        verify(message, never()).delete();
    }

    @Test
    void pollIsNeverProxied() {
        when(data.poll()).thenReturn(Possible.of(mock(PollData.class)));
        listener.handle(event).block(Duration.ofSeconds(5));
        verifyNoInteractions(webhooks);
        verify(message, never()).delete();
    }

    @Test
    void missingPermissionsNeverPublishesOrDeletes() {
        enablePipeline();
        when(channel.getEffectivePermissions(botId)).thenReturn(Mono.just(PermissionSet.of(Permission.VIEW_CHANNEL)));
        listener.handle(event).block(Duration.ofSeconds(5));
        verifyNoInteractions(webhooks);
        verify(message, never()).delete();
    }

    @Test
    void nonNsfwChannelsAreNeverProxied() {
        enablePipeline();
        when(channel.isNsfw()).thenReturn(false);
        listener.handle(event).block(Duration.ofSeconds(5));
        verifyNoInteractions(webhooks);
        verify(message, never()).delete();
    }

    @Test
    void botAndWebhookMessagesAreIgnored() {
        when(author.isBot()).thenReturn(true);
        listener.handle(event).block(Duration.ofSeconds(5));
        when(author.isBot()).thenReturn(false);
        when(message.getWebhookId()).thenReturn(Optional.of(Snowflake.of(7)));
        listener.handle(event).block(Duration.ofSeconds(5));
        verifyNoInteractions(settings, webhooks);
    }
}
