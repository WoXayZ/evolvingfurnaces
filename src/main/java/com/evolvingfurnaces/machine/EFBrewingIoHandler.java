package com.evolvingfurnaces.machine;

import com.evolvingfurnaces.tier.EFIoRole;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

public final class EFBrewingIoHandler implements ResourceHandler<ItemResource> {

    private final EvolvingBrewingStandBlockEntity machine;
    private final ResourceHandler<ItemResource> items;
    @Nullable
    private final Direction side;

    public EFBrewingIoHandler(EvolvingBrewingStandBlockEntity machine, @Nullable Direction side) {
        this.machine = machine;
        this.side = side;
        this.items = VanillaContainerWrapper.of(machine);
    }

    @Override
    public int size() {
        return machine.getContainerSize();
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
