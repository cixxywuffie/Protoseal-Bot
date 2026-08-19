package com.cixtrowolf.protoseal.model.restraint;


import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum RestraintDefinition {
    LEG_CUFFS("legcuffs", RestraintZone.LEGS,
            level("none","%s removes %s's restraints, freeing their legs", "%s removes their leg restraints, freeing their legs"),
            level("cuffs","%s fastens a pair of cuffs around %s's legs, restricting their movement", "%s fastens a pair of cuffs around their own legs"),
            level("spreader bar","%s cuffs %s's legs and secures them to a spreader bar, making it difficult to walk", "%s secures their cuffed legs to a spreader bar"),
            level("hobbles","%s tightly binds %s's ankles with rope, preventing them from moving", "%s tightly binds their own ankles with rope")),
    ARM_CUFFS("armcuffs", RestraintZone.ARMS,
            level("free","%s removes %s's restraints, freeing their arms", "%s removes their arm restraints, freeing their arms"),
            level("armbinder","%s places %s's arms behind their back and secures them inside a leather armbinder", "%s secures their own arms behind their back in an armbinder"),
            level("cuffs","%s fastens a pair of police cuffs around %s's wrists, restricting their movement", "%s fastens a pair of cuffs around their own wrists"),
            level("rope","%s tightly binds %s's arms with rope, making escape impossible", "%s tightly binds their own arms with rope")),
    GAG("gag", RestraintZone.GAG,
            level("none","%s removes %s's gag, letting them speak freely again", "%s removes their own gag, letting them speak freely again"),
            level("ballgag","%s fits a ball gag securely on %s", "%s fits a ball gag on themselves"),
            level("bitgag","%s buckles a bit gag in place on %s", "%s buckles a bit gag in place on themselves"),
            level("tape","%s applies tape over %s's mouth, leaving them unable to speak", "%s applies tape over their own mouth, leaving themselves unable to speak")),
    HOOD("hood", RestraintZone.HOOD,
            level("none","%s removes %s's hood, letting them see clearly again", "%s removes their own hood, letting themselves see clearly again"),
            level("gimp hood","%s fits a leather gimp hood over %s's head", "%s fits a leather gimp hood over their own head"),
            level("gas mask","%s secures a gas mask hood on %s, blocking their vision and making it harder to breathe through the filters", "%s secures a gas mask on themselves, blocking their vision and making it harder to breathe through the filters"),
            level("sensory deprivation","%s fastens a full hood securely over %s's head. They cannot hear, see, or speak", "%s fastens a full hood securely over their own head. They cannot hear, see, or speak")),
    STRAITJACKET("straitjacket", RestraintZone.STRAITJACKET,
            level("none","%s removes %s's straitjacket, freeing their arms", "%s removes their own straitjacket, freeing their arms"),
            level("shoulder strap","%s fits a straitjacket around %s and fastens it tightly across their shoulders", "%s puts on a straitjacket and fastens it tightly across their shoulders"),
            level("front strap","%s secures %s's arms and fastens the strap across the front of the jacket", "%s secures their own arms and fastens the strap across the front of the jacket"),
            level("back strap","%s tightly fastens a straitjacket around %s and secures the straps across their back", "%s tightly fastens a straitjacket around themselves and secures the straps across their back")),
    SUITS("suits", RestraintZone.SUIT,
            level("none","%s removes %s's restraint suit", "%s removes their own restraint suit"),
            level("latex","%s helps %s into a latex restraint suit", "%s puts on a latex restraint suit"),
            level("plush","%s seals %s into a plush suit", "%s seals themselves into a plush suit"),
            level("gimp","%s secures %s in a full-body restraint suit", "%s secures themselves in a full-body restraint suit")),
    MITTS("mitts", RestraintZone.MITTS,
            level("none","%s removes %s's mitts, freeing their paws", "%s removes their own mitts, freeing their paws"),
            level("default","%s fits a pair of soft mittens over %s's paws", "%s fits a pair of soft mittens over their own paws"),
            level("puppy","%s secures a pair of puppy paws over %s's hands", "%s secures a pair of puppy paws over their own hands"),
            level("hooves","%s fastens a pair of horse hooves over %s's hands", "%s fastens a pair of horse hooves over their own hands")),
    CHASTITY("chastity", RestraintZone.CHASTITY,
            level("none","%s removes %s's chastity restraint", "%s removes their own chastity restraint"),
            level("cage","%s secures a chastity cage on %s", "%s secures a chastity cage on themselves"),
            level("belt","%s fastens a chastity belt around %s", "%s fastens a chastity belt around themselves"),
            level("null bulge","%s seals %s into a smooth null-bulge chastity restraint", "%s seals themselves into a smooth null-bulge chastity restraint")),
    BLINDFOLD("blindfold", RestraintZone.BLINDFOLD,
            level("none","%s removes %s's blindfold, restoring their vision", "%s removes their own blindfold, restoring their vision"),
            level("default","%s secures a leather blindfold over %s's eyes", "%s secures a leather blindfold over their own eyes"),
            level("bandage","%s wraps a soft bandage firmly over %s's eyes", "%s wraps a soft bandage firmly over their own eyes"),
            level("blind goggles","%s fits blind protection goggles over %s's eyes", "%s fits blind protection goggles over their own eyes"));

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

    private static RestraintLevel level(String name,String message, String selfMessage) {
        return new RestraintLevel(name, message, selfMessage);
    }
}
