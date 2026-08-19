package com.cixtrowolf.protoseal.persistence.restraint;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cixtrowolf.protoseal.persistence.consent.ConsentService;
import com.cixtrowolf.protoseal.model.restraint.RestraintZone;
import com.cixtrowolf.protoseal.model.restraint.RestraintLockType;

import java.util.List;

@Service
public class RestraintStateService {

    private final RestraintStateRepository repository;
    private final ConsentService consentService;

    public RestraintStateService(RestraintStateRepository repository, ConsentService consentService) {
        this.repository = repository;
        this.consentService = consentService;
    }

    @Transactional
    public StateUpdateResult saveState(String guildId, String userId, RestraintZone zone, int level, String actorId, String name) {
        if (!consentService.canManageRestraints(guildId, userId, actorId)) {
            return StateUpdateResult.CONSENT_DENIED;
        }
        var activeStates = repository
                .findByGuildIdAndUserIdAndLevelGreaterThanOrderByZoneAsc(guildId, userId, 0);
        if (zone != RestraintZone.MITTS
                && activeStates.stream().anyMatch(state -> state.getZone() == RestraintZone.MITTS)) {
            return StateUpdateResult.MITTS_ACTIVE;
        }
        var activeLock = activeStates
                .stream()
                .filter(RestraintState::isLocked)
                .findFirst();
        var existingState = repository.findByGuildIdAndUserIdAndZone(guildId, userId, zone);
        if (existingState.isPresent()) {
            var state = existingState.get();
            if (state.isLocked()) {
                return StateUpdateResult.LOCKED;
            }
            state.updateLevel(level,name);
            if (level > 0 && activeLock.isPresent()) {
                var lock = activeLock.get();
                state.applyLock(lock.getLockType(), lock.getLockedByUserId());
            }
        } else {
            var state = new RestraintState(guildId, userId, zone, level, name);
            if (level > 0 && activeLock.isPresent()) {
                var lock = activeLock.get();
                state.applyLock(lock.getLockType(), lock.getLockedByUserId());
            }
            repository.save(state);
        }
        return StateUpdateResult.UPDATED;
    }

    @Transactional
    public LockResult updateLocks(String guildId, String userId, RestraintLockType lockType, String actorId) {
        if (!consentService.canManageRestraints(guildId, userId, actorId)) {
            return LockResult.CONSENT_DENIED;
        }
        var states = repository.findByGuildIdAndUserIdAndLevelGreaterThanOrderByZoneAsc(guildId, userId, 0);
        if (states.isEmpty()) {
            return LockResult.NO_ACTIVE_RESTRAINT;
        }
        if (states.stream().anyMatch(state -> state.getLockType() == RestraintLockType.PERMALOCK)) {
            return LockResult.PERMALOCKED;
        }
        if (states.stream().anyMatch(state -> state.isLocked()
                && !state.getLockedByUserId().equals(actorId))) {
            return LockResult.LOCKED_BY_ANOTHER_USER;
        }
        if (lockType == null) {
            if (states.stream().noneMatch(RestraintState::isLocked)) {
                return LockResult.NOT_LOCKED;
            }
            states.stream().filter(RestraintState::isLocked).forEach(RestraintState::removeLock);
            return LockResult.REMOVED;
        }
        states.forEach(state -> state.applyLock(lockType, actorId));
        return LockResult.APPLIED;
    }

    @Transactional
    public void clearStates(String guildId, String userId) {
        repository.deleteByGuildIdAndUserId(guildId, userId);
    }

    @Transactional(readOnly = true)
    public List<RestraintState> findActiveStates(String guildId, String userId) {
        return repository.findByGuildIdAndUserIdAndLevelGreaterThanOrderByZoneAsc(guildId, userId, 0);
    }

    public enum LockResult {
        APPLIED,
        REMOVED,
        NO_ACTIVE_RESTRAINT,
        NOT_LOCKED,
        PERMALOCKED,
        LOCKED_BY_ANOTHER_USER,
        CONSENT_DENIED
    }

    public enum StateUpdateResult {
        UPDATED,
        LOCKED,
        MITTS_ACTIVE,
        CONSENT_DENIED
    }
}
