package com.evolvingfurnaces.machine;

import com.evolvingfurnaces.tier.EFIoRole;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public final class EFBrewingIoHandler implements IItemHandler {

    private final EvolvingBrewingStandBlockEntity machine;
    @Nullable
    private final Direction side;

    public EFBrewingIoHandler(EvolvingBrewingStandBlockEntity machine, @Nullable Direction side) {
        this.machine = machine;
        this.side = side;
    }

    @Override
    public int getSlots() {
        return machine.getContainerSize();
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
        int limit = slotLimit(slot, stack);
        if (!existing.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(existing, stack)) {
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
                machine.setItem(slot, stack.copyWithCount(toInsert));
            } else {
                existing.grow(toInsert);
                machine.setChanged();
            }
        }
        return stack.copyWithCount(stack.getCount() - toInsert);
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
        int toExtract = Math.min(amount, Math.min(existing.getCount(), existing.getMaxStackSize()));
        ItemStack result = existing.copyWithCount(toExtract);
        if (!simulate) {
            existing.shrink(toExtract);
            machine.setChanged();
        }
        return result;
    }

    @Override
    public int getSlotLimit(int slot) {
        return slotLimit(slot, machine.getItem(slot));
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return isInsertSlot(slot) && machine.canPlaceItem(slot, stack);
    }

    private int slotLimit(int slot, ItemStack stack) {
        if (machine.isPotionSlot(slot)) {
            return machine.batchSize();
        }
        if (slot == EvolvingBrewingStandBlockEntity.SLOT_INGREDIENT) {
            if (stack.isEmpty()) {
                return Math.max(machine.getMaxStackSize(), machine.batchSize());
            }
            return Math.max(stack.getMaxStackSize(), machine.batchSize());
        }
        if (stack.isEmpty()) {
            return machine.getMaxStackSize();
        }
        return Math.min(stack.getMaxStackSize(), machine.getMaxStackSize());
    }

    private EFIoRole role() {
        return side == null ? null : machine.io().get(side);
    }

    private boolean isInsertSlot(int slot) {
        EFIoRole role = role();
        if (role == null) {
            return slot != -1;
        }
        return switch (role) {
            case INPUT -> slot == EvolvingBrewingStandBlockEntity.SLOT_INGREDIENT || machine.isPotionSlot(slot);
            case FUEL -> slot == EvolvingBrewingStandBlockEntity.SLOT_FUEL;
            case OUTPUT, DISABLED, ENERGY -> false;
        };
    }

    private boolean isExtractSlot(int slot) {
        EFIoRole role = role();
        if (role == null) {
            return machine.isPotionSlot(slot) || slot == EvolvingBrewingStandBlockEntity.SLOT_FUEL;
        }
        return switch (role) {
            case OUTPUT -> machine.isPotionSlot(slot);
            case FUEL -> slot == EvolvingBrewingStandBlockEntity.SLOT_FUEL;
            case INPUT, DISABLED, ENERGY -> false;
        };
    }
}
