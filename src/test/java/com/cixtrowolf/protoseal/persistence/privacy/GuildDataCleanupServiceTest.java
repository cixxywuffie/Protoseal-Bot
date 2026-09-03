package com.cixtrowolf.protoseal.persistence.privacy;

import com.cixtrowolf.protoseal.persistence.channel.BlockedGuildChannelRepository;
import com.cixtrowolf.protoseal.persistence.channel.GuildChannelRepository;
import com.cixtrowolf.protoseal.persistence.consent.ConsentOwnerRequestRepository;
import com.cixtrowolf.protoseal.persistence.consent.ConsentRestraintRequestRepository;
import com.cixtrowolf.protoseal.persistence.consent.ConsentSettingRepository;
import com.cixtrowolf.protoseal.persistence.restraint.RestraintStateRepository;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GuildDataCleanupServiceTest {

    @Test
    void deletesAllDataBelongingToGuild() {
        var restraintRepository = mock(RestraintStateRepository.class);
        var consentRepository = mock(ConsentSettingRepository.class);
        var ownerRequestRepository = mock(ConsentOwnerRequestRepository.class);
        var restraintRequestRepository = mock(ConsentRestraintRequestRepository.class);
        var guildChannelRepository = mock(GuildChannelRepository.class);
        var blockedChannelRepository = mock(BlockedGuildChannelRepository.class);
        var service = new GuildDataCleanupService(restraintRepository, consentRepository,
                ownerRequestRepository, restraintRequestRepository, guildChannelRepository,
                blockedChannelRepository);

        service.deleteForGuild("guild");

        verify(restraintRepository).deleteAllByGuildId("guild");
        verify(consentRepository).deleteAllByGuildId("guild");
        verify(ownerRequestRepository).deleteAllByGuildId("guild");
        verify(restraintRequestRepository).deleteAllByGuildId("guild");
        verify(guildChannelRepository).deleteAllByGuildId("guild");
        verify(blockedChannelRepository).deleteAllByGuildId("guild");
    }
}
