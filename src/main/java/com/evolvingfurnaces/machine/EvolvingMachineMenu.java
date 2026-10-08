package com.evolvingfurnaces.machine;

import com.evolvingfurnaces.registry.EFMenus;
import com.evolvingfurnaces.tier.EFMachineKind;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class EvolvingMachineMenu extends AbstractFurnaceMenu {

    private final EFMachineKind kind;
    private final BlockPos blockPos;
    private final Inventory playerInventory;
    private final ContainerData data;

    public EvolvingMachineMenu(int containerId, Inventory inventory, EvolvingMachineBlockEntity machine, ContainerData data, EFMachineKind kind) {
        super(EFMenus.MACHINE.get(), kind.recipeType(), kind.recipeInputs(), kind.recipeBookType(), containerId, inventory, machine, data);
        this.kind = kind;
        this.blockPos = machine.getBlockPos();
        this.playerInventory = inventory;
        this.data = data;
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

    public int energyStored() {
        return unpack(data.get(EvolvingMachineBlockEntity.DATA_ENERGY_LOW), data.get(EvolvingMachineBlockEntity.DATA_ENERGY_HIGH));
    }

    public int energyCapacity() {
        return unpack(data.get(EvolvingMachineBlockEntity.DATA_CAPACITY_LOW), data.get(EvolvingMachineBlockEntity.DATA_CAPACITY_HIGH));
    }

    public EFEnergyModule energyModule() {
        int ordinal = data.get(EvolvingMachineBlockEntity.DATA_MODULE);
        EFEnergyModule[] values = EFEnergyModule.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return EFEnergyModule.NONE;
        }
        return values[ordinal];
    }

    @Override
    public boolean isLit() {
        if (energyModule() == EFEnergyModule.GENERATOR) {
            int cap = energyCapacity();
            if (cap > 0 && energyStored() >= cap) {
                return false;
            }
        }
        return super.isLit();
    }

    @Override
    protected boolean canSmelt(ItemStack stack) {
        if (energyModule() == EFEnergyModule.GENERATOR) {
            return false;
        }
        return super.canSmelt(stack);
    }

    @Override
    protected boolean isFuel(ItemStack stack) {
        if (energyModule() == EFEnergyModule.ELECTRIC) {
            return false;
        }
        return super.isFuel(stack);
    }

    private static int unpack(int low, int high) {
        return (low & 0x7FFF) | ((high & 0x7FFF) << 15);
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
