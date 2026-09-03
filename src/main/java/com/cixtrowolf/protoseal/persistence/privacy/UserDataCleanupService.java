package com.cixtrowolf.protoseal.persistence.privacy;

import com.cixtrowolf.protoseal.persistence.consent.ConsentMode;
import com.cixtrowolf.protoseal.persistence.consent.ConsentOwnerRequestRepository;
import com.cixtrowolf.protoseal.persistence.consent.ConsentRestraintRequestRepository;
import com.cixtrowolf.protoseal.persistence.consent.ConsentSettingRepository;
import com.cixtrowolf.protoseal.persistence.restraint.RestraintStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDataCleanupService {

    private final RestraintStateRepository restraintStateRepository;
    private final ConsentSettingRepository consentSettingRepository;
    private final ConsentOwnerRequestRepository ownerRequestRepository;
    private final ConsentRestraintRequestRepository restraintRequestRepository;

    public UserDataCleanupService(RestraintStateRepository restraintStateRepository,
                                  ConsentSettingRepository consentSettingRepository,
                                  ConsentOwnerRequestRepository ownerRequestRepository,
                                  ConsentRestraintRequestRepository restraintRequestRepository) {
        this.restraintStateRepository = restraintStateRepository;
        this.consentSettingRepository = consentSettingRepository;
        this.ownerRequestRepository = ownerRequestRepository;
        this.restraintRequestRepository = restraintRequestRepository;
    }

    @Transactional
    public void deleteForMember(String guildId, String userId) {
        // Preserve other members' restraint records, but remove locks owned by the departing member.
        restraintStateRepository.findByGuildIdAndLockedByUserId(guildId, userId)
                .forEach(state -> state.removeLock());
        restraintStateRepository.deleteByGuildIdAndUserId(guildId, userId);

        // Owner mode cannot remain linked to a member who is no longer in the guild.
        consentSettingRepository.findByGuildIdAndOwnerUserId(guildId, userId)
                .forEach(setting -> setting.update(ConsentMode.SELF_ONLY, null));
        consentSettingRepository.deleteByGuildIdAndUserId(guildId, userId);

        ownerRequestRepository.deleteByGuildIdAndRequesterUserId(guildId, userId);
        ownerRequestRepository.deleteByGuildIdAndOwnerUserId(guildId, userId);
        restraintRequestRepository.deleteByGuildIdAndTargetUserId(guildId, userId);
        restraintRequestRepository.deleteByGuildIdAndActorUserId(guildId, userId);
    }
}
