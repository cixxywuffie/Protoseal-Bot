package com.cixtrowolf.protoseal.model.restraint;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestraintDefinitionTest {

    @Test
    void exposesEveryRegisteredRestraintCommand() {
        Set<String> expectedCommands = Set.of(
                "armcuffs", "legcuffs", "gag", "hood", "straitjacket",
                "suits", "mitts", "chastity", "blindfold", "collar", "confine", "encase");

        Set<String> actualCommands = Stream.of(RestraintDefinition.values())
                .map(RestraintDefinition::getCommandName)
                .collect(Collectors.toSet());

        assertEquals(expectedCommands, actualCommands);
        expectedCommands.forEach(command -> assertTrue(RestraintDefinition.fromCommandName(command).isPresent()));
    }

    @Test
    void everyDefinitionHasUsableLevels() {
        Stream.of(RestraintDefinition.values())
                .forEach(definition -> {
                    assertTrue(definition.getMaximumLevel() >= 3);
                    for (int level = 0; level <= definition.getMaximumLevel(); level++) {
                        assertFalse(definition.getLevel(level).message().isBlank());
                        assertFalse(definition.getLevel(level).selfMessage().isBlank());
                    }
                });
    }

    @Test
    void commandNamesZonesAndDatabaseIdsStayUnique() {
        assertEquals(RestraintDefinition.values().length,
                Stream.of(RestraintDefinition.values()).map(RestraintDefinition::getCommandName).distinct().count());
        assertEquals(RestraintDefinition.values().length,
                Stream.of(RestraintDefinition.values()).map(RestraintDefinition::getZone).distinct().count());
        assertEquals(RestraintZone.values().length,
                Stream.of(RestraintZone.values()).map(RestraintZone::getDatabaseId).distinct().count());
        assertTrue(RestraintDefinition.fromCommandName("unknown").isEmpty());
    }
}
