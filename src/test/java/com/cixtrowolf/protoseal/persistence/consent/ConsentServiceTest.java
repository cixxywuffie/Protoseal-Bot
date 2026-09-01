package com.cixtrowolf.protoseal.persistence.consent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsentServiceTest {

    @Mock
    private ConsentSettingRepository settingRepository;

    @Mock
    private ConsentOwnerRequestRepository ownerRequestRepository;

    @Mock
    private ConsentRestraintRequestRepository restraintRequestRepository;

    private ConsentService consentService;

    @BeforeEach
    void setUp() {
        consentService = new ConsentService(settingRepository, ownerRequestRepository, restraintRequestRepository);
    }

    @Test
    void defaultsToSelfOnly() {
        when(settingRepository.findByGuildIdAndUserId("guild", "target")).thenReturn(Optional.empty());

        assertTrue(consentService.canManageRestraints("guild", "target", "target"));
        assertFalse(consentService.canManageRestraints("guild", "target", "other"));
    }

    @Test
    void statusDefaultsToSelfOnlyWhenNoSettingExists() {
        when(settingRepository.findByGuildIdAndUserId("guild", "target")).thenReturn(Optional.empty());

        var status = consentService.getConsentStatus("guild", "target");

        assertEquals(ConsentMode.SELF_ONLY, status.mode());
        assertEquals(null, status.ownerUserId());
    }

    @Test
    void statusIncludesTheSelectedOwner() {
        var setting = new ConsentSetting("guild", "target", ConsentMode.OWNER, "owner");
        when(settingRepository.findByGuildIdAndUserId("guild", "target")).thenReturn(Optional.of(setting));

        var status = consentService.getConsentStatus("guild", "target");

        assertEquals(ConsentMode.OWNER, status.mode());
        assertEquals("owner", status.ownerUserId());
    }

    @Test
    void exposedAllowsEveryUser() {
        var setting = new ConsentSetting("guild", "target", ConsentMode.EXPOSED, null);
        when(settingRepository.findByGuildIdAndUserId("guild", "target")).thenReturn(Optional.of(setting));

        assertTrue(consentService.canManageRestraints("guild", "target", "other"));
    }

    @Test
    void askAllowsSelfAndRequiresApprovalForOtherUsers() {
        var setting = new ConsentSetting("guild", "target", ConsentMode.ASK, null);
        when(settingRepository.findByGuildIdAndUserId("guild", "target")).thenReturn(Optional.of(setting));

        assertTrue(consentService.canManageRestraints("guild", "target", "target"));
        assertFalse(consentService.canManageRestraints("guild", "target", "other"));
        assertTrue(consentService.requiresRestraintApproval("guild", "target", "other"));
        assertFalse(consentService.requiresRestraintApproval("guild", "target", "target"));
    }

    @Test
    void acceptedAskRequestIsSingleUseAndReturnsTheApprovedAction() {
        var setting = new ConsentSetting("guild", "target", ConsentMode.ASK, null);
        var request = new ConsentRestraintRequest("token", "guild", "target", "actor",
                com.cixtrowolf.protoseal.model.restraint.RestraintZone.GAG, 1, "ball gag",
                Instant.now().plusSeconds(60));
        when(restraintRequestRepository.findByToken("token")).thenReturn(Optional.of(request));
        when(settingRepository.findByGuildIdAndUserId("guild", "target")).thenReturn(Optional.of(setting));

        var response = consentService.respondToRestraintRequest("token", "target", true);

        assertEquals(ConsentService.RestraintRequestResult.ACCEPTED, response.result());
        assertEquals(request, response.request());
        verify(restraintRequestRepository).delete(request);
    }

    @Test
    void ownerAllowsOnlyTheSelectedOwner() {
        var setting = new ConsentSetting("guild", "target", ConsentMode.OWNER, "owner");
        when(settingRepository.findByGuildIdAndUserId("guild", "target")).thenReturn(Optional.of(setting));

        assertTrue(consentService.canManageRestraints("guild", "target", "owner"));
        assertFalse(consentService.canManageRestraints("guild", "target", "target"));
        assertFalse(consentService.canManageRestraints("guild", "target", "other"));
    }

    @Test
    void disabledRejectsEveryUser() {
        var setting = new ConsentSetting("guild", "target", ConsentMode.DISABLED, null);
        when(settingRepository.findByGuildIdAndUserId("guild", "target")).thenReturn(Optional.of(setting));

        assertFalse(consentService.canManageRestraints("guild", "target", "target"));
        assertFalse(consentService.canManageRestraints("guild", "target", "other"));
    }

    @Test
    void updatingConsentCancelsPendingRequestAndClearsAnOldOwner() {
        var setting = new ConsentSetting("guild", "target", ConsentMode.OWNER, "old-owner");
        when(settingRepository.findByGuildIdAndUserId("guild", "target")).thenReturn(Optional.of(setting));

        consentService.updateConsent("guild", "target", ConsentMode.EXPOSED, null);

        verify(ownerRequestRepository).deleteByGuildIdAndRequesterUserId("guild", "target");
        verify(restraintRequestRepository).deleteByGuildIdAndTargetUserId("guild", "target");
        assertEquals(ConsentMode.EXPOSED, setting.getMode());
        assertEquals(null, setting.getOwnerUserId());
        verify(settingRepository, never()).save(any());
    }

    @Test
    void creatingOwnerRequestPersistsATokenThatExpiresInTwentyFourHours() {
        when(ownerRequestRepository.findByGuildIdAndRequesterUserId("guild", "target"))
                .thenReturn(Optional.empty());
        var before = Instant.now().plusSeconds(24 * 60 * 60 - 2);

        String token = consentService.createOwnerRequest("guild", "target", "owner");

        var captor = org.mockito.ArgumentCaptor.forClass(ConsentOwnerRequest.class);
        verify(ownerRequestRepository).save(captor.capture());
        var request = captor.getValue();
        assertNotNull(token);
        assertFalse(token.isBlank());
        assertEquals(token, request.getToken());
        assertEquals("guild", request.getGuildId());
        assertEquals("target", request.getRequesterUserId());
        assertEquals("owner", request.getOwnerUserId());
        assertTrue(request.getExpiresAt().isAfter(before));
        assertTrue(request.getExpiresAt().isBefore(Instant.now().plusSeconds(24 * 60 * 60 + 2)));
    }

    @Test
    void onlyInvitedOwnerCanAnswerARequest() {
        var request = requestExpiringAt(Instant.now().plusSeconds(60));
        when(ownerRequestRepository.findByToken("token")).thenReturn(Optional.of(request));

        var response = consentService.respondToOwnerRequest("token", "intruder", true);

        assertEquals(ConsentService.OwnerRequestResult.NOT_INVITED_USER, response.result());
        assertEquals("target", response.requesterUserId());
        verify(ownerRequestRepository, never()).delete(any());
        verify(settingRepository, never()).save(any());
    }

    @Test
    void expiredOwnerRequestCannotChangeConsent() {
        var request = requestExpiringAt(Instant.now().minusSeconds(1));
        when(ownerRequestRepository.findByToken("token")).thenReturn(Optional.of(request));

        var response = consentService.respondToOwnerRequest("token", "owner", true);

        assertEquals(ConsentService.OwnerRequestResult.EXPIRED, response.result());
        verify(ownerRequestRepository).delete(request);
        verify(settingRepository, never()).save(any());
    }

    @Test
    void rejectingOwnerRequestDeletesItWithoutChangingConsent() {
        var request = requestExpiringAt(Instant.now().plusSeconds(60));
        when(ownerRequestRepository.findByToken("token")).thenReturn(Optional.of(request));

        var response = consentService.respondToOwnerRequest("token", "owner", false);

        assertEquals(ConsentService.OwnerRequestResult.REJECTED, response.result());
        verify(ownerRequestRepository).delete(request);
        verify(settingRepository, never()).findByGuildIdAndUserId(any(), any());
        verify(settingRepository, never()).save(any());
    }

    @Test
    void acceptingOwnerRequestActivatesOwnerConsent() {
        var request = requestExpiringAt(Instant.now().plusSeconds(60));
        var setting = new ConsentSetting("guild", "target", ConsentMode.SELF_ONLY, null);
        when(ownerRequestRepository.findByToken("token")).thenReturn(Optional.of(request));
        when(settingRepository.findByGuildIdAndUserId("guild", "target")).thenReturn(Optional.of(setting));

        var response = consentService.respondToOwnerRequest("token", "owner", true);

        assertEquals(ConsentService.OwnerRequestResult.ACCEPTED, response.result());
        assertEquals(ConsentMode.OWNER, setting.getMode());
        assertEquals("owner", setting.getOwnerUserId());
        verify(ownerRequestRepository).delete(request);
    }

    @Test
    void unknownOwnerRequestReturnsNotFound() {
        when(ownerRequestRepository.findByToken("missing")).thenReturn(Optional.empty());

        var response = consentService.respondToOwnerRequest("missing", "owner", true);

        assertEquals(ConsentService.OwnerRequestResult.NOT_FOUND, response.result());
        assertEquals(null, response.requesterUserId());
    }

    private ConsentOwnerRequest requestExpiringAt(Instant expiresAt) {
        return new ConsentOwnerRequest("token", "guild", "target", "owner", expiresAt);
    }
}
