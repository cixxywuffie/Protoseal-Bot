package com.cixtrowolf.protoseal.persistence.channel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "guild_speech_settings")
public class GuildSpeechSettings {
    @Id
    @Column(name = "guild_id", length = 32)
    private String guildId;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    protected GuildSpeechSettings() {}

    public GuildSpeechSettings(String guildId, boolean enabled) {
        this.guildId = guildId;
        this.enabled = enabled;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
