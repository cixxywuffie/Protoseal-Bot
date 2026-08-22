package com.cixtrowolf.protoseal.persistence.restraint;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cixtrowolf.protoseal.persistence.consent.ConsentService;
import com.cixtrowolf.protoseal.model.restraint.RestraintZone;
import com.cixtrowolf.protoseal.model.restraint.RestraintLockType;

import java.util.List;
import java.time.Duration;
import java.time.Instant;

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
                inheritLock(state, lock);
            }
        } else {
            var state = new RestraintState(guildId, userId, zone, level, name);
            if (level > 0 && activeLock.isPresent()) {
                var lock = activeLock.get();
                inheritLock(state, lock);
            }
            repository.save(state);
        }
        return StateUpdateResult.UPDATED;
    }

    private void inheritLock(RestraintState state, RestraintState source) {
        if (source.getLockType() == RestraintLockType.TIMELOCK) {
            state.applyTimelock(source.getLockedByUserId(), source.getLockExpiresAt());
        } else {
            state.applyLock(source.getLockType(), source.getLockedByUserId());
        }
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
        if (states.stream().anyMatch(state -> state.getLockType() == RestraintLockType.TIMELOCK && state.isLocked())) {
            return LockResult.TIMELOCKED;
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
    public TimelockResult applyTimelock(String guildId, String userId, String actorId, Duration duration) {
        if (!consentService.canManageRestraints(guildId, userId, actorId)) {
            return TimelockResult.CONSENT_DENIED;
        }
        if (duration.isNegative() || duration.isZero() || duration.compareTo(Duration.ofDays(30)) > 0) {
            return TimelockResult.INVALID_DURATION;
        }
        var states = repository.findByGuildIdAndUserIdAndLevelGreaterThanOrderByZoneAsc(guildId, userId, 0);
        if (states.isEmpty()) return TimelockResult.NO_ACTIVE_RESTRAINT;
        if (states.stream().anyMatch(state -> state.getLockType() == RestraintLockType.PERMALOCK)) {
            return TimelockResult.PERMALOCKED;
        }
        if (states.stream().anyMatch(RestraintState::isLocked)) return TimelockResult.ALREADY_LOCKED;

        Instant expiresAt = Instant.now().plus(duration);
        states.forEach(state -> state.applyTimelock(actorId, expiresAt));
        return TimelockResult.APPLIED;
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
        TIMELOCKED,
        LOCKED_BY_ANOTHER_USER,
        CONSENT_DENIED
    }

    public enum StateUpdateResult {
        UPDATED,
        LOCKED,
        MITTS_ACTIVE,
        CONSENT_DENIED
    }

    public enum TimelockResult {
        APPLIED, NO_ACTIVE_RESTRAINT, ALREADY_LOCKED, PERMALOCKED, INVALID_DURATION, CONSENT_DENIED
    }
}
