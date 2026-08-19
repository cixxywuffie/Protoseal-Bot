package com.cixtrowolf.protoseal.persistence.restraint;

import com.cixtrowolf.protoseal.model.restraint.RestraintLockType;
import com.cixtrowolf.protoseal.model.restraint.RestraintZone;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestraintStateTest {

    @Test
    void settingLevelToZeroAlsoRemovesItsLock() {
        var state = new RestraintState("guild", "user", RestraintZone.GAG, 2,"bit gag");
        state.applyLock(RestraintLockType.GLUE, "owner");

        state.updateLevel(0,"none");

        assertEquals(0, state.getLevel());
        assertEquals("none", state.getName());
        assertFalse(state.isLocked());
        assertEquals(null, state.getLockType());
        assertEquals(null, state.getLockedByUserId());
    }

    @Test
    void lockIsActiveOnlyWhenTypeAndOwnerArePresent() {
        var state = new RestraintState("guild", "user", RestraintZone.GAG, 1,"ball gag");

        state.applyLock(RestraintLockType.SEWN, "owner");
        assertTrue(state.isLocked());

        state.removeLock();
        assertFalse(state.isLocked());
    }
}
