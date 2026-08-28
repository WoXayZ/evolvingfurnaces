package com.evolvingfurnaces.machine;

import com.evolvingfurnaces.tier.EFIoRole;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public final class EFMachineIoHandler implements IItemHandler {

    public static final int INPUT = 0;
    public static final int FUEL = 1;
    public static final int OUTPUT = 2;

    private final EvolvingMachineBlockEntity machine;
    @Nullable
    private final Direction side;

    public EFMachineIoHandler(EvolvingMachineBlockEntity machine, @Nullable Direction side) {
        this.machine = machine;
        this.side = side;
    }

    @Override
    public int getSlots() {
        return 3;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return machine.getItem(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !isInsertSlot(slot) || !machine.canPlaceItem(slot, stack)) {
            return stack;
        }
        ItemStack existing = machine.getItem(slot);
        int limit = Math.min(stack.getMaxStackSize(), machine.getMaxStackSize());
        if (!existing.isEmpty()) {
            if (!ItemStack.isSameItemSameTags(existing, stack)) {
                return stack;
            }
            limit -= existing.getCount();
        }
        if (limit <= 0) {
            return stack;
        }
        int toInsert = Math.min(stack.getCount(), limit);
        if (!simulate) {
            if (existing.isEmpty()) {
                machine.setItem(slot, EFStacks.copyWithCount(stack, toInsert));
            } else {
                existing.grow(toInsert);
                machine.setChanged();
            }
        }
        return EFStacks.copyWithCount(stack, stack.getCount() - toInsert);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0 || !isExtractSlot(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack existing = machine.getItem(slot);
        if (existing.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int toExtract = Math.min(amount, existing.getCount());
        ItemStack result = EFStacks.copyWithCount(existing, toExtract);
        if (!simulate) {
            existing.shrink(toExtract);
            machine.setChanged();
        }
        return result;
    }

    @Override
    public int getSlotLimit(int slot) {
        return machine.getMaxStackSize();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return isInsertSlot(slot) && machine.canPlaceItem(slot, stack);
    }

    private EFIoRole role() {
        return side == null ? null : machine.io().get(side);
    }

    private boolean isInsertSlot(int slot) {
        EFIoRole role = role();
        if (role == null) {
            return slot != OUTPUT;
        }
        return switch (role) {
            case INPUT -> slot == INPUT;
            case FUEL -> slot == FUEL;
            case OUTPUT, DISABLED, ENERGY -> false;
        };
    }

    private boolean isExtractSlot(int slot) {
        EFIoRole role = role();
        if (role == null) {
            return slot == OUTPUT || slot == FUEL;
        }
        return switch (role) {
            case OUTPUT -> slot == OUTPUT;
            case FUEL -> slot == FUEL;
            case INPUT, DISABLED, ENERGY -> false;
        };
    }
}
