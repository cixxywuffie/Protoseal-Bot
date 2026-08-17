package com.cixtrowolf.protoseal.model.restraint;

import java.util.Arrays;

public enum RestraintZone {
    LEGS(1, "Legs", "🦵"),
    ARMS(2, "Arms", "⛓️"),
    GAG(3, "Gag", "🤐"),
    HOOD(4, "Hood", "🪖"),
    STRAITJACKET(5, "Straitjacket", "🧺"),
    SUIT(6, "Suit", "🥻"),
    MITTS(7, "Mitts", "🧤"),
    CHASTITY(8, "Chastity", "🔐"),
    BLINDFOLD(9, "Blindfold", "🙈");

    private final int databaseId;
    private final String displayName;
    private final String emoji;

    RestraintZone(int databaseId, String displayName, String emoji) {
        this.databaseId = databaseId;
        this.displayName = displayName;
        this.emoji = emoji;
    }

    public int getDatabaseId() {
        return databaseId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmoji() {
        return emoji;
    }

    public static RestraintZone fromDatabaseId(int databaseId) {
        return Arrays.stream(values())
                .filter(zone -> zone.databaseId == databaseId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown restraint zone: " + databaseId));
    }
}
