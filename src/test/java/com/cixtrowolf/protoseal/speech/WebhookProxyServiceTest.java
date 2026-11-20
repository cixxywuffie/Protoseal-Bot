package com.cixtrowolf.protoseal.speech;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.Webhook;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.WebhookExecuteSpec;
import discord4j.rest.util.AllowedMentions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WebhookProxyServiceTest {
    @Test
    void onlyUsesBotOwnedWebhookAndSuppressesMentionsAndEmbeds() {
        var channel = mock(TextChannel.class);
        var bot = mock(User.class);
        var webhook = mock(Webhook.class);
        var author = mock(Member.class);
        var result = mock(Message.class);
        var botId = Snowflake.of(1);
        when(channel.getWebhooks()).thenReturn(Flux.just(webhook));
        when(webhook.getName()).thenReturn(Optional.of("ProtoSeal speech"));
        when(webhook.getCreator()).thenReturn(Optional.of(bot));
        when(bot.getId()).thenReturn(botId);
        when(webhook.getToken()).thenReturn(Optional.of("token"));
        when(author.getDisplayName()).thenReturn("Example");
        when(author.getEffectiveAvatarUrl()).thenReturn("https://example.com/avatar");
        when(webhook.executeAndWait(any(WebhookExecuteSpec.class))).thenReturn(Mono.just(result));

        assertSame(result, new WebhookProxyService().publish(channel, botId, author, "@everyone hello").block());
        var captor = ArgumentCaptor.forClass(WebhookExecuteSpec.class);
        verify(webhook).executeAndWait(captor.capture());
        var spec = captor.getValue();
        assertEquals(AllowedMentions.suppressAll().toData(), spec.allowedMentions().get().toData());
        assertTrue(spec.flags().get().contains(Message.Flag.SUPPRESS_EMBEDS));
        assertEquals("@everyone hello", spec.content().get());
    }
}
