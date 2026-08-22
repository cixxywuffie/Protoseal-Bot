package com.cixtrowolf.protoseal.model.restraint;


import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum RestraintDefinition {
    LEG_CUFFS("legcuffs", RestraintZone.LEGS,
            level("none","%s removes %s's restraints, freeing their legs", "%s removes their leg restraints, freeing their legs"),
            level("loose cuffs","%s loosely cuffs %s's ankles, allowing short steps", "%s loosely cuffs their own ankles"),
            level("taut cuffs","%s tightly cuffs %s's ankles together", "%s tightly cuffs their own ankles together"),
            level("standing hobble","%s secures %s's legs in a standing hobble", "%s secures their own legs in a standing hobble"),
            level("kneeling tie","%s folds and binds %s's legs beneath them", "%s folds and binds their own legs beneath them"),
            level("frog tie","%s binds %s's legs in a restrictive frog tie", "%s binds their own legs in a frog tie"),
            level("seated tie","%s binds %s's legs in a seated position", "%s binds their own legs in a seated position"),
            level("spreader bar","%s cuffs %s's legs to a spreader bar", "%s secures their cuffed legs to a spreader bar"),
            level("hogtie","%s pulls %s's limbs together in a hogtie", "%s secures themselves in a hogtie"),
            level("side hogtie","%s binds %s in a sideways hogtie", "%s binds themselves in a sideways hogtie"),
            level("strict hogtie","%s draws %s into a strict hogtie", "%s draws themselves into a strict hogtie")),
    ARM_CUFFS("armcuffs", RestraintZone.ARMS,
            level("free","%s removes %s's restraints, freeing their arms", "%s removes their arm restraints, freeing their arms"),
            level("armbinder","%s places %s's arms behind their back and secures them inside a leather armbinder", "%s secures their own arms behind their back in an armbinder"),
            level("behind back","%s cuffs %s's wrists behind their back", "%s cuffs their own wrists behind their back"),
            level("behind tight","%s draws %s's wrists tightly together behind their back", "%s draws their own wrists tightly together behind their back"),
            level("behind belt","%s secures %s's wrists behind them to a waist belt", "%s secures their own wrists behind them to a waist belt"),
            level("behind T-belt","%s locks %s's wrists behind them to a T-shaped belt", "%s locks their own wrists behind them to a T-shaped belt"),
            level("elbows","%s binds %s's arms together at the elbows", "%s binds their own arms together at the elbows"),
            level("front cuffs","%s cuffs %s's wrists together in front", "%s cuffs their own wrists together in front"),
            level("front belt","%s secures %s's wrists to a belt in front", "%s secures their own wrists to a belt in front"),
            level("reverse prayer","%s binds %s's arms in a reverse-prayer position", "%s binds their own arms in a reverse-prayer position"),
            level("at sides","%s secures %s's wrists at their sides", "%s secures their own wrists at their sides"),
            level("sides tight","%s pins %s's wrists tightly against their sides", "%s pins their own wrists tightly against their sides"),
            level("wrapped lights","%s wraps %s's arms in glowing restraint lights", "%s wraps their own arms in glowing restraint lights")),
    GAG("gag", RestraintZone.GAG,
            level("none","%s removes %s's gag, letting them speak freely again", "%s removes their own gag, letting them speak freely again"),
            level("ballgag","%s fits a ball gag securely on %s", "%s fits a ball gag on themselves"),
            level("bitgag","%s buckles a bit gag in place on %s", "%s buckles a bit gag in place on themselves"),
            level("tape","%s applies tape over %s's mouth, leaving them unable to speak", "%s applies tape over their own mouth, leaving themselves unable to speak"),
            level("wire muzzle","%s straps a wire-frame muzzle over %s's snout", "%s straps a wire-frame muzzle over their own snout"),
            level("leather muzzle","%s buckles a leather muzzle securely over %s's mouth", "%s buckles a leather muzzle over their own mouth"),
            level("ring gag","%s fastens a ring gag in %s's mouth", "%s fastens a ring gag in their own mouth"),
            level("sock gag","%s packs a soft sock gag into %s's mouth", "%s packs a soft sock gag into their own mouth"),
            level("pacifier gag","%s secures a pacifier gag in %s's mouth", "%s secures a pacifier gag in their own mouth"),
            level("panel gag","%s buckles a padded panel gag over %s's mouth", "%s buckles a padded panel gag over their own mouth"),
            level("inflatable gag","%s fits an inflatable gag securely in %s's mouth", "%s fits an inflatable gag in their own mouth"),
            level("reindeer muzzle","%s fits a festive reindeer muzzle over %s's snout", "%s fits a festive reindeer muzzle over their own snout")),
    HOOD("hood", RestraintZone.HOOD,
            level("none","%s removes %s's hood, letting them see clearly again", "%s removes their own hood, letting themselves see clearly again"),
            level("gimp hood","%s fits a leather gimp hood over %s's head", "%s fits a leather gimp hood over their own head"),
            level("gas mask","%s secures a gas mask hood on %s, blocking their vision and making it harder to breathe through the filters", "%s secures a gas mask on themselves, blocking their vision and making it harder to breathe through the filters"),
            level("sensory deprivation","%s fastens a full hood securely over %s's head. They cannot hear, see, or speak", "%s fastens a full hood securely over their own head. They cannot hear, see, or speak"),
            level("open-face hood","%s laces an open-face hood around %s's head", "%s laces an open-face hood around their own head"),
            level("bondage hood","%s buckles a close-fitting bondage hood over %s's head", "%s buckles a bondage hood over their own head"),
            level("puppy hood","%s fits a puppy hood over %s's head", "%s fits a puppy hood over their own head"),
            level("kitty hood","%s fits a kitty hood over %s's head", "%s fits a kitty hood over their own head"),
            level("drone hood","%s seals a featureless drone hood over %s's head", "%s seals a drone hood over their own head"),
            level("pony hood","%s fits an equine pony hood over %s's head", "%s fits a pony hood over their own head")),
    STRAITJACKET("straitjacket", RestraintZone.STRAITJACKET,
            level("none","%s removes %s's straitjacket, freeing their arms", "%s removes their own straitjacket, freeing their arms"),
            level("shoulder strap","%s fits a straitjacket around %s and fastens it tightly across their shoulders", "%s puts on a straitjacket and fastens it tightly across their shoulders"),
            level("front strap","%s secures %s's arms and fastens the strap across the front of the jacket", "%s secures their own arms and fastens the strap across the front of the jacket"),
            level("back strap","%s tightly fastens a straitjacket around %s and secures the straps across their back", "%s tightly fastens a straitjacket around themselves and secures the straps across their back"),
            level("front sleeves","%s crosses and secures %s's straitjacket sleeves in front", "%s crosses and secures their straitjacket sleeves in front"),
            level("reverse sleeves","%s pulls %s's straitjacket sleeves behind their back", "%s pulls their straitjacket sleeves behind their back"),
            level("single crotch strap","%s tightens a single crotch strap on %s's straitjacket", "%s tightens their straitjacket's single crotch strap"),
            level("double crotch strap","%s secures two crotch straps on %s's straitjacket", "%s secures both crotch straps on their straitjacket"),
            level("triple crotch strap","%s locks three crotch straps on %s's straitjacket", "%s locks all three crotch straps on their straitjacket")),
    SUITS("suits", RestraintZone.SUIT,
            level("none","%s removes %s's restraint suit", "%s removes their own restraint suit"),
            level("latex","%s helps %s into a latex restraint suit", "%s puts on a latex restraint suit"),
            level("plush","%s seals %s into a plush suit", "%s seals themselves into a plush suit"),
            level("gimp","%s secures %s in a full-body restraint suit", "%s secures themselves in a full-body restraint suit"),
            level("puppy","%s seals %s into a puppy suit", "%s seals themselves into a puppy suit"),
            level("kitty","%s seals %s into a kitty suit", "%s seals themselves into a kitty suit"),
            level("pony","%s fits %s into a pony suit", "%s fits themselves into a pony suit"),
            level("drone","%s converts %s's silhouette with a drone suit", "%s puts on a featureless drone suit"),
            level("cow","%s fits %s into a cow-themed suit", "%s fits themselves into a cow-themed suit"),
            level("catsuit","%s zips %s into a close-fitting catsuit", "%s zips themselves into a close-fitting catsuit"),
            level("reindeer","%s dresses %s in a reindeer suit", "%s dresses themselves in a reindeer suit"),
            level("toy suit","%s seals %s into a toy-like restraint suit", "%s seals themselves into a toy-like restraint suit"),
            level("bitchsuit","%s secures %s in a restrictive bitchsuit", "%s secures themselves in a restrictive bitchsuit"),
            level("hogsack","%s closes a compact hogsack around %s", "%s closes a compact hogsack around themselves"),
            level("sleepsack","%s slides %s into a snug sleepsack", "%s slides themselves into a snug sleepsack"),
            level("sleeper suit","%s fastens %s into a padded sleeper suit", "%s fastens themselves into a padded sleeper suit")),
    MITTS("mitts", RestraintZone.MITTS,
            level("none","%s removes %s's mitts, freeing their paws", "%s removes their own mitts, freeing their paws"),
            level("default","%s fits a pair of soft mittens over %s's paws", "%s fits a pair of soft mittens over their own paws"),
            level("puppy","%s secures a pair of puppy paws over %s's hands", "%s secures a pair of puppy paws over their own hands"),
            level("hooves","%s fastens a pair of horse hooves over %s's hands", "%s fastens a pair of horse hooves over their own hands"),
            level("cow hooves","%s secures a pair of cow hooves over %s's hands", "%s secures cow hooves over their own hands"),
            level("ducky mitts","%s fits squeaky ducky mitts over %s's hands", "%s fits squeaky ducky mitts over their own hands")),
    CHASTITY("chastity", RestraintZone.CHASTITY,
            level("none","%s removes %s's chastity restraint", "%s removes their own chastity restraint"),
            level("cage","%s secures a chastity cage on %s", "%s secures a chastity cage on themselves"),
            level("belt","%s fastens a chastity belt around %s", "%s fastens a chastity belt around themselves"),
            level("null bulge","%s seals %s into a smooth null-bulge chastity restraint", "%s seals themselves into a smooth null-bulge chastity restraint"),
            level("udder","%s secures an udder-style chastity cover on %s", "%s secures an udder-style chastity cover on themselves")),
    BLINDFOLD("blindfold", RestraintZone.BLINDFOLD,
            level("none","%s removes %s's blindfold, restoring their vision", "%s removes their own blindfold, restoring their vision"),
            level("default","%s secures a leather blindfold over %s's eyes", "%s secures a leather blindfold over their own eyes"),
            level("bandage","%s wraps a soft bandage firmly over %s's eyes", "%s wraps a soft bandage firmly over their own eyes"),
            level("blind goggles","%s fits blind protection goggles over %s's eyes", "%s fits blind protection goggles over their own eyes"),
            level("paneled blindfold","%s buckles a paneled blindfold over %s's eyes", "%s buckles a paneled blindfold over their own eyes"),
            level("opaque contacts","%s places opaque contacts over %s's eyes", "%s places opaque contacts over their own eyes")),
    COLLAR("collar", RestraintZone.COLLAR,
            level("none", "%s removes %s's collar", "%s removes their own collar"),
            level("leather", "%s fastens a leather collar around %s's neck", "%s fastens a leather collar around their own neck"),
            level("latex", "%s fits a latex collar around %s's neck", "%s fits a latex collar around their own neck"),
            level("rubber", "%s secures a rubber collar around %s's neck", "%s secures a rubber collar around their own neck"),
            level("chain", "%s closes a chain collar around %s's neck", "%s closes a chain collar around their own neck"),
            level("iron", "%s locks an iron collar around %s's neck", "%s locks an iron collar around their own neck")),
    CONFINE("confine", RestraintZone.CONFINE,
            level("none", "%s releases %s from confinement", "%s leaves their confinement"),
            level("cell", "%s confines %s in a secure cell", "%s confines themselves in a secure cell"),
            level("padded room", "%s confines %s in a padded room", "%s confines themselves in a padded room"),
            level("sack", "%s confines %s inside a secured restraint sack", "%s confines themselves inside a restraint sack"),
            level("circle", "%s confines %s inside a marked circle", "%s confines themselves inside a marked circle"),
            level("pit", "%s confines %s in a deep pit", "%s confines themselves in a deep pit")),
    ENCASE("encase", RestraintZone.ENCASE,
            level("none", "%s releases %s from their encasement", "%s frees themselves from their encasement"),
            level("mummy", "%s wraps %s from head to toe like a mummy", "%s wraps themselves from head to toe like a mummy"),
            level("gibbet", "%s secures %s inside a rigid gibbet", "%s secures themselves inside a rigid gibbet"),
            level("rubber", "%s encases %s tightly in rubber", "%s encases themselves tightly in rubber"),
            level("glass", "%s seals %s inside a glass enclosure", "%s seals themselves inside a glass enclosure"),
            level("cage", "%s locks %s inside a body-sized cage", "%s locks themselves inside a body-sized cage"),
            level("pole", "%s binds %s securely to a pole", "%s binds themselves securely to a pole"),
            level("vacbed", "%s seals %s inside a restrictive vacuum bed", "%s seals themselves inside a restrictive vacuum bed"),
            level("cement", "%s encases %s in hardened cement", "%s encases themselves in hardened cement"),
            level("glue", "%s immobilizes %s inside a layer of strong glue", "%s immobilizes themselves inside a layer of strong glue"));

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
