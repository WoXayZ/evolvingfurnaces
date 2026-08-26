package com.evolvingfurnaces.machine;

import com.evolvingfurnaces.registry.EFMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.level.block.entity.BlockEntity;

public class EvolvingBrewingStandMenu extends AbstractContainerMenu {

    private final EvolvingBrewingStandBlockEntity stand;
    private final ContainerData data;
    private final BlockPos blockPos;

    public EvolvingBrewingStandMenu(
            int containerId,
            Inventory inventory,
            EvolvingBrewingStandBlockEntity stand,
            ContainerData data
    ) {
        super(EFMenus.BREWING_STAND.get(), containerId);
        this.stand = stand;
        this.data = data;
        this.blockPos = stand.getBlockPos();
        addSlot(new PotionSlot(stand, 0, 56, 51));
        addSlot(new PotionSlot(stand, 1, 79, 58));
        addSlot(new PotionSlot(stand, 2, 102, 51));
        addSlot(new IngredientSlot(stand, 3, 79, 17));
        addSlot(new FuelSlot(stand, 4, 17, 17));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
        addDataSlots(data);
    }

    public EvolvingBrewingStandMenu(int containerId, Inventory inventory, net.minecraft.network.FriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos());
    }

    public EvolvingBrewingStandMenu(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, resolve(inventory, pos), dataOf(inventory, pos));
    }

    public BlockPos blockPos() {
        return blockPos;
    }

    public int machineLevel() {
        return stand.machineLevel();
    }

    public int cooks() {
        return stand.cooks();
    }

    public int brewTime() {
        return data.get(0);
    }

    public int brewTimeTotal() {
        return Math.max(1, data.get(1));
    }

    public int fuel() {
        return data.get(2);
    }

    @Override
    public boolean stillValid(Player player) {
        return stand.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < 5) {
            boolean moved = index == 4 ? moveItemStackTo(stack, 5, 41, true) : moveToPlayer(stack);
            if (!moved) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(Items.BLAZE_POWDER)) {
            if (!moveItemStackTo(stack, 4, 5, false)) {
                return ItemStack.EMPTY;
            }
        } else if (isIngredient(stack)) {
            if (!moveIntoBatchSlots(stack, 3, 4)) {
                return ItemStack.EMPTY;
            }
        } else if (isPotion(stack)) {
            if (!moveIntoBatchSlots(stack, 0, 3)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    private boolean moveToPlayer(ItemStack stack) {
        if (stack.getMaxStackSize() <= 1) {
            return moveUnstackedToPlayer(stack);
        }
        return moveItemStackTo(stack, 5, 41, true);
    }

    private boolean moveUnstackedToPlayer(ItemStack stack) {
        boolean moved = false;
        while (!stack.isEmpty()) {
            ItemStack one = EFStacks.copyWithCount(stack, 1);
            if (!moveItemStackTo(one, 5, 41, true)) {
                break;
            }
            stack.shrink(1);
            moved = true;
        }
        return moved;
    }

    /**
     * Vanilla {@code moveItemStackTo} skips merging when {@code ItemStack.isStackable()} is false,
     * so potions and turtle helmets (max stack 1) never fill a brewing slot past 1 on shift-click.
     */
    private boolean moveIntoBatchSlots(ItemStack stack, int start, int end) {
        boolean moved = false;
        for (int i = start; i < end && !stack.isEmpty(); i++) {
            Slot slot = slots.get(i);
            ItemStack existing = slot.getItem();
            if (existing.isEmpty() || !ItemStack.isSameItemSameTags(existing, stack) || !slot.mayPlace(stack)) {
                continue;
            }
            int space = slot.getMaxStackSize(stack) - existing.getCount();
            if (space <= 0) {
                continue;
            }
            int toMove = Math.min(space, stack.getCount());
            existing.grow(toMove);
            stack.shrink(toMove);
            slot.set(existing);
            moved = true;
        }
        for (int i = start; i < end && !stack.isEmpty(); i++) {
            Slot slot = slots.get(i);
            if (!slot.getItem().isEmpty() || !slot.mayPlace(stack)) {
                continue;
            }
            int toMove = Math.min(slot.getMaxStackSize(stack), stack.getCount());
            slot.setByPlayer(stack.split(toMove));
            moved = true;
        }
        return moved;
    }

    private boolean isIngredient(ItemStack stack) {
        return PotionBrewing.isIngredient(stack);
    }

    private boolean isPotion(ItemStack stack) {
        return net.minecraftforge.common.brewing.BrewingRecipeRegistry.isValidInput(stack);
    }

    private static EvolvingBrewingStandBlockEntity resolve(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof EvolvingBrewingStandBlockEntity machine) {
            return machine;
        }
        throw new IllegalStateException("Missing evolving brewing stand at " + pos);
    }

    private static ContainerData dataOf(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof EvolvingBrewingStandBlockEntity machine) {
            return machine.dataAccess();
        }
        return new SimpleContainerData(EvolvingBrewingStandBlockEntity.DATA_COUNT);
    }

    private static final class PotionSlot extends Slot {
        private final EvolvingBrewingStandBlockEntity stand;

        private PotionSlot(EvolvingBrewingStandBlockEntity stand, int slot, int x, int y) {
            super(stand, slot, x, y);
            this.stand = stand;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stand.canPlaceItem(getContainerSlot(), stack);
        }

        @Override
        public int getMaxStackSize() {
            return stand.batchSize();
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return stand.batchSize();
        }
    }

    private static final class IngredientSlot extends Slot {
        private final EvolvingBrewingStandBlockEntity stand;

        private IngredientSlot(EvolvingBrewingStandBlockEntity stand, int slot, int x, int y) {
            super(stand, slot, x, y);
            this.stand = stand;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stand.canPlaceItem(getContainerSlot(), stack);
        }

        @Override
        public int getMaxStackSize() {
            return Math.max(64, stand.batchSize());
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return Math.max(stack.getMaxStackSize(), stand.batchSize());
        }
    }

    private static final class FuelSlot extends Slot {
        private FuelSlot(EvolvingBrewingStandBlockEntity stand, int slot, int x, int y) {
            super(stand, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(Items.BLAZE_POWDER);
        }
    }
}
