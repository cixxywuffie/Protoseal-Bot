package com.cixtrowolf.protoseal.commands.restraint;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermalockConfirmationTest {

    @Test
    void roundTripsAConfirmationButtonId() {
        var confirmation = new PermalockConfirmation(
                PermalockConfirmation.Action.CONFIRM, "123", "456", "789");

        assertEquals(confirmation, PermalockConfirmation.parse(confirmation.customId()).orElseThrow());
    }

    @Test
    void roundTripsACancellationButtonId() {
        var confirmation = new PermalockConfirmation(
                PermalockConfirmation.Action.CANCEL, "123", "456", "789");

        assertEquals(confirmation, PermalockConfirmation.parse(confirmation.customId()).orElseThrow());
    }

    @Test
    void rejectsMalformedOrUnrelatedButtonIds() {
        assertTrue(PermalockConfirmation.parse("consent-owner:accept:token").isEmpty());
        assertTrue(PermalockConfirmation.parse("permalock:confirm:not-a-snowflake:456:789").isEmpty());
        assertTrue(PermalockConfirmation.parse("permalock:unknown:123:456:789").isEmpty());
    }
}
