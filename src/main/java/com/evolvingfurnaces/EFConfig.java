package com.evolvingfurnaces;

import com.evolvingfurnaces.machine.EFEnergyModule;
import com.evolvingfurnaces.tier.EFMachineKind;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class EFConfig {

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue FURNACE_ENABLED;
    public static final ModConfigSpec.BooleanValue BLAST_FURNACE_ENABLED;
    public static final ModConfigSpec.BooleanValue SMOKER_ENABLED;
    public static final ModConfigSpec.BooleanValue BREWING_STAND_ENABLED;
    public static final ModConfigSpec.BooleanValue GENERATOR_ENABLED;
    public static final ModConfigSpec.BooleanValue ELECTRIC_ENABLED;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("furnace");
        FURNACE_ENABLED = builder
                .comment(
                        "If false, evolving furnaces cannot be created and are hidden from creative and recipe viewers.",
                        "Already placed evolving furnaces convert back to a vanilla furnace on load.",
                        "Contents are kept; tier, cooks and IO are lost. Vanilla furnaces are never modified.")
                .define("enabled", true);
        builder.pop();

        builder.push("blast_furnace");
        BLAST_FURNACE_ENABLED = builder
                .comment(
                        "If false, evolving blast furnaces cannot be created and are hidden from creative and recipe viewers.",
                        "Already placed evolving blast furnaces convert back to a vanilla blast furnace on load.",
                        "Contents are kept; tier, cooks and IO are lost. Vanilla blast furnaces are never modified.")
                .define("enabled", true);
        builder.pop();

        builder.push("smoker");
        SMOKER_ENABLED = builder
                .comment(
                        "If false, evolving smokers cannot be created and are hidden from creative and recipe viewers.",
                        "Already placed evolving smokers convert back to a vanilla smoker on load.",
                        "Contents are kept; tier, cooks and IO are lost. Vanilla smokers are never modified.")
                .define("enabled", true);
        builder.pop();

        builder.push("brewing_stand");
        BREWING_STAND_ENABLED = builder
                .comment(
                        "If false, evolving brewing stands cannot be created and are hidden from creative and recipe viewers.",
                        "Already placed evolving brewing stands convert back to a vanilla brewing stand on load.",
                        "Contents are kept; tier, brews and IO are lost. Vanilla brewing stands are never modified.")
                .define("enabled", true);
        builder.pop();

        builder.push("generator");
        GENERATOR_ENABLED = builder
                .comment(
                        "If false, the generator module cannot be installed and its GUI button is hidden.",
                        "Already installed generator modules are removed on tick; the furnace keeps its tier, cooks, contents and goes back to cooking.",
                        "Cables no longer extract FE from that machine.")
                .define("enabled", true);
        builder.pop();

        builder.push("electric");
        ELECTRIC_ENABLED = builder
                .comment(
                        "If false, the electric module cannot be installed and its GUI button is hidden.",
                        "Already installed electric modules are removed on tick; the furnace keeps its tier, cooks, contents and goes back to burning fuel.",
                        "Cables no longer insert FE into that machine.")
                .define("enabled", true);
        builder.pop();

        SPEC = builder.build();
    }

    private EFConfig() {
    }

    public static boolean enabled(EFMachineKind kind) {
        return switch (kind) {
            case FURNACE -> FURNACE_ENABLED.get();
            case BLAST_FURNACE -> BLAST_FURNACE_ENABLED.get();
            case SMOKER -> SMOKER_ENABLED.get();
            case BREWING_STAND -> BREWING_STAND_ENABLED.get();
        };
    }

    public static boolean generatorEnabled() {
        return GENERATOR_ENABLED.get();
    }

    public static boolean electricEnabled() {
        return ELECTRIC_ENABLED.get();
    }

    public static boolean anyEnergyModuleEnabled() {
        return generatorEnabled() || electricEnabled();
    }

    public static boolean moduleEnabled(EFEnergyModule module) {
        return switch (module) {
            case NONE -> true;
            case GENERATOR -> generatorEnabled();
            case ELECTRIC -> electricEnabled();
        };
    }

    public static boolean moduleAvailable(EFMachineKind kind, EFEnergyModule module) {
        return moduleEnabled(module) && module.supportedOn(kind);
    }
}
