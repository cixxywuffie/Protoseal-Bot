package com.cixtrowolf.protoseal.persistence.channel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GuildChannelServiceTest {

    private final GuildChannelRepository repository = mock(GuildChannelRepository.class);
    private final BlockedGuildChannelRepository blockedRepository = mock(BlockedGuildChannelRepository.class);
    private final GuildChannelService service = new GuildChannelService(repository, blockedRepository);

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

    @Test
    void clearingConfigurationAlsoRemovesLegacyWhitelistEntries() {
        service.clear("guild");

        verify(repository).deleteAllByGuildId("guild");
        verify(blockedRepository).deleteAllByGuildId("guild");
    }
}
