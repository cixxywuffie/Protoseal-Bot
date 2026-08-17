package com.cixtrowolf.protoseal.model.restraint;


import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum RestraintDefinition {
    LEG_CUFFS("legcuffs", RestraintZone.LEGS,
            level("%s removed %s's restrains, making their legs free to move", "%s removes their leg restraints, making their legs free to move"),
            level("%s fits to %s's legs a pair of cuffs, can't run so far from master now", "%s fits a pair of cuffs to their own legs"),
            level("%s spreads %s's legs, fitting a pair of cuffs and attach them a long bar and making really difficult to walk", "%s fits a spreader bar to their own cuffed legs"),
            level("%s ties to %s's ankles with a rope, fitting them tight to make it impossible to move", "%s ties their own ankles tightly with rope")),
    ARM_CUFFS("armcuffs", RestraintZone.ARMS,
            level("%s removed %s's restrains, making their arms free to move", "%s removes their arm restraints, making their arms free to move"),
            level("%s puts %s's arms behind of their back and slide a leather sack, covering their arms and fitting them, making impossible to remove", "%s secures their own arms behind their back in an armbinder"),
            level("%s fits on %s's wrists a pair of police cuffs, fitting them and making difficult to move those grabby paws", "%s fits a pair of cuffs to their own wrists"),
            level("%s grabs a rope and ties %s's arms, fitting them tight enough making impossible to escape", "%s ties their own arms tightly with rope")),
    GAG("gag", RestraintZone.GAG,
            level("%s removes %s's gag, letting them speak freely again", "%s removes their own gag, letting them speak freely again"),
            level("%s fits a ball gag securely on %s", "%s fits a ball gag on themselves"),
            level("%s buckles a bit gag in place on %s", "%s buckles a bit gag in place on themselves"),
            level("%s applies tape over %s's mouth, leaving them unable to speak", "%s applies tape over their own mouth, leaving themselves unable to speak")),
    HOOD("hood", RestraintZone.HOOD,
            level("%s removes %s's hood, letting them see clearly again", "%s removes their own hood, letting themselves see clearly again"),
            level("%s fits a leather hood over %s's head", "%s fits a leather hood over their own head"),
            level("%s secures a blindfold hood on %s, blocking their vision", "%s secures a blindfold hood on themselves, blocking their vision"),
            level("%s fastens a full hood securely over %s's head", "%s fastens a full hood securely over their own head")),
    STRAITJACKET("straitjacket", RestraintZone.STRAITJACKET,
            level("%s removes %s's straitjacket, freeing their arms", "%s removes their own straitjacket, freeing their arms"),
            level("%s fastens a canvas straitjacket around %s", "%s fastens a canvas straitjacket around themselves"),
            level("%s secures %s in a padlocked straitjacket", "%s secures themselves in a padlocked straitjacket"),
            level("%s tightly fastens a full straitjacket around %s", "%s tightly fastens a full straitjacket around themselves")),
    SUITS("suits", RestraintZone.SUIT,
            level("%s removes %s's restraint suit", "%s removes their own restraint suit"),
            level("%s helps %s into a latex restraint suit", "%s puts on a latex restraint suit"),
            level("%s seals %s into an enclosed restraint suit", "%s seals themselves into an enclosed restraint suit"),
            level("%s secures %s in a full-body restraint suit", "%s secures themselves in a full-body restraint suit")),
    MITTS("mitts", RestraintZone.MITTS,
            level("%s removes %s's mitts, freeing their paws", "%s removes their own mitts, freeing their paws"),
            level("%s fits a pair of soft mittens over %s's paws", "%s fits a pair of soft mittens over their own paws"),
            level("%s secures a pair of puppy paws over %s's hands", "%s secures a pair of puppy paws over their own hands"),
            level("%s fastens a pair of horse hooves over %s's hands", "%s fastens a pair of horse hooves over their own hands")),
    CHASTITY("chastity", RestraintZone.CHASTITY,
            level("%s removes %s's chastity restraint", "%s removes their own chastity restraint"),
            level("%s secures a chastity cage on %s", "%s secures a chastity cage on themselves"),
            level("%s fastens a chastity belt around %s", "%s fastens a chastity belt around themselves"),
            level("%s seals %s into a smooth null-bulge chastity restraint", "%s seals themselves into a smooth null-bulge chastity restraint")),
    BLINDFOLD("blindfold", RestraintZone.BLINDFOLD,
            level("%s removes %s's blindfold, restoring their vision", "%s removes their own blindfold, restoring their vision"),
            level("%s secures a leather blindfold over %s's eyes", "%s secures a leather blindfold over their own eyes"),
            level("%s wraps a soft bandage firmly over %s's eyes", "%s wraps a soft bandage firmly over their own eyes"),
            level("%s fits blind protection goggles over %s's eyes", "%s fits blind protection goggles over their own eyes"));

    private static final Map<String, RestraintDefinition> BY_COMMAND = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(RestraintDefinition::getCommandName, Function.identity()));

    private final String commandName;
    private final RestraintZone zone;
    private final RestraintLevel[] levels;

    RestraintDefinition(String commandName, RestraintZone zone, RestraintLevel... levels) {
        this.commandName = commandName;
        this.zone = zone;
        this.levels = levels.clone();
    }

    public String getCommandName() {
        return commandName;
    }

    public RestraintZone getZone() {
        return zone;
    }

    public int getMaximumLevel() {
        return levels.length - 1;
    }

    public RestraintLevel getLevel(int level) {
        return levels[level];
    }

    public static Optional<RestraintDefinition> fromCommandName(String commandName) {
        return Optional.ofNullable(BY_COMMAND.get(commandName));
    }

    private static RestraintLevel level(String message, String selfMessage) {
        return new RestraintLevel(message, selfMessage);
    }
}
