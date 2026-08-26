package com.evolvingfurnaces.tier;

import com.evolvingfurnaces.machine.EFEnergyModule;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public record EFTierList(List<EFMachineTier> levels, EFUpgradeCost generatorCost, EFUpgradeCost electricCost) {

    public static final Codec<EFTierList> CODEC = RecordCodecBuilder.<EFTierList>create(instance -> instance.group(
            EFMachineTier.CODEC.listOf().fieldOf("levels").forGetter(EFTierList::levels),
            EFUpgradeCost.CODEC.optionalFieldOf("generator_cost", EFUpgradeCost.EMPTY).forGetter(EFTierList::generatorCost),
            EFUpgradeCost.CODEC.optionalFieldOf("electric_cost", EFUpgradeCost.EMPTY).forGetter(EFTierList::electricCost)
    ).apply(instance, EFTierList::new)).flatXmap(EFTierList::validate, EFTierList::validate);

    public EFTierList(List<EFMachineTier> levels) {
        this(levels, EFUpgradeCost.EMPTY, EFUpgradeCost.EMPTY);
    }

    private static DataResult<EFTierList> validate(EFTierList list) {
        if (list.levels.isEmpty()) {
            return DataResult.error(() -> "levels must not be empty");
        }
        if (list.generatorCost.experienceLevels() < 0) {
            return DataResult.error(() -> "generator_cost.experience_levels must be >= 0");
        }
        if (list.electricCost.experienceLevels() < 0) {
            return DataResult.error(() -> "electric_cost.experience_levels must be >= 0");
        }
        Set<Integer> seen = new HashSet<>();
        for (EFMachineTier tier : list.levels) {
            if (!seen.add(tier.level())) {
                return DataResult.error(() -> "duplicate level " + tier.level());
            }
        }
        List<EFMachineTier> sorted = list.levels.stream()
                .sorted(Comparator.comparingInt(EFMachineTier::level))
                .toList();
        return DataResult.success(new EFTierList(sorted, list.generatorCost, list.electricCost));
    }

    public Optional<EFMachineTier> byLevel(int level) {
        return levels.stream().filter(t -> t.level() == level).findFirst();
    }

    public Optional<EFMachineTier> first() {
        return levels.isEmpty() ? Optional.empty() : Optional.of(levels.getFirst());
    }

    public Optional<EFMachineTier> nextAfter(int currentLevel) {
        if (currentLevel <= 0) {
            return first();
        }
        for (int i = 0; i < levels.size(); i++) {
            if (levels.get(i).level() == currentLevel) {
                if (i + 1 < levels.size()) {
                    return Optional.of(levels.get(i + 1));
                }
                return Optional.empty();
            }
        }
        return levels.stream().filter(t -> t.level() > currentLevel).findFirst();
    }

    public boolean isMax(int currentLevel) {
        return nextAfter(currentLevel).isEmpty();
    }

    public EFUpgradeCost moduleCost(EFEnergyModule module) {
        return switch (module) {
            case GENERATOR -> generatorCost;
            case ELECTRIC -> electricCost;
            case NONE -> EFUpgradeCost.EMPTY;
        };
    }
}
