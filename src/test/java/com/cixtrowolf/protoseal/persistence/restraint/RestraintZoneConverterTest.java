package com.cixtrowolf.protoseal.persistence.restraint;

import com.cixtrowolf.protoseal.model.restraint.RestraintZone;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RestraintZoneConverterTest {

    private final RestraintZoneConverter converter = new RestraintZoneConverter();

    @Test
    void everyZoneRoundTripsThroughItsStableDatabaseId() {
        Stream.of(RestraintZone.values()).forEach(zone ->
                assertEquals(zone, converter.convertToEntityAttribute(converter.convertToDatabaseColumn(zone))));
    }

    @Test
    void nullValuesRemainNull() {
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void unknownDatabaseIdIsRejectedInsteadOfSilentlyMapped() {
        assertThrows(IllegalArgumentException.class, () -> converter.convertToEntityAttribute(999));
    }
}
