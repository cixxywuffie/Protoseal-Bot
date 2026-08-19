package com.cixtrowolf.protoseal.persistence.channel;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GuildChannelServiceTest {

    private final GuildChannelRepository repository = mock(GuildChannelRepository.class);
    private final BlockedGuildChannelRepository blockedRepository = mock(BlockedGuildChannelRepository.class);
    private final GuildChannelService service = new GuildChannelService(repository, blockedRepository);

    @Test
    void blocksChannelsThatAreNotExplicitlyListed() {
        assertFalse(service.isAllowed("guild", "channel"));
    }

    @Test
    void onlyAllowsListedChannelsAfterConfiguration() {
        when(repository.existsByGuildIdAndChannelId("guild", "allowed")).thenReturn(true);

        assertTrue(service.isAllowed("guild", "allowed"));
        assertFalse(service.isAllowed("guild", "blocked"));
    }

    @Test
    void removesAnAdditionalAllowedChannel() {
        when(repository.existsByGuildIdAndChannelId("guild", "channel")).thenReturn(true);

        assertTrue(service.remove("guild", "channel"));
        verify(repository).deleteByGuildIdAndChannelId("guild", "channel");
    }

    @Test
    void listsConfiguredChannelIds() {
        when(repository.findAllByGuildIdOrderByChannelId("guild"))
                .thenReturn(List.of(new GuildChannel("guild", "123"), new GuildChannel("guild", "456")));

        assertTrue(service.list("guild").equals(List.of("123", "456")));
    }

    @Test
    void blacklistTakesPriorityWhenCheckingAChannel() {
        when(blockedRepository.existsByGuildIdAndChannelId("guild", "123")).thenReturn(true);

        assertTrue(service.isBlocked("guild", "123"));
    }

    @Test
    void blockingAWhitelistedChannelRemovesItFromTheWhitelist() {
        when(repository.existsByGuildIdAndChannelId("guild", "123")).thenReturn(true);

        assertTrue(service.block("guild", "123"));
        verify(repository).deleteByGuildIdAndChannelId("guild", "123");
    }
}
