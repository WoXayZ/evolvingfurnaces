package com.evolvingfurnaces.tier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public record EFStyleRef(ResourceLocation blockId, EFTextureFace texture) {

    public static final Codec<EFStyleRef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("block").forGetter(EFStyleRef::blockId),
            EFTextureFace.CODEC.optionalFieldOf("texture", EFTextureFace.SIDE).forGetter(EFStyleRef::texture)
    ).apply(instance, EFStyleRef::new));

    public Block block() {
        return BuiltInRegistries.BLOCK.get(blockId);
    }
}
