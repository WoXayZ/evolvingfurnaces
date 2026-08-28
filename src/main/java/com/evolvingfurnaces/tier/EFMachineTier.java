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
        int batchSize,
        int energyCapacity,
        int energyGenerate,
        int energyConsume
) {

    public static final Codec<EFMachineTier> CODEC = RecordCodecBuilder.<EFMachineTier>create(instance -> instance.group(
            Codec.INT.fieldOf("level").forGetter(EFMachineTier::level),
            Codec.STRING.optionalFieldOf("name").forGetter(EFMachineTier::name),
            EFDisplayNames.CODEC.fieldOf("display_name").forGetter(EFMachineTier::displayName),
            EFStyleRef.CODEC.fieldOf("style").forGetter(EFMachineTier::style),
            Codec.INT.optionalFieldOf("required_cooks").forGetter(tier -> Optional.of(tier.requiredCooks)),
            Codec.INT.optionalFieldOf("required_brews").forGetter(tier -> Optional.empty()),
            EFUpgradeCost.CODEC.fieldOf("upgrade_cost").forGetter(EFMachineTier::upgradeCost),
            Codec.INT.optionalFieldOf("cook_time").forGetter(tier -> Optional.of(tier.cookTime)),
            Codec.INT.optionalFieldOf("brew_time").forGetter(tier -> Optional.empty()),
            Codec.INT.fieldOf("batch_size").forGetter(EFMachineTier::batchSize),
            Codec.INT.optionalFieldOf("energy_capacity", 0).forGetter(EFMachineTier::energyCapacity),
            Codec.INT.optionalFieldOf("energy_generate", 0).forGetter(EFMachineTier::energyGenerate),
            Codec.INT.optionalFieldOf("energy_consume", 0).forGetter(EFMachineTier::energyConsume)
    ).apply(instance, EFMachineTier::fromCodec)).flatXmap(EFMachineTier::validate, EFMachineTier::validate);

    private static EFMachineTier fromCodec(
            int level,
            Optional<String> name,
            EFDisplayNames displayName,
            EFStyleRef style,
            Optional<Integer> requiredCooks,
            Optional<Integer> requiredBrews,
            EFUpgradeCost upgradeCost,
            Optional<Integer> cookTime,
            Optional<Integer> brewTime,
            int batchSize,
            int energyCapacity,
            int energyGenerate,
            int energyConsume
    ) {
        return new EFMachineTier(
                level,
                name,
                displayName,
                style,
                requiredCooks.orElseGet(() -> requiredBrews.orElse(-1)),
                upgradeCost,
                cookTime.orElseGet(() -> brewTime.orElse(-1)),
                batchSize,
                energyCapacity,
                energyGenerate,
                energyConsume
        );
    }

    private static DataResult<EFMachineTier> validate(EFMachineTier tier) {
        if (tier.level < 1) {
            return DataResult.error(() -> "level must be >= 1, got " + tier.level);
        }
        if (tier.cookTime < 1) {
            return DataResult.error(() -> "cook_time/brew_time must be >= 1 at level " + tier.level);
        }
        if (tier.batchSize < 1) {
            return DataResult.error(() -> "batch_size must be >= 1 at level " + tier.level);
        }
        if (tier.requiredCooks < 0) {
            return DataResult.error(() -> "required_cooks/required_brews must be >= 0 at level " + tier.level);
        }
        if (tier.upgradeCost.experienceLevels() < 0) {
            return DataResult.error(() -> "experience_levels must be >= 0 at level " + tier.level);
        }
        if (tier.energyCapacity < 0) {
            return DataResult.error(() -> "energy_capacity must be >= 0 at level " + tier.level);
        }
        if (tier.energyGenerate < 0) {
            return DataResult.error(() -> "energy_generate must be >= 0 at level " + tier.level);
        }
        if (tier.energyConsume < 0) {
            return DataResult.error(() -> "energy_consume must be >= 0 at level " + tier.level);
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

    public int generatorOutput() {
        if (energyGenerate <= 0) {
            return 0;
        }
        return Math.max(1, energyGenerate / 4);
    }
}
