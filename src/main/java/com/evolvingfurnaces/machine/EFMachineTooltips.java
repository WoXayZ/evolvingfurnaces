package com.evolvingfurnaces.machine;

import com.evolvingfurnaces.tier.EFCostIngredient;
import com.evolvingfurnaces.tier.EFMachineKind;
import com.evolvingfurnaces.tier.EFMachineTier;
import com.evolvingfurnaces.tier.EFTierManager;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

public final class EFMachineTooltips {

    private EFMachineTooltips() {
    }

    public static void appendItem(ItemStack stack, List<Component> tooltip, @Nullable Player player) {
        if (!(stack.getItem() instanceof EvolvingMachineItem item)) {
            return;
        }
        int level = EvolvingMachineItem.levelOf(stack);
        int cooks = cooksOf(stack);
        append(tooltip, item.kind(), level, cooks, player);
    }

    public static void append(List<Component> tooltip, EFMachineKind kind, int level, int cooks, @Nullable Player player) {
        EFMachineTier current = EFTierManager.tierClient(kind, level)
                .or(() -> EFTierManager.tier(kind, level))
                .orElse(null);
        if (current == null) {
            return;
        }
        EFMachineTier next = EFTierManager.nextClient(kind, level)
                .or(() -> EFTierManager.next(kind, level))
                .orElse(null);
        int required = next == null ? current.requiredCooks() : next.requiredCooks();
        tooltip.add(Component.translatable("evolvingfurnaces.gui.level", current.level()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(kind.timeLangKey(), current.cookTime(), current.cookTime() / 20.0).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(kind.batchLangKey(), current.batchSize()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(kind.progressLangKey(), cooks, required).withStyle(ChatFormatting.GRAY));
        if (next == null) {
            tooltip.add(Component.translatable("evolvingfurnaces.gui.max_level").withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("evolvingfurnaces.gui.next", next.displayName().resolveForPlayer(player)).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("evolvingfurnaces.gui.xp_cost", next.upgradeCost().experienceLevels()).withStyle(ChatFormatting.GRAY));
            for (EFCostIngredient cost : next.upgradeCost().items()) {
                tooltip.add(Component.literal("- " + cost.count() + " ").append(costLabel(cost)).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    private static int cooksOf(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) {
            return 0;
        }
        CompoundTag tag = data.copyTag();
        return Math.max(0, tag.getInt("Cooks"));
    }

    private static Component costLabel(EFCostIngredient cost) {
        if (cost.itemId().isPresent()) {
            return Component.translatable(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(cost.itemId().get()).getDescriptionId());
        }
        return Component.literal("#" + cost.tagId().map(Object::toString).orElse("tag"));
    }
}
