package com.cixtrowolf.protoseal.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class EnumPersistenceMappingTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void extensibleEnumsAreStoredAsVarcharColumns() {
        assertEquals("CHARACTER VARYING", columnType("RESTRAINT_STATES", "LOCK_TYPE"));
        assertEquals("CHARACTER VARYING", columnType("CONSENT_SETTINGS", "MODE"));
    }

    private String columnType(String table, String column) {
        return jdbcTemplate.queryForObject("""
                SELECT DATA_TYPE
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = ? AND COLUMN_NAME = ?
                """, String.class, table, column);
    }
}
