package com.evolvingfurnaces.tier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public record EFTierList(List<EFMachineTier> levels) {

    public static final Codec<EFTierList> CODEC = RecordCodecBuilder.<EFTierList>create(instance -> instance.group(
            EFMachineTier.CODEC.listOf().fieldOf("levels").forGetter(EFTierList::levels)
    ).apply(instance, EFTierList::new)).flatXmap(EFTierList::validate, EFTierList::validate);

    private static DataResult<EFTierList> validate(EFTierList list) {
        if (list.levels.isEmpty()) {
            return DataResult.error(() -> "levels must not be empty");
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
        return DataResult.success(new EFTierList(sorted));
    }

    public Optional<EFMachineTier> byLevel(int level) {
        return levels.stream().filter(t -> t.level() == level).findFirst();
    }

    public Optional<EFMachineTier> first() {
        return levels.isEmpty() ? Optional.empty() : Optional.of(levels.get(0));
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
}
