package com.evolvingfurnaces.tier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

public record EFCostIngredient(Optional<Identifier> itemId, Optional<Identifier> tagId, int count) {

    public static final Codec<EFCostIngredient> CODEC = RecordCodecBuilder.<EFCostIngredient>create(instance -> instance.group(
            Identifier.CODEC.optionalFieldOf("item").forGetter(EFCostIngredient::itemId),
            Identifier.CODEC.optionalFieldOf("tag").forGetter(EFCostIngredient::tagId),
            Codec.INT.optionalFieldOf("count", 1).forGetter(EFCostIngredient::count)
    ).apply(instance, EFCostIngredient::new)).flatXmap(EFCostIngredient::validate, EFCostIngredient::validate);

    private static DataResult<EFCostIngredient> validate(EFCostIngredient value) {
        boolean hasItem = value.itemId.isPresent();
        boolean hasTag = value.tagId.isPresent();
        if (hasItem == hasTag) {
            return DataResult.error(() -> "upgrade cost entry must have exactly one of 'item' or 'tag'");
        }
        if (value.count < 1) {
            return DataResult.error(() -> "upgrade cost count must be >= 1");
        }
        return DataResult.success(value);
    }

    public static EFCostIngredient item(String id, int count) {
        return new EFCostIngredient(Optional.of(Identifier.parse(id)), Optional.empty(), count);
    }

    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (itemId.isPresent()) {
            return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(itemId.get());
        }
        TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId.orElseThrow());
        return stack.is(tag);
    }

    public ItemStack displayStack() {
        if (itemId.isPresent()) {
            Item item = BuiltInRegistries.ITEM.get(itemId.get()).map(holder -> holder.value()).orElse(Items.AIR);
            return new ItemStack(item, Math.min(count, item.getDefaultMaxStackSize()));
        }
        return ItemStack.EMPTY;
    }
}
