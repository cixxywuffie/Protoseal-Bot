package com.cixtrowolf.protoseal.persistence.consent;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class ConsentService {

    private final ConsentSettingRepository repository;
    private final ConsentOwnerRequestRepository ownerRequestRepository;
    private final ConsentRestraintRequestRepository restraintRequestRepository;

    public ConsentService(ConsentSettingRepository repository,
                          ConsentOwnerRequestRepository ownerRequestRepository,
                          ConsentRestraintRequestRepository restraintRequestRepository) {
        this.repository = repository;
        this.ownerRequestRepository = ownerRequestRepository;
        this.restraintRequestRepository = restraintRequestRepository;
    }

    @Transactional(readOnly = true)
    public boolean canManageRestraints(String guildId, String targetUserId, String actorUserId) {
        var setting = repository.findByGuildIdAndUserId(guildId, targetUserId).orElse(null);
        ConsentMode mode = setting == null ? ConsentMode.SELF_ONLY : setting.getMode();

        return switch (mode) {
            case SELF_ONLY -> targetUserId.equals(actorUserId);
            case ASK -> targetUserId.equals(actorUserId);
            case EXPOSED -> true;
            case OWNER -> setting != null && actorUserId.equals(setting.getOwnerUserId());
            case DISABLED -> false;
        };
    }

    @Transactional(readOnly = true)
    public ConsentStatus getConsentStatus(String guildId, String userId) {
        return repository.findByGuildIdAndUserId(guildId, userId)
                .map(setting -> new ConsentStatus(setting.getMode(), setting.getOwnerUserId()))
                .orElseGet(() -> new ConsentStatus(ConsentMode.SELF_ONLY, null));
    }

    @Transactional
    public void updateConsent(String guildId, String userId, ConsentMode mode, String ownerUserId) {
        ownerRequestRepository.deleteByGuildIdAndRequesterUserId(guildId, userId);
        restraintRequestRepository.deleteByGuildIdAndTargetUserId(guildId, userId);
        repository.findByGuildIdAndUserId(guildId, userId)
                .ifPresentOrElse(
                        setting -> setting.update(mode, ownerUserId),
                        () -> repository.save(new ConsentSetting(guildId, userId, mode, ownerUserId)));
    }

    @Transactional
    public void resetConsent(String guildId, String userId) {
        updateConsent(guildId, userId, ConsentMode.SELF_ONLY, null);
    }

    @Transactional
    public String createOwnerRequest(String guildId, String requesterUserId, String ownerUserId) {
        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(Duration.ofHours(24));
        ownerRequestRepository.findByGuildIdAndRequesterUserId(guildId, requesterUserId)
                .ifPresentOrElse(
                        request -> request.update(token, ownerUserId, expiresAt),
                        () -> ownerRequestRepository.save(new ConsentOwnerRequest(
                                token, guildId, requesterUserId, ownerUserId, expiresAt)));
        return token;
    }

    @Transactional
    public void cancelOwnerRequest(String guildId, String requesterUserId) {
        ownerRequestRepository.deleteByGuildIdAndRequesterUserId(guildId, requesterUserId);
    }

    @Transactional(readOnly = true)
    public boolean requiresRestraintApproval(String guildId, String targetUserId, String actorUserId) {
        if (targetUserId.equals(actorUserId)) return false;
        return repository.findByGuildIdAndUserId(guildId, targetUserId)
                .map(setting -> setting.getMode() == ConsentMode.ASK)
                .orElse(false);
    }

    @Transactional
    public String createRestraintRequest(String guildId, String targetUserId, String actorUserId, String channelId,
                                         com.cixtrowolf.protoseal.model.restraint.RestraintZone zone,
                                         int level, String name) {
        String token = UUID.randomUUID().toString();
        restraintRequestRepository.save(new ConsentRestraintRequest(token, guildId, targetUserId, actorUserId, channelId,
                zone, level, name, Instant.now().plus(Duration.ofMinutes(5))));
        return token;
    }

    @Transactional
    public void cancelRestraintRequest(String token) {
        restraintRequestRepository.findByToken(token).ifPresent(restraintRequestRepository::delete);
    }

    @Transactional
    public RestraintRequestResponse respondToRestraintRequest(String token, String actorUserId, boolean accepted) {
        var request = restraintRequestRepository.findByToken(token).orElse(null);
        if (request == null) return new RestraintRequestResponse(RestraintRequestResult.NOT_FOUND, null);
        if (!request.getTargetUserId().equals(actorUserId)) {
            return new RestraintRequestResponse(RestraintRequestResult.NOT_TARGET_USER, null);
        }
        restraintRequestRepository.delete(request);
        if (request.getExpiresAt().isBefore(Instant.now())) {
            return new RestraintRequestResponse(RestraintRequestResult.EXPIRED, null);
        }
        var setting = repository.findByGuildIdAndUserId(request.getGuildId(), request.getTargetUserId()).orElse(null);
        if (setting == null || setting.getMode() != ConsentMode.ASK) {
            return new RestraintRequestResponse(RestraintRequestResult.CONSENT_CHANGED, null);
        }
        return new RestraintRequestResponse(
                accepted ? RestraintRequestResult.ACCEPTED : RestraintRequestResult.REJECTED, request);
    }

    @Transactional
    public OwnerRequestResponse respondToOwnerRequest(String token, String actorUserId, boolean accepted) {
        var request = ownerRequestRepository.findByToken(token).orElse(null);
        if (request == null) {
            return new OwnerRequestResponse(OwnerRequestResult.NOT_FOUND, null);
        }
        if (!request.getOwnerUserId().equals(actorUserId)) {
            return new OwnerRequestResponse(OwnerRequestResult.NOT_INVITED_USER, request.getRequesterUserId());
        }
        if (request.getExpiresAt().isBefore(Instant.now())) {
            ownerRequestRepository.delete(request);
            return new OwnerRequestResponse(OwnerRequestResult.EXPIRED, request.getRequesterUserId());
        }

        ownerRequestRepository.delete(request);
        if (!accepted) {
            return new OwnerRequestResponse(OwnerRequestResult.REJECTED, request.getRequesterUserId());
        }

        repository.findByGuildIdAndUserId(request.getGuildId(), request.getRequesterUserId())
                .ifPresentOrElse(
                        setting -> setting.update(ConsentMode.OWNER, request.getOwnerUserId()),
                        () -> repository.save(new ConsentSetting(request.getGuildId(), request.getRequesterUserId(),
                                ConsentMode.OWNER, request.getOwnerUserId())));
        return new OwnerRequestResponse(OwnerRequestResult.ACCEPTED, request.getRequesterUserId());
    }

    public enum OwnerRequestResult {
        ACCEPTED,
        REJECTED,
        EXPIRED,
        NOT_FOUND,
        NOT_INVITED_USER
    }

    public record OwnerRequestResponse(OwnerRequestResult result, String requesterUserId) {
    }

    public enum RestraintRequestResult { ACCEPTED, REJECTED, EXPIRED, NOT_FOUND, NOT_TARGET_USER, CONSENT_CHANGED }
    public record RestraintRequestResponse(RestraintRequestResult result, ConsentRestraintRequest request) {}

    public record ConsentStatus(ConsentMode mode, String ownerUserId) {
    }
}
