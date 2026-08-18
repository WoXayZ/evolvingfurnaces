package com.evolvingfurnaces.tier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

public record EFUpgradeCost(List<EFCostIngredient> items, int experienceLevels) {

    public static final Codec<EFUpgradeCost> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EFCostIngredient.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(EFUpgradeCost::items),
            Codec.INT.optionalFieldOf("experience_levels", 0).forGetter(EFUpgradeCost::experienceLevels)
    ).apply(instance, EFUpgradeCost::new));
}
