package com.evolvingfurnaces.machine;

import com.evolvingfurnaces.client.render.EvolvingMachineItemRenderer;
import com.evolvingfurnaces.tier.EFMachineKind;
import com.evolvingfurnaces.tier.EFMachineTier;
import com.evolvingfurnaces.tier.EFTierManager;
import java.util.List;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

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
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("MachineLevel", Tag.TAG_INT)) {
            return Math.max(1, tag.getInt("MachineLevel"));
        }
        if (tag != null && tag.contains("BlockEntityTag", Tag.TAG_COMPOUND)) {
            CompoundTag be = tag.getCompound("BlockEntityTag");
            if (be.contains("MachineLevel", Tag.TAG_INT)) {
                return Math.max(1, be.getInt("MachineLevel"));
            }
        }
        return 1;
    }

    public static EFEnergyModule moduleOf(ItemStack stack) {
        String name = readString(stack, "EnergyModule");
        if (name == null || name.isEmpty()) {
            return EFEnergyModule.NONE;
        }
        try {
            return EFEnergyModule.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return EFEnergyModule.NONE;
        }
    }

    public static int energyOf(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("Energy", Tag.TAG_INT)) {
            return Math.max(0, tag.getInt("Energy"));
        }
        if (tag != null && tag.contains("BlockEntityTag", Tag.TAG_COMPOUND)) {
            return Math.max(0, tag.getCompound("BlockEntityTag").getInt("Energy"));
        }
        return 0;
    }

    public static ItemStack withMachine(Item item, int level, EFEnergyModule module, int energy) {
        ItemStack stack = withLevel(item, level);
        if (module.installed()) {
            stack.getOrCreateTag().putString("EnergyModule", module.getSerializedName());
            stack.getOrCreateTagElement("BlockEntityTag").putString("EnergyModule", module.getSerializedName());
        }
        if (energy > 0) {
            stack.getOrCreateTag().putInt("Energy", energy);
            stack.getOrCreateTagElement("BlockEntityTag").putInt("Energy", energy);
        }
        return stack;
    }

    @Nullable
    private static String readString(ItemStack stack, String key) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(key, Tag.TAG_STRING)) {
            return tag.getString(key);
        }
        if (tag != null && tag.contains("BlockEntityTag", Tag.TAG_COMPOUND)) {
            CompoundTag be = tag.getCompound("BlockEntityTag");
            if (be.contains(key, Tag.TAG_STRING)) {
                return be.getString(key);
            }
        }
        return null;
    }

    public static ItemStack withLevel(Item item, int level) {
        ItemStack stack = new ItemStack(item);
        int value = Math.max(1, level);
        stack.getOrCreateTag().putInt("MachineLevel", value);
        stack.getOrCreateTagElement("BlockEntityTag").putInt("MachineLevel", value);
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
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        EFMachineTooltips.appendItem(stack, tooltip, null);
    }

    @Override
    public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return EvolvingMachineItemRenderer.instance();
            }
        });
    }
}
