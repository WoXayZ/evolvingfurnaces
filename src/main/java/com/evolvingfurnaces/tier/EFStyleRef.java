package com.evolvingfurnaces.tier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public record EFStyleRef(Identifier blockId, EFTextureFace texture) {

    public static final Codec<EFStyleRef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("block").forGetter(EFStyleRef::blockId),
            EFTextureFace.CODEC.optionalFieldOf("texture", EFTextureFace.SIDE).forGetter(EFStyleRef::texture)
    ).apply(instance, EFStyleRef::new));

    public Block block() {
        return BuiltInRegistries.BLOCK.get(blockId).map(holder -> holder.value()).orElse(Blocks.AIR);
    }
}
