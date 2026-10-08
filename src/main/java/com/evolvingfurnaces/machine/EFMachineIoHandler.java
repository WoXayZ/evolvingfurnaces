package com.evolvingfurnaces.machine;

import com.evolvingfurnaces.tier.EFIoRole;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

public final class EFMachineIoHandler implements ResourceHandler<ItemResource> {

    public static final int INPUT = 0;
    public static final int FUEL = 1;
    public static final int OUTPUT = 2;

    private final EvolvingMachineBlockEntity machine;
    private final ResourceHandler<ItemResource> items;
    @Nullable
    private final Direction side;

    public EFMachineIoHandler(EvolvingMachineBlockEntity machine, @Nullable Direction side) {
        this.machine = machine;
        this.side = side;
        this.items = VanillaContainerWrapper.of(machine);
    }

    @Override
    public int size() {
        return 3;
    }

    @Override
    public ItemResource getResource(int index) {
        return items.getResource(index);
    }

    @Override
    public long getAmountAsLong(int index) {
        return items.getAmountAsLong(index);
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        return items.getCapacityAsLong(index, resource);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return isInsertSlot(index) && machine.canPlaceItem(index, resource.toStack());
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (!isInsertSlot(index)) {
            return 0;
        }
        return items.insert(index, resource, amount, transaction);
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (!isExtractSlot(index)) {
            return 0;
        }
        return items.extract(index, resource, amount, transaction);
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
