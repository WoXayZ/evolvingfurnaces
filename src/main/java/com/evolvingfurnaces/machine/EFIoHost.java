package com.evolvingfurnaces.machine;

import com.evolvingfurnaces.tier.EFMachineKind;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface EFIoHost {

    EFMachineKind kind();

    int machineLevel();

    int cooks();

    void setMachineLevel(int level);

    default EFEnergyModule energyModule() {
        return EFEnergyModule.NONE;
    }

    EFMachineIo io();

    BlockPos getBlockPos();

    BlockState getBlockState();

    Level getLevel();

    void setChanged();
}
