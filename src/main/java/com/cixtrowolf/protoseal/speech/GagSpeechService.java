package com.cixtrowolf.protoseal.speech;

import com.cixtrowolf.protoseal.model.restraint.GagIntensity;
import com.cixtrowolf.protoseal.model.restraint.RestraintDefinition;
import com.cixtrowolf.protoseal.model.restraint.RestraintZone;
import com.cixtrowolf.protoseal.persistence.consent.ConsentService;
import com.cixtrowolf.protoseal.persistence.restraint.RestraintStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class GagSpeechService {
    private final RestraintStateRepository restraints;
    private final ConsentService consent;
    private final GagSpeechTransformer transformer;

    public GagSpeechService(RestraintStateRepository restraints, ConsentService consent,
                            GagSpeechTransformer transformer) {
        this.restraints = restraints;
        this.consent = consent;
        this.transformer = transformer;
    }

    @Transactional(readOnly = true)
    public Optional<String> transform(String guildId, String userId, String content) {
        var state = restraints.findByGuildIdAndUserIdAndZone(guildId, userId, RestraintZone.GAG);
        if (state.isEmpty() || state.get().getLevel() <= 0) return Optional.empty();
        int level = state.get().getLevel();
        if (level == RestraintDefinition.REINDEER_MUZZLE_LEVEL) return Optional.of("Hrrn! Hrrn!");
        GagIntensity intensity = consent.getGagIntensity(guildId, userId).resolve(level);
        if (intensity == GagIntensity.NONE) return Optional.empty();
        String transformed = transformer.transform(content, intensity);
        return transformed.equals(content) ? Optional.empty() : Optional.of(transformed);
    }
}
