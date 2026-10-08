package com.evolvingfurnaces.machine;

import com.evolvingfurnaces.registry.EFDataComponents;
import com.evolvingfurnaces.tier.EFMachineKind;
import com.evolvingfurnaces.tier.EFMachineTier;
import com.evolvingfurnaces.tier.EFTierManager;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

public class EvolvingMachineItem extends BlockItem {

    private final EFMachineKind kind;

    public EvolvingMachineItem(Block block, Properties properties, EFMachineKind kind) {
        super(block, properties);
        this.kind = kind;
    }

    public EFMachineKind kind() {
        return kind;
    }

    public static int levelOf(ItemStack stack) {
        Integer value = stack.get(EFDataComponents.MACHINE_LEVEL.get());
        return value == null ? 1 : Math.max(1, value);
    }

    public static EFEnergyModule moduleOf(ItemStack stack) {
        EFEnergyModule module = stack.get(EFDataComponents.ENERGY_MODULE.get());
        return module == null ? EFEnergyModule.NONE : module;
    }

    public static int energyOf(ItemStack stack) {
        Integer value = stack.get(EFDataComponents.ENERGY_STORED.get());
        return value == null ? 0 : Math.max(0, value);
    }

    public static ItemStack withLevel(Item item, int level) {
        ItemStack stack = new ItemStack(item);
        stack.set(EFDataComponents.MACHINE_LEVEL.get(), Math.max(1, level));
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        EFMachineTier tier = EFTierManager.tierClient(kind, levelOf(stack))
                .orElseGet(() -> EFTierManager.tier(kind, levelOf(stack)).orElse(null));
        if (tier == null) {
            return super.getName(stack);
        }
        return tier.displayName().toComponent();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        List<Component> lines = new ArrayList<>();
        EFMachineTooltips.appendItem(stack, lines, null);
        lines.forEach(tooltip);
    }
}
