package com.cixtrowolf.protoseal.persistence.restraint;

import com.cixtrowolf.protoseal.model.restraint.RestraintZone;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class RestraintZoneConverter implements AttributeConverter<RestraintZone, Integer> {

    @Override
    public Integer convertToDatabaseColumn(RestraintZone zone) {
        return zone == null ? null : zone.getDatabaseId();
    }

    @Override
    public RestraintZone convertToEntityAttribute(Integer databaseId) {
        return databaseId == null ? null : RestraintZone.fromDatabaseId(databaseId);
    }
}
