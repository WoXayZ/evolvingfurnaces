package com.evolvingfurnaces.compat.jade;

import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.machine.EvolvingBrewingStandBlockEntity;
import com.evolvingfurnaces.machine.EvolvingMachineBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.TooltipPosition;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.JadeUI;

public enum EFJadeNameProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        Component name;
        if (accessor.getBlockEntity() instanceof EvolvingMachineBlockEntity machine) {
            name = IThemeHelper.get().title(machine.getDisplayName());
        } else if (accessor.getBlockEntity() instanceof EvolvingBrewingStandBlockEntity brewing) {
            name = IThemeHelper.get().title(brewing.getDisplayName());
        } else {
            return;
        }
        tooltip.remove(JadeIds.CORE_OBJECT_NAME);
        tooltip.add(0, JadeUI.text(name).tag(JadeIds.CORE_OBJECT_NAME));
    }

    @Override
    public Identifier getUid() {
        return EFConstants.id("machine_name");
    }

    @Override
    public int getDefaultPriority() {
        return TooltipPosition.HEAD + 1;
    }
}
