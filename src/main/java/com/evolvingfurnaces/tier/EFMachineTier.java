package com.evolvingfurnaces.tier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;

public record EFMachineTier(
        int level,
        Optional<String> name,
        EFDisplayNames displayName,
        EFStyleRef style,
        int requiredCooks,
        EFUpgradeCost upgradeCost,
        int cookTime,
        int batchSize
) {

    public static final Codec<EFMachineTier> CODEC = RecordCodecBuilder.<EFMachineTier>create(instance -> instance.group(
            Codec.INT.fieldOf("level").forGetter(EFMachineTier::level),
            Codec.STRING.optionalFieldOf("name").forGetter(EFMachineTier::name),
            EFDisplayNames.CODEC.fieldOf("display_name").forGetter(EFMachineTier::displayName),
            EFStyleRef.CODEC.fieldOf("style").forGetter(EFMachineTier::style),
            Codec.INT.fieldOf("required_cooks").forGetter(EFMachineTier::requiredCooks),
            EFUpgradeCost.CODEC.fieldOf("upgrade_cost").forGetter(EFMachineTier::upgradeCost),
            Codec.INT.fieldOf("cook_time").forGetter(EFMachineTier::cookTime),
            Codec.INT.fieldOf("batch_size").forGetter(EFMachineTier::batchSize)
    ).apply(instance, EFMachineTier::new)).flatXmap(EFMachineTier::validate, EFMachineTier::validate);

    private static DataResult<EFMachineTier> validate(EFMachineTier tier) {
        if (tier.level < 1) {
            return DataResult.error(() -> "level must be >= 1, got " + tier.level);
        }
        if (tier.cookTime < 1) {
            return DataResult.error(() -> "cook_time must be >= 1 at level " + tier.level);
        }
        if (tier.batchSize < 1) {
            return DataResult.error(() -> "batch_size must be >= 1 at level " + tier.level);
        }
        if (tier.requiredCooks < 0) {
            return DataResult.error(() -> "required_cooks must be >= 0 at level " + tier.level);
        }
        if (tier.upgradeCost.experienceLevels() < 0) {
            return DataResult.error(() -> "experience_levels must be >= 0 at level " + tier.level);
        }
        if (tier.displayName.byLocale().isEmpty()) {
            return DataResult.error(() -> "display_name must contain at least one locale at level " + tier.level);
        }
        if (!tier.displayName.byLocale().containsKey("en_us")) {
            return DataResult.error(() -> "display_name.en_us is required at level " + tier.level);
        }
        return DataResult.success(tier);
    }

    public String debugName() {
        return name.orElse("level_" + level);
    }
}
