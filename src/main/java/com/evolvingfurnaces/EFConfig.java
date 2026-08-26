package com.evolvingfurnaces;

import com.evolvingfurnaces.tier.EFMachineKind;
import net.minecraftforge.common.ForgeConfigSpec;

public final class EFConfig {

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue FURNACE_ENABLED;
    public static final ForgeConfigSpec.BooleanValue BLAST_FURNACE_ENABLED;
    public static final ForgeConfigSpec.BooleanValue SMOKER_ENABLED;
    public static final ForgeConfigSpec.BooleanValue BREWING_STAND_ENABLED;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

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
}
