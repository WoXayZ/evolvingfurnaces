package com.evolvingfurnaces.machine;

import com.evolvingfurnaces.registry.EFMenus;
import com.evolvingfurnaces.tier.EFMachineKind;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;

public class EvolvingMachineMenu extends AbstractFurnaceMenu {

    private final EFMachineKind kind;
    private final BlockPos blockPos;
    private final Inventory playerInventory;

    public EvolvingMachineMenu(int containerId, Inventory inventory, EvolvingMachineBlockEntity machine, ContainerData data, EFMachineKind kind) {
        super(EFMenus.MACHINE.get(), kind.recipeType(), kind.recipeBookType(), containerId, inventory, machine, data);
        this.kind = kind;
        this.blockPos = machine.getBlockPos();
        this.playerInventory = inventory;
    }

    public EvolvingMachineMenu(int containerId, Inventory inventory, net.minecraft.network.FriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos());
    }

    public EvolvingMachineMenu(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, resolve(inventory, pos), dataOf(inventory, pos), kindOf(inventory, pos));
    }

    public EFMachineKind kind() {
        return kind;
    }

    public BlockPos blockPos() {
        return blockPos;
    }

    public int machineLevel() {
        if (playerInventory.player.level().getBlockEntity(blockPos) instanceof EvolvingMachineBlockEntity machine) {
            return machine.machineLevel();
        }
        return 1;
    }

    public int cooks() {
        if (playerInventory.player.level().getBlockEntity(blockPos) instanceof EvolvingMachineBlockEntity machine) {
            return machine.cooks();
        }
        return 0;
    }

    private static EvolvingMachineBlockEntity resolve(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof EvolvingMachineBlockEntity machine) {
            return machine;
        }
        throw new IllegalStateException("Missing evolving machine at " + pos);
    }

    private static ContainerData dataOf(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof EvolvingMachineBlockEntity machine) {
            return machine.dataAccess();
        }
        return new SimpleContainerData(EvolvingMachineBlockEntity.DATA_COUNT);
    }

    private static EFMachineKind kindOf(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof EvolvingMachineBlockEntity machine) {
            return machine.kind();
        }
        return EFMachineKind.FURNACE;
    }
}
