package com.evolvingfurnaces.tier;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class EFDefaultTiers {

    private EFDefaultTiers() {
    }

    private static final String[] NAMES = {
            "copper", "amethyst", "iron", "lapis", "redstone", "gold", "diamond", "emerald", "obsidian", "netherite"
    };
    private static final String[] EN_ADJ = {
            "Copper", "Amethyst", "Iron", "Lapis Lazuli", "Redstone", "Gold", "Diamond", "Emerald", "Obsidian", "Netherite"
    };
    private static final String[] FR_ADJ = {
            "cuivre", "améthyste", "fer", "lapis-lazuli", "redstone", "or", "diamant", "émeraude", "obsidienne", "netherite"
    };
    private static final String[] BLOCKS = {
            "minecraft:copper_block",
            "minecraft:amethyst_block",
            "minecraft:iron_block",
            "minecraft:lapis_block",
            "minecraft:redstone_block",
            "minecraft:gold_block",
            "minecraft:diamond_block",
            "minecraft:emerald_block",
            "minecraft:obsidian",
            "minecraft:netherite_block"
    };
    private static final int[] COOK_FURNACE = {200, 190, 180, 170, 160, 150, 140, 130, 120, 110};
    private static final int[] BATCH = {1, 1, 1, 2, 2, 2, 4, 4, 4, 8};
    private static final int[] REQUIRED_COOKS = {0, 256, 1024, 4096, 16384, 65536, 262144, 1048576, 4194304, 16777216};
    private static final int[] XP = {2, 5, 8, 10, 12, 15, 18, 20, 25, 30};

    public static Map<EFMachineKind, EFTierList> all() {
        EnumMap<EFMachineKind, EFTierList> map = new EnumMap<>(EFMachineKind.class);
        for (EFMachineKind kind : EFMachineKind.values()) {
            map.put(kind, forKind(kind));
        }
        return map;
    }

    public static EFTierList forKind(EFMachineKind kind) {
        EFMachineTier[] tiers = new EFMachineTier[NAMES.length];
        for (int i = 0; i < NAMES.length; i++) {
            int cookTime = switch (kind) {
                case FURNACE -> COOK_FURNACE[i];
                case BLAST_FURNACE, SMOKER -> COOK_FURNACE[i] / 2;
            };
            String enName;
            String frName;
            switch (kind) {
                case FURNACE -> {
                    enName = EN_ADJ[i] + " Furnace";
                    frName = "Fourneau en " + FR_ADJ[i];
                }
                case BLAST_FURNACE -> {
                    enName = EN_ADJ[i] + " Blast Furnace";
                    frName = "Haut-fourneau en " + FR_ADJ[i];
                }
                case SMOKER -> {
                    enName = EN_ADJ[i] + " Smoker";
                    frName = "Fumoir en " + FR_ADJ[i];
                }
                default -> {
                    enName = EN_ADJ[i];
                    frName = FR_ADJ[i];
                }
            }
            Map<String, String> names = new LinkedHashMap<>();
            names.put("en_us", enName);
            names.put("fr_fr", frName);
            tiers[i] = new EFMachineTier(
                    i + 1,
                    Optional.of(NAMES[i]),
                    new EFDisplayNames(names),
                    new EFStyleRef(ResourceLocation.parse(BLOCKS[i]), EFTextureFace.SIDE),
                    REQUIRED_COOKS[i],
                    new EFUpgradeCost(List.of(EFCostIngredient.item(BLOCKS[i], 8)), XP[i]),
                    cookTime,
                    BATCH[i]
            );
        }
        return new EFTierList(List.of(tiers));
    }
}
