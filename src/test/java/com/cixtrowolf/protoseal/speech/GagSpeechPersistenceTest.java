package com.cixtrowolf.protoseal.speech;

import com.cixtrowolf.protoseal.model.restraint.GagIntensity;
import com.cixtrowolf.protoseal.model.restraint.RestraintZone;
import com.cixtrowolf.protoseal.persistence.consent.ConsentService;
import com.cixtrowolf.protoseal.persistence.restraint.RestraintStateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({ConsentService.class, RestraintStateService.class, GagSpeechService.class, GagSpeechTransformer.class})
class GagSpeechPersistenceTest {
    @Autowired private RestraintStateService restraints;
    @Autowired private ConsentService consent;
    @Autowired private GagSpeechService speech;

    @Test
    void preferenceSurvivesRemovalAndSafewordAndIsScopedToGuild() {
        assertEquals(RestraintStateService.StateUpdateResult.UPDATED,
                restraints.saveState("g1", "user", RestraintZone.GAG, 1, "user", "ballgag", GagIntensity.STRONG));
        assertEquals(GagIntensity.STRONG, consent.getGagIntensity("g1", "user"));
        assertEquals(GagIntensity.DEFAULT, consent.getGagIntensity("g2", "user"));
        assertTrue(speech.transform("g1", "user", "Hello world").isPresent());

        assertEquals(RestraintStateService.StateUpdateResult.UPDATED,
                restraints.saveState("g1", "user", RestraintZone.GAG, 0, "user", "none"));
        assertTrue(speech.transform("g1", "user", "Hello world").isEmpty());
        restraints.clearStates("g1", "user");
        assertEquals(GagIntensity.STRONG, consent.getGagIntensity("g1", "user"));
        assertTrue(speech.transform("g1", "user", "Hello world").isEmpty());
    }

    @Test
    void changingIntensityOnlyPreservesTheExistingGagType() {
        assertEquals(RestraintStateService.StateUpdateResult.UPDATED,
                restraints.saveState("g1", "user", RestraintZone.GAG, 3, "user", "tape"));

        assertEquals(RestraintStateService.GagIntensityUpdateResult.UPDATED,
                restraints.updateGagIntensity("g1", "user", "user", GagIntensity.STRONG));

        var state = restraints.findActiveStates("g1", "user").getFirst();
        assertEquals(3, state.getLevel());
        assertEquals("tape", state.getName());
        assertEquals(GagIntensity.STRONG, consent.getGagIntensity("g1", "user"));
    }
}
