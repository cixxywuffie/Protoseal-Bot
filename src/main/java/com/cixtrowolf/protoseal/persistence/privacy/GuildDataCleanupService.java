package com.cixtrowolf.protoseal.persistence.privacy;

import com.cixtrowolf.protoseal.persistence.channel.BlockedGuildChannelRepository;
import com.cixtrowolf.protoseal.persistence.channel.GuildChannelRepository;
import com.cixtrowolf.protoseal.persistence.consent.ConsentOwnerRequestRepository;
import com.cixtrowolf.protoseal.persistence.consent.ConsentRestraintRequestRepository;
import com.cixtrowolf.protoseal.persistence.consent.ConsentSettingRepository;
import com.cixtrowolf.protoseal.persistence.restraint.RestraintStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuildDataCleanupService {

    private final RestraintStateRepository restraintStateRepository;
    private final ConsentSettingRepository consentSettingRepository;
    private final ConsentOwnerRequestRepository ownerRequestRepository;
    private final ConsentRestraintRequestRepository restraintRequestRepository;
    private final GuildChannelRepository guildChannelRepository;
    private final BlockedGuildChannelRepository blockedGuildChannelRepository;

    public GuildDataCleanupService(RestraintStateRepository restraintStateRepository,
                                   ConsentSettingRepository consentSettingRepository,
                                   ConsentOwnerRequestRepository ownerRequestRepository,
                                   ConsentRestraintRequestRepository restraintRequestRepository,
                                   GuildChannelRepository guildChannelRepository,
                                   BlockedGuildChannelRepository blockedGuildChannelRepository) {
        this.restraintStateRepository = restraintStateRepository;
        this.consentSettingRepository = consentSettingRepository;
        this.ownerRequestRepository = ownerRequestRepository;
        this.restraintRequestRepository = restraintRequestRepository;
        this.guildChannelRepository = guildChannelRepository;
        this.blockedGuildChannelRepository = blockedGuildChannelRepository;
    }

    @Transactional
    public void deleteForGuild(String guildId) {
        restraintRequestRepository.deleteAllByGuildId(guildId);
        ownerRequestRepository.deleteAllByGuildId(guildId);
        restraintStateRepository.deleteAllByGuildId(guildId);
        consentSettingRepository.deleteAllByGuildId(guildId);
        guildChannelRepository.deleteAllByGuildId(guildId);
        blockedGuildChannelRepository.deleteAllByGuildId(guildId);
    }
}
