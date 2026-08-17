package com.cixtrowolf.protoseal.model.restraint;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RestraintLockTypeTest {

    @Test
    void exposesEverySupportedLockType() {
        Set<String> expected = Set.of("PADLOCK", "GLUE", "SEWN", "TAPE", "PERMALOCK");
        Set<String> actual = Stream.of(RestraintLockType.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertEquals(expected, actual);
    }

    @Test
    void everyLockTypeHasDisplayMetadata() {
        Stream.of(RestraintLockType.values()).forEach(lockType -> {
            assertFalse(lockType.getDisplayName().isBlank());
            assertFalse(lockType.getEmoji().isBlank());
        });
    }
}
