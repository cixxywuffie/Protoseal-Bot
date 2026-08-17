package com.cixtrowolf.protoseal.commands.restraint;

import java.util.Optional;

public record PermalockConfirmation(Action action, String guildId, String targetUserId, String actorUserId) {

    private static final String PREFIX = "permalock";

    public enum Action {
        CONFIRM,
        CANCEL
    }

    public String customId() {
        return String.join(":", PREFIX, action.name().toLowerCase(), guildId, targetUserId, actorUserId);
    }

    public static Optional<PermalockConfirmation> parse(String customId) {
        if (customId == null || !customId.startsWith(PREFIX + ":")) {
            return Optional.empty();
        }

        String[] parts = customId.split(":", -1);
        if (parts.length != 5 || !isSnowflake(parts[2]) || !isSnowflake(parts[3]) || !isSnowflake(parts[4])) {
            return Optional.empty();
        }

        try {
            return Optional.of(new PermalockConfirmation(
                    Action.valueOf(parts[1].toUpperCase()), parts[2], parts[3], parts[4]));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static boolean isSnowflake(String value) {
        return value.matches("[0-9]{1,20}");
    }
}
