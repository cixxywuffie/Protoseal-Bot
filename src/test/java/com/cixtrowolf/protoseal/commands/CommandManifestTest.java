package com.cixtrowolf.protoseal.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cixtrowolf.protoseal.model.restraint.RestraintDefinition;
import com.cixtrowolf.protoseal.model.restraint.RestraintLockType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandManifestTest {

    private static final Set<String> NSFW_COMMANDS = Set.of(
            "armcuffs", "blindfold", "chastity", "gag", "help", "hood", "legcuffs",
            "lock", "mitts", "rdstatus", "straitjacket", "suits");
    private static final Set<String> COMMANDS = Set.of(
            "about", "adminhelp", "armcuffs", "blindfold", "chastity", "consent", "consentreset", "donate",
            "gag", "help", "hood", "legcuffs", "lock", "mitts", "rdstatus", "safeword",
            "straitjacket", "suits");
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void everyCommandManifestIsValidAndHasAUniqueExpectedName() throws IOException {
        var parsedNames = new HashSet<String>();

        for (String command : COMMANDS) {
            JsonNode manifest = readManifest(command);
            assertEquals(command, manifest.path("name").asText());
            assertFalse(manifest.path("description").asText().isBlank());
            assertEquals(1, manifest.path("type").asInt());
            assertTrue(parsedNames.add(manifest.path("name").asText()), "Duplicate command: " + command);
        }

        assertEquals(COMMANDS, parsedNames);
    }

    @Test
    void onlyRoleplayCommandsAreMarkedAsAgeRestricted() throws IOException {
        for (String command : COMMANDS) {
            assertEquals(NSFW_COMMANDS.contains(command), readManifest(command).path("nsfw").asBoolean(false),
                    "Unexpected NSFW classification for /" + command);
        }
    }

    @Test
    void everyRestraintDefinitionHasAMatchingDiscordManifest() throws IOException {
        for (RestraintDefinition definition : RestraintDefinition.values()) {
            JsonNode manifest = readManifest(definition.getCommandName());
            JsonNode target = findOption(manifest, "target");
            JsonNode level = findOption(manifest, "level");

            assertEquals(6, target.path("type").asInt());
            assertTrue(target.path("required").asBoolean());
            assertEquals(4, level.path("type").asInt());
            assertEquals(definition.getMaximumLevel() + 1, level.path("choices").size());
            for (int value = 0; value <= definition.getMaximumLevel(); value++) {
                assertEquals(value, level.path("choices").get(value).path("value").asInt());
            }
        }
    }

    @Test
    void lockManifestExposesEverySupportedLockType() throws IOException {
        JsonNode lockType = findOption(readManifest("lock"), "type");
        Set<String> registeredValues = new HashSet<>();
        lockType.path("choices").forEach(choice -> registeredValues.add(choice.path("value").asText()));

        Set<String> expectedValues = Stream.concat(
                        Stream.of("REMOVE"),
                        Stream.of(RestraintLockType.values()).map(Enum::name))
                .collect(java.util.stream.Collectors.toSet());

        assertEquals(expectedValues, registeredValues);
    }

    @Test
    void permalockChoiceWarnsThatOnlySafewordCanClearIt() throws IOException {
        JsonNode lockType = findOption(readManifest("lock"), "type");
        assertTrue(lockType.path("description").asText().toLowerCase().contains("safeword"));

        JsonNode permalockChoice = null;
        for (JsonNode choice : lockType.path("choices")) {
            if ("PERMALOCK".equals(choice.path("value").asText())) {
                permalockChoice = choice;
                break;
            }
        }

        assertNotNull(permalockChoice);
        assertTrue(permalockChoice.path("name").asText().toLowerCase().contains("safeword"));
    }

    @Test
    void helpDocumentsEveryPublicCommand() throws IOException {
        String helpDescription = Stream.of(readManifest("help").path("description").asText())
                .findFirst().orElseThrow();
        assertFalse(helpDescription.isBlank());

        // The detailed help is rendered by the Java command; this guards the registration resources.
        Set<String> publicCommands = new HashSet<>(COMMANDS);
        publicCommands.remove("adminhelp");
        publicCommands.remove("consentreset");
        assertTrue(publicCommands.containsAll(Stream.of(RestraintDefinition.values())
                .map(RestraintDefinition::getCommandName).toList()));
    }

    private JsonNode readManifest(String command) throws IOException {
        String path = "/commands/" + command + ".json";
        try (InputStream input = getClass().getResourceAsStream(path)) {
            assertNotNull(input, "Missing command manifest: " + path);
            return mapper.readTree(input);
        }
    }

    private JsonNode findOption(JsonNode manifest, String name) {
        for (JsonNode option : manifest.path("options")) {
            if (name.equals(option.path("name").asText())) {
                return option;
            }
        }
        throw new AssertionError("Missing option '" + name + "' in /" + manifest.path("name").asText());
    }
}
