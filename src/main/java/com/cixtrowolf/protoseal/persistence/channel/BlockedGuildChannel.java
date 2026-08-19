package com.cixtrowolf.protoseal.persistence.channel;

import jakarta.persistence.*;

@Entity
@Table(name = "blocked_guild_channels", uniqueConstraints = @UniqueConstraint(
        name = "uk_blocked_guild_channel", columnNames = {"guild_id", "channel_id"}))
public class BlockedGuildChannel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "guild_id", nullable = false, length = 32)
    private String guildId;

    @Column(name = "channel_id", nullable = false, length = 32)
    private String channelId;

    protected BlockedGuildChannel() { }

    public BlockedGuildChannel(String guildId, String channelId) {
        this.guildId = guildId;
        this.channelId = channelId;
    }

    public String getChannelId() { return channelId; }
}
