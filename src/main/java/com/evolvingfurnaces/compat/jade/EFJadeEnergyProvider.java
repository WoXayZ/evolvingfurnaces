package com.evolvingfurnaces.compat.jade;

import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.machine.EvolvingMachineBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.JadeUI;

public enum EFJadeEnergyProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains("EFModule")) {
            return;
        }
        tooltip.add(JadeUI.text(Component.translatable("evolvingfurnaces.gui.module." + data.getStringOr("EFModule", "none"))));
        tooltip.add(JadeUI.text(Component.translatable(
                "evolvingfurnaces.gui.energy",
                data.getIntOr("EFEnergy", 0),
                data.getIntOr("EFEnergyMax", 0))));
    }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof EvolvingMachineBlockEntity machine)) {
            return;
        }
        if (!machine.energyModule().installed()) {
            return;
        }
        tag.putString("EFModule", machine.energyModule().getSerializedName());
        tag.putInt("EFEnergy", machine.energyStored());
        tag.putInt("EFEnergyMax", machine.energyCapacity());
    }

    @Override
    public Identifier getUid() {
        return EFConstants.id("machine_energy");
    }
}
