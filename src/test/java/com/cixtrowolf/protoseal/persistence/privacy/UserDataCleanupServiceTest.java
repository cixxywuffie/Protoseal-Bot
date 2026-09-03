package com.cixtrowolf.protoseal.persistence.privacy;

import com.cixtrowolf.protoseal.model.restraint.RestraintLockType;
import com.cixtrowolf.protoseal.model.restraint.RestraintZone;
import com.cixtrowolf.protoseal.persistence.consent.ConsentMode;
import com.cixtrowolf.protoseal.persistence.consent.ConsentOwnerRequestRepository;
import com.cixtrowolf.protoseal.persistence.consent.ConsentRestraintRequestRepository;
import com.cixtrowolf.protoseal.persistence.consent.ConsentSetting;
import com.cixtrowolf.protoseal.persistence.consent.ConsentSettingRepository;
import com.cixtrowolf.protoseal.persistence.restraint.RestraintState;
import com.cixtrowolf.protoseal.persistence.restraint.RestraintStateRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserDataCleanupServiceTest {

    @Test
    void deletesMemberDataAndRemovesReferencesFromOtherMembers() {
        var restraintRepository = mock(RestraintStateRepository.class);
        var consentRepository = mock(ConsentSettingRepository.class);
        var ownerRequestRepository = mock(ConsentOwnerRequestRepository.class);
        var restraintRequestRepository = mock(ConsentRestraintRequestRepository.class);
        var service = new UserDataCleanupService(restraintRepository, consentRepository,
                ownerRequestRepository, restraintRequestRepository);

        var otherMemberState = new RestraintState("guild", "other", RestraintZone.ARMS, 1, "Cuffs");
        otherMemberState.applyLock(RestraintLockType.PADLOCK, "departing");
        var otherMemberConsent = new ConsentSetting("guild", "other", ConsentMode.OWNER, "departing");
        when(restraintRepository.findByGuildIdAndLockedByUserId("guild", "departing"))
                .thenReturn(List.of(otherMemberState));
        when(consentRepository.findByGuildIdAndOwnerUserId("guild", "departing"))
                .thenReturn(List.of(otherMemberConsent));

        service.deleteForMember("guild", "departing");

        assertNull(otherMemberState.getLockType());
        assertNull(otherMemberState.getLockedByUserId());
        assertEquals(ConsentMode.SELF_ONLY, otherMemberConsent.getMode());
        assertNull(otherMemberConsent.getOwnerUserId());
        verify(restraintRepository).deleteByGuildIdAndUserId("guild", "departing");
        verify(consentRepository).deleteByGuildIdAndUserId("guild", "departing");
        verify(ownerRequestRepository).deleteByGuildIdAndRequesterUserId("guild", "departing");
        verify(ownerRequestRepository).deleteByGuildIdAndOwnerUserId("guild", "departing");
        verify(restraintRequestRepository).deleteByGuildIdAndTargetUserId("guild", "departing");
        verify(restraintRequestRepository).deleteByGuildIdAndActorUserId("guild", "departing");
    }
}
