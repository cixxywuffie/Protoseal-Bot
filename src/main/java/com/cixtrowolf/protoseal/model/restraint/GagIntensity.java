package com.cixtrowolf.protoseal.model.restraint;

public enum GagIntensity {
    DEFAULT, NONE, LIGHT, MEDIUM, STRONG, EXTREME;

    public GagIntensity resolve(int gagLevel) {
        if (this != DEFAULT) return this;
        return switch (gagLevel) {
            case 2, 6 -> LIGHT;
            case 1, 4, 8 -> MEDIUM;
            case 7, 9, 10 -> STRONG;
            case 3, 5 -> EXTREME;
            default -> throw new IllegalArgumentException("No default intensity for gag level " + gagLevel);
        };
    }
}
