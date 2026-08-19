package com.cixtrowolf.protoseal.persistence.restraint;

import com.cixtrowolf.protoseal.model.restraint.RestraintLockType;
import com.cixtrowolf.protoseal.model.restraint.RestraintZone;
import com.cixtrowolf.protoseal.persistence.consent.ConsentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestraintStateServiceTest {

    @Mock RestraintStateRepository repository;
    @Mock ConsentService consentService;
    private RestraintStateService service;

    @BeforeEach
    void setUp() {
        service = new RestraintStateService(repository, consentService);
    }

    @Test
    void deniedConsentPreventsAnyStateLookupOrWrite() {
        when(consentService.canManageRestraints("guild", "target", "actor")).thenReturn(false);

        var result = service.saveState("guild", "target", RestraintZone.GAG, 1, "actor", "ball gag");

        assertEquals(RestraintStateService.StateUpdateResult.CONSENT_DENIED, result);
        verify(repository, never()).findByGuildIdAndUserIdAndLevelGreaterThanOrderByZoneAsc(any(), any(), any(Integer.class));
        verify(repository, never()).save(any());
    }

    @Test
    void lockedRestraintCannotBeChangedEvenByItsLocker() {
        allowConsent();
        var state = state(RestraintZone.GAG, 1, "ball gag");
        state.applyLock(RestraintLockType.PADLOCK, "actor");
        when(repository.findByGuildIdAndUserIdAndLevelGreaterThanOrderByZoneAsc("guild", "target", 0))
                .thenReturn(List.of(state));
        when(repository.findByGuildIdAndUserIdAndZone("guild", "target", RestraintZone.GAG))
                .thenReturn(Optional.of(state));

        var result = service.saveState("guild", "target", RestraintZone.GAG, 0, "actor", "none");

        assertEquals(RestraintStateService.StateUpdateResult.LOCKED, result);
        assertEquals(1, state.getLevel());
    }

    @Test
    void activeMittsPreventActivatingAnotherRestraint() {
        allowConsent();
        var mitts = state(RestraintZone.MITTS, 1, "default");
        when(repository.findByGuildIdAndUserIdAndLevelGreaterThanOrderByZoneAsc("guild", "target", 0))
                .thenReturn(List.of(mitts));

        var result = service.saveState("guild", "target", RestraintZone.GAG, 1, "actor", "default");

        assertEquals(RestraintStateService.StateUpdateResult.MITTS_ACTIVE, result);
        verify(repository, never()).findByGuildIdAndUserIdAndZone(any(), any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void activeMittsPreventAnotherRestraintFromBeingRemoved() {
        allowConsent();
        var mitts = state(RestraintZone.MITTS, 1, "default");
        var gag = state(RestraintZone.GAG, 2, "bit gag");
        when(repository.findByGuildIdAndUserIdAndLevelGreaterThanOrderByZoneAsc("guild", "target", 0))
                .thenReturn(List.of(mitts, gag));
        var result = service.saveState("guild", "target", RestraintZone.GAG, 0, "actor", "none");

        assertEquals(RestraintStateService.StateUpdateResult.MITTS_ACTIVE, result);
        assertEquals(2, gag.getLevel());
        verify(repository, never()).findByGuildIdAndUserIdAndZone(any(), any(), any());
    }

    @Test
    void activeMittsCanStillBeChangedOrRemoved() {
        allowConsent();
        var mitts = state(RestraintZone.MITTS, 1, "default");
        when(repository.findByGuildIdAndUserIdAndLevelGreaterThanOrderByZoneAsc("guild", "target", 0))
                .thenReturn(List.of(mitts));
        when(repository.findByGuildIdAndUserIdAndZone("guild", "target", RestraintZone.MITTS))
                .thenReturn(Optional.of(mitts));

        var result = service.saveState("guild", "target", RestraintZone.MITTS, 0, "actor", "none");

        assertEquals(RestraintStateService.StateUpdateResult.UPDATED, result);
        assertEquals(0, mitts.getLevel());
    }

    @Test
    void restraintAddedDuringGlobalLockInheritsThatLock() {
        allowConsent();
        var lockedState = state(RestraintZone.GAG, 1, "ball gag");
        lockedState.applyLock(RestraintLockType.PERMALOCK, "locker");
        when(repository.findByGuildIdAndUserIdAndLevelGreaterThanOrderByZoneAsc("guild", "target", 0))
                .thenReturn(List.of(lockedState));
        when(repository.findByGuildIdAndUserIdAndZone("guild", "target", RestraintZone.MITTS))
                .thenReturn(Optional.empty());

        assertEquals(RestraintStateService.StateUpdateResult.UPDATED,
                service.saveState("guild", "target", RestraintZone.MITTS, 2, "actor", "puppy"));

        var captor = ArgumentCaptor.forClass(RestraintState.class);
        verify(repository).save(captor.capture());
        assertEquals(2, captor.getValue().getLevel());
        assertEquals("puppy", captor.getValue().getName());
        assertEquals(RestraintLockType.PERMALOCK, captor.getValue().getLockType());
        assertEquals("locker", captor.getValue().getLockedByUserId());
    }

    @Test
    void applyingLockAffectsEveryActiveRestraint() {
        allowConsent();
        var gag = state(RestraintZone.GAG, 1, "ball gag");
        var mitts = state(RestraintZone.MITTS, 2, "puppy");
        when(repository.findByGuildIdAndUserIdAndLevelGreaterThanOrderByZoneAsc("guild", "target", 0))
                .thenReturn(List.of(gag, mitts));

        var result = service.updateLocks("guild", "target", RestraintLockType.TAPE, "actor");

        assertEquals(RestraintStateService.LockResult.APPLIED, result);
        assertEquals(RestraintLockType.TAPE, gag.getLockType());
        assertEquals(RestraintLockType.TAPE, mitts.getLockType());
        assertEquals("actor", gag.getLockedByUserId());
        assertEquals("actor", mitts.getLockedByUserId());
    }

    @Test
    void foreignLockPreventsReplacingOrRemovingAnyLock() {
        allowConsent();
        var foreign = state(RestraintZone.GAG, 1, "ball gag");
        foreign.applyLock(RestraintLockType.PADLOCK, "another-user");
        var unlocked = state(RestraintZone.MITTS, 1, "default");
        when(repository.findByGuildIdAndUserIdAndLevelGreaterThanOrderByZoneAsc("guild", "target", 0))
                .thenReturn(List.of(foreign, unlocked));

        var result = service.updateLocks("guild", "target", null, "actor");

        assertEquals(RestraintStateService.LockResult.LOCKED_BY_ANOTHER_USER, result);
        assertTrue(foreign.isLocked());
        assertFalse(unlocked.isLocked());
    }

    @Test
    void permalockCannotBeRemovedOrReplacedEvenByItsLocker() {
        allowConsent();
        var state = state(RestraintZone.GAG, 1, "ball gag");
        state.applyLock(RestraintLockType.PERMALOCK, "actor");
        when(repository.findByGuildIdAndUserIdAndLevelGreaterThanOrderByZoneAsc("guild", "target", 0))
                .thenReturn(List.of(state));

        assertEquals(RestraintStateService.LockResult.PERMALOCKED,
                service.updateLocks("guild", "target", null, "actor"));
        assertEquals(RestraintStateService.LockResult.PERMALOCKED,
                service.updateLocks("guild", "target", RestraintLockType.PADLOCK, "actor"));
        assertEquals(RestraintLockType.PERMALOCK, state.getLockType());
        assertEquals("actor", state.getLockedByUserId());
    }

    @Test
    void safewordClearStillDeletesPermanentlyLockedStates() {
        service.clearStates("guild", "target");

        verify(repository).deleteByGuildIdAndUserId("guild", "target");
    }

    @Test
    void lockRemovalClearsAllLocksOwnedByActor() {
        allowConsent();
        var gag = state(RestraintZone.GAG, 1,"ball gag");
        var mitts = state(RestraintZone.MITTS, 1, "default");
        gag.applyLock(RestraintLockType.PADLOCK, "actor");
        mitts.applyLock(RestraintLockType.PADLOCK, "actor");
        when(repository.findByGuildIdAndUserIdAndLevelGreaterThanOrderByZoneAsc("guild", "target", 0))
                .thenReturn(List.of(gag, mitts));

        assertEquals(RestraintStateService.LockResult.REMOVED,
                service.updateLocks("guild", "target", null, "actor"));
        assertFalse(gag.isLocked());
        assertFalse(mitts.isLocked());
    }

    @Test
    void lockRequiresAtLeastOneActiveRestraint() {
        allowConsent();
        when(repository.findByGuildIdAndUserIdAndLevelGreaterThanOrderByZoneAsc("guild", "target", 0))
                .thenReturn(List.of());

        assertEquals(RestraintStateService.LockResult.NO_ACTIVE_RESTRAINT,
                service.updateLocks("guild", "target", RestraintLockType.PADLOCK, "actor"));
    }

    @Test
    void clearAndFindDelegateToTheScopedRepositoryOperations() {
        var states = List.of(state(RestraintZone.GAG, 1, "ball gag"));
        when(repository.findByGuildIdAndUserIdAndLevelGreaterThanOrderByZoneAsc("guild", "target", 0))
                .thenReturn(states);

        service.clearStates("guild", "target");

        verify(repository).deleteByGuildIdAndUserId("guild", "target");
        assertEquals(states, service.findActiveStates("guild", "target"));
    }

    private void allowConsent() {
        when(consentService.canManageRestraints("guild", "target", "actor")).thenReturn(true);
    }

    private RestraintState state(RestraintZone zone, int level, String name) {
        return new RestraintState("guild", "target", zone, level, name);
    }
}
