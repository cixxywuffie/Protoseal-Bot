package com.cixtrowolf.protoseal.listeners;

import com.cixtrowolf.protoseal.persistence.privacy.GuildDataCleanupService;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GuildDeleteListenerTest {

    @Test
    void deletesDataWhenBotIsRemovedFromGuild() {
        var cleanupService = mock(GuildDataCleanupService.class);
        var event = mock(GuildDeleteEvent.class);
        when(event.getGuildId()).thenReturn(Snowflake.of("123"));
        when(event.isUnavailable()).thenReturn(false);

        new GuildDeleteListener(cleanupService).handle(event).block();

        verify(cleanupService).deleteForGuild("123");
    }

    @Test
    void preservesDataWhenGuildIsTemporarilyUnavailable() {
        var cleanupService = mock(GuildDataCleanupService.class);
        var event = mock(GuildDeleteEvent.class);
        when(event.getGuildId()).thenReturn(Snowflake.of("123"));
        when(event.isUnavailable()).thenReturn(true);

        new GuildDeleteListener(cleanupService).handle(event).block();

        verify(cleanupService, never()).deleteForGuild("123");
    }
}
