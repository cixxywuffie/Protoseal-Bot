package com.cixtrowolf.protoseal.persistence.channel;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuildSpeechSettingsService {
    private final GuildSpeechSettingsRepository repository;

    public GuildSpeechSettingsService(GuildSpeechSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public boolean isEnabled(String guildId) {
        return repository.findById(guildId).map(GuildSpeechSettings::isEnabled).orElse(false);
    }

    @Transactional
    public void setEnabled(String guildId, boolean enabled) {
        var settings = repository.findById(guildId).orElseGet(() -> new GuildSpeechSettings(guildId, enabled));
        settings.setEnabled(enabled);
        repository.save(settings);
    }
}
