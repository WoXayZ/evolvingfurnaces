package com.evolvingfurnaces.compat.jade;

import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.machine.EvolvingMachineBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.TooltipPosition;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.IElementHelper;

public enum EFJadeNameProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof EvolvingMachineBlockEntity machine)) {
            return;
        }
        Component name = IThemeHelper.get().title(machine.getDisplayName());
        tooltip.remove(JadeIds.CORE_OBJECT_NAME);
        tooltip.add(0, IElementHelper.get().text(name).tag(JadeIds.CORE_OBJECT_NAME));
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
