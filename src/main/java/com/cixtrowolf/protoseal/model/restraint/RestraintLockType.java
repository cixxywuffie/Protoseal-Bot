package com.cixtrowolf.protoseal.model.restraint;

public enum RestraintLockType {
    PADLOCK("Padlock", "🔒"),
    GLUE("Glue", "🧴"),
    SEWN("Sewn", "🪡"),
    TAPE("Tape", "📼"),
    PERMALOCK("Permalock", "♾️"),
    TIMELOCK("Timelock", "⏳");

    private final String displayName;
    private final String emoji;

    RestraintLockType(String displayName, String emoji) {
        this.displayName = displayName;
        this.emoji = emoji;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmoji() {
        return emoji;
    }
}
