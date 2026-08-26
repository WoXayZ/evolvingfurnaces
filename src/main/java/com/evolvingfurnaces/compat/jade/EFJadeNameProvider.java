package com.evolvingfurnaces.compat.jade;

import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.machine.EvolvingBrewingStandBlockEntity;
import com.evolvingfurnaces.machine.EvolvingMachineBlockEntity;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.TooltipPosition;
import snownee.jade.api.config.IPluginConfig;

public enum EFJadeNameProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (accessor.getBlockEntity() instanceof EvolvingMachineBlockEntity machine) {
            tooltip.add(machine.getDisplayName());
        } else if (accessor.getBlockEntity() instanceof EvolvingBrewingStandBlockEntity brewing) {
            tooltip.add(brewing.getDisplayName());
        }
    }

    @Override
    public ResourceLocation getUid() {
        return EFConstants.id("machine_name");
    }

    @Override
    public int getDefaultPriority() {
        return TooltipPosition.HEAD + 1;
    }
}
