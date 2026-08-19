package com.evolvingfurnaces.machine;

import com.evolvingfurnaces.registry.EFBlockEntities;
import com.evolvingfurnaces.registry.EFDataComponents;
import com.evolvingfurnaces.tier.EFMachineKind;
import com.evolvingfurnaces.tier.EFMachineTier;
import com.evolvingfurnaces.tier.EFTierManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public class EvolvingBrewingStandBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, EFIoHost {

    public static final int SLOT_LEFT = 0;
    public static final int SLOT_MIDDLE = 1;
    public static final int SLOT_RIGHT = 2;
    public static final int SLOT_INGREDIENT = 3;
    public static final int SLOT_FUEL = 4;
    public static final int DATA_COUNT = 6;
    public static final int FUEL_USES = 20;

    private static final int[] SLOT_NONE = new int[0];
    private static final int[] SLOT_POTIONS = {SLOT_LEFT, SLOT_MIDDLE, SLOT_RIGHT};
    private static final int[] SLOT_INPUTS = {SLOT_LEFT, SLOT_MIDDLE, SLOT_RIGHT, SLOT_INGREDIENT};
    private static final int[] SLOT_FUEL_ONLY = {SLOT_FUEL};

    private NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
    private final EFMachineIo io = new EFMachineIo();
    private final EFBrewingIoHandler[] handlers = new EFBrewingIoHandler[7];
    private int machineLevel = 1;
    private int cooks;
    private int brewTime;
    private int brewTimeTotal = 400;
    private int fuel;
    private int cachedBrewTime = 400;
    private int cachedBatchSize = 1;
    @Nullable
    private Item cycleIngredient;
    private boolean convertingToVanilla;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> brewTime;
                case 1 -> brewTimeTotal;
                case 2 -> fuel;
                case 3 -> cooks;
                case 4 -> machineLevel;
                case 5 -> cachedBatchSize;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> brewTime = value;
                case 1 -> brewTimeTotal = value;
                case 2 -> fuel = value;
                case 3 -> cooks = Math.max(0, value);
                case 4 -> {
                    machineLevel = Math.max(1, value);
                    refreshTierStats();
                }
                case 5 -> cachedBatchSize = Math.max(1, value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public EvolvingBrewingStandBlockEntity(BlockPos pos, BlockState state) {
        super(EFBlockEntities.BREWING_STAND.get(), pos, state);
        refreshTierStats();
    }

    @Override
    public EFMachineKind kind() {
        return EFMachineKind.BREWING_STAND;
    }

    @Override
    public int machineLevel() {
        return machineLevel;
    }

    @Override
    public int cooks() {
        return cooks;
    }

    public int brewTime() {
        return brewTime;
    }

    public int brewTimeTotal() {
        return brewTimeTotal;
    }

    public int fuel() {
        return fuel;
    }

    public int batchSize() {
        return Math.max(1, cachedBatchSize);
    }

    private EFMachineTier liveTier() {
        if (level != null && level.isClientSide) {
            return EFTierManager.tierClient(kind(), machineLevel)
                    .orElseGet(() -> EFTierManager.listClient(kind()).first().orElseThrow());
        }
        return tier();
    }

    @Override
    public EFMachineIo io() {
        return io;
    }

    public ContainerData dataAccess() {
        return dataAccess;
    }

    public EFMachineTier tier() {
        return EFTierManager.tier(kind(), machineLevel).orElseGet(() -> EFTierManager.list(kind()).first().orElseThrow());
    }

    public void markConvertingToVanilla() {
        convertingToVanilla = true;
    }

    public boolean convertingToVanilla() {
        return convertingToVanilla;
    }

    @Override
    public void setMachineLevel(int level) {
        int oldTotal = Math.max(1, brewTimeTotal);
        this.machineLevel = Math.max(1, level);
        refreshTierStats();
        if (brewTime > 0) {
            brewTime = Math.max(1, (int) Math.round(brewTime * (double) cachedBrewTime / oldTotal));
            brewTime = Math.min(brewTime, cachedBrewTime);
        }
        this.brewTimeTotal = cachedBrewTime;
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public IItemHandler handler(@Nullable Direction side) {
        int index = side == null ? 6 : side.ordinal();
        EFBrewingIoHandler handler = handlers[index];
        if (handler == null) {
            handler = new EFBrewingIoHandler(this, side);
            handlers[index] = handler;
        }
        return handler;
    }

    /**
     * At most one brew completion per game tick. Fuel is consumed once per operation.
     * Ingredient cost is the largest brewable potion stack among the three slots.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, EvolvingBrewingStandBlockEntity machine) {
        if (level instanceof ServerLevel server && EFDisableConverter.convertIfDisabled(server, pos, machine)) {
            return;
        }

        ItemStack fuelStack = machine.items.get(SLOT_FUEL);
        if (machine.fuel <= 0 && fuelStack.is(Items.BLAZE_POWDER)) {
            machine.fuel = FUEL_USES;
            fuelStack.shrink(1);
            setChanged(level, pos, state);
        }

        boolean canBrew = machine.isBrewable(level);
        if (machine.brewTime > 0) {
            machine.brewTime--;
            if (machine.brewTime == 0 && canBrew) {
                machine.completeBrew(level);
            } else if (!canBrew || machine.ingredientChanged()) {
                machine.brewTime = 0;
                machine.cycleIngredient = null;
            }
            setChanged(level, pos, state);
        } else if (canBrew && machine.fuel > 0) {
            machine.refreshTierStats();
            machine.fuel--;
            machine.brewTimeTotal = machine.cachedBrewTime;
            machine.brewTime = machine.brewTimeTotal;
            machine.cycleIngredient = machine.items.get(SLOT_INGREDIENT).getItem();
            setChanged(level, pos, state);
        }

        machine.updateBottles(level, pos, state);
    }

    private boolean ingredientChanged() {
        ItemStack ingredient = items.get(SLOT_INGREDIENT);
        return cycleIngredient != null && (ingredient.isEmpty() || ingredient.getItem() != cycleIngredient);
    }

    private boolean isBrewable(Level level) {
        PotionBrewing brewing = level.potionBrewing();
        ItemStack ingredient = items.get(SLOT_INGREDIENT);
        if (ingredient.isEmpty() || !brewing.isIngredient(ingredient)) {
            return false;
        }
        int cost = ingredientCost(brewing, ingredient);
        return cost > 0 && ingredient.getCount() >= cost;
    }

    /**
     * 8-7-4 consumes 8, 3-2-0 consumes 3. Empty slots and bottles that cannot mix are ignored.
     */
    private int ingredientCost(PotionBrewing brewing, ItemStack ingredient) {
        int max = 0;
        for (int slot = 0; slot < 3; slot++) {
            ItemStack bottle = items.get(slot);
            if (bottle.isEmpty() || !brewing.hasMix(single(bottle), ingredient)) {
                continue;
            }
            max = Math.max(max, bottle.getCount());
        }
        return max;
    }

    private void completeBrew(Level level) {
        PotionBrewing brewing = level.potionBrewing();
        ItemStack ingredient = items.get(SLOT_INGREDIENT);
        if (ingredient.isEmpty() || !brewing.isIngredient(ingredient)) {
            cycleIngredient = null;
            return;
        }
        int cost = ingredientCost(brewing, ingredient);
        if (cost < 1 || ingredient.getCount() < cost) {
            cycleIngredient = null;
            return;
        }
        int produced = 0;
        for (int slot = 0; slot < 3; slot++) {
            ItemStack bottle = items.get(slot);
            if (bottle.isEmpty()) {
                continue;
            }
            ItemStack one = single(bottle);
            if (!brewing.hasMix(one, ingredient)) {
                continue;
            }
            ItemStack result = brewing.mix(ingredient, one);
            if (result.isEmpty()) {
                continue;
            }
            result.setCount(bottle.getCount());
            items.set(slot, result);
            produced += result.getCount();
        }
        if (produced <= 0) {
            cycleIngredient = null;
            return;
        }
        consumeIngredient(level, ingredient, cost);
        cooks += produced;
        cycleIngredient = null;
        level.levelEvent(LevelEvent.SOUND_BREWING_STAND_BREW, worldPosition, 0);
    }

    private void consumeIngredient(Level level, ItemStack ingredient, int cost) {
        ItemStack leftover = ingredient.getCraftingRemainingItem();
        ingredient.shrink(cost);
        if (!leftover.isEmpty()) {
            leftover = leftover.copyWithCount(leftover.getCount() * cost);
            if (ingredient.isEmpty()) {
                ingredient = leftover;
            } else {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), leftover);
            }
        }
        items.set(SLOT_INGREDIENT, ingredient);
    }

    private void updateBottles(Level level, BlockPos pos, BlockState state) {
        BlockState next = state;
        boolean changed = false;
        for (int i = 0; i < BrewingStandBlock.HAS_BOTTLE.length; i++) {
            boolean has = !items.get(i).isEmpty();
            if (next.getValue(BrewingStandBlock.HAS_BOTTLE[i]) != has) {
                next = next.setValue(BrewingStandBlock.HAS_BOTTLE[i], has);
                changed = true;
            }
        }
        if (changed) {
            level.setBlock(pos, next, Block.UPDATE_CLIENTS);
        }
    }

    public void loadVanillaData(CompoundTag tag, ItemStack[] stacks, int newLevel, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
        for (int i = 0; i < Math.min(stacks.length, items.size()); i++) {
            items.set(i, stacks[i]);
        }
        this.machineLevel = newLevel;
        refreshTierStats();
        this.brewTimeTotal = cachedBrewTime;
        this.io.resetDefaults();
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void refreshTierStats() {
        EFMachineTier current = liveTier();
        cachedBrewTime = current.cookTime();
        cachedBatchSize = current.batchSize();
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (!stack.isEmpty()) {
            int limit;
            if (isPotionSlot(slot)) {
                limit = batchSize();
            } else if (slot == SLOT_INGREDIENT) {
                limit = Math.max(stack.getMaxStackSize(), batchSize());
            } else {
                limit = Math.min(getMaxStackSize(), stack.getMaxStackSize());
            }
            stack.limitSize(limit);
        }
        setChanged();
    }

    private static ItemStack single(ItemStack stack) {
        return stack.copyWithCount(1);
    }

    public boolean isPotionSlot(int slot) {
        return slot >= SLOT_LEFT && slot <= SLOT_RIGHT;
    }

    @Override
    protected Component getDefaultName() {
        return tier().displayName().toComponent();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new EvolvingBrewingStandMenu(containerId, inventory, this, dataAccess);
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (level == null) {
            return false;
        }
        PotionBrewing brewing = level.potionBrewing();
        if (isPotionSlot(slot)) {
            if (!brewing.isInput(stack)) {
                return false;
            }
            ItemStack existing = items.get(slot);
            return existing.isEmpty() || ItemStack.isSameItemSameComponents(existing, stack);
        }
        if (slot == SLOT_INGREDIENT) {
            return brewing.isIngredient(stack);
        }
        if (slot == SLOT_FUEL) {
            return stack.is(Items.BLAZE_POWDER);
        }
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return switch (io.get(side)) {
            case INPUT -> SLOT_INPUTS;
            case FUEL -> SLOT_FUEL_ONLY;
            case OUTPUT -> SLOT_POTIONS;
            case DISABLED -> SLOT_NONE;
        };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        if (direction != null && io.get(direction) == com.evolvingfurnaces.tier.EFIoRole.DISABLED) {
            return false;
        }
        if (direction != null) {
            return switch (io.get(direction)) {
                case INPUT -> slot != SLOT_FUEL && canPlaceItem(slot, stack);
                case FUEL -> slot == SLOT_FUEL && canPlaceItem(slot, stack);
                case OUTPUT, DISABLED -> false;
            };
        }
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return switch (io.get(direction)) {
            case OUTPUT -> isPotionSlot(slot);
            case FUEL -> slot == SLOT_FUEL;
            case INPUT, DISABLED -> false;
        };
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        if (!stack.isEmpty() && level != null) {
            PotionBrewing brewing = level.potionBrewing();
            if (brewing.isInput(stack)) {
                return batchSize();
            }
            if (brewing.isIngredient(stack)) {
                return Math.max(stack.getMaxStackSize(), batchSize());
            }
        }
        return Math.min(getMaxStackSize(), stack.getMaxStackSize());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Items", Tag.TAG_LIST)) {
            items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag, items, registries);
        }
        brewTime = tag.getInt("BrewTime");
        brewTimeTotal = tag.getInt("BrewTimeTotal");
        fuel = tag.getInt("Fuel");
        machineLevel = Math.max(1, tag.getInt("MachineLevel"));
        cooks = Math.max(0, tag.getInt("Cooks"));
        refreshTierStats();
        if (brewTimeTotal < 1) {
            brewTimeTotal = cachedBrewTime;
        }
        if (brewTime > 0 && !items.get(SLOT_INGREDIENT).isEmpty()) {
            cycleIngredient = items.get(SLOT_INGREDIENT).getItem();
        }
        io.load(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("BrewTime", brewTime);
        tag.putInt("BrewTimeTotal", brewTimeTotal);
        tag.putInt("Fuel", fuel);
        tag.putInt("MachineLevel", machineLevel);
        tag.putInt("Cooks", cooks);
        ContainerHelper.saveAllItems(tag, items, registries);
        io.save(tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("MachineLevel", machineLevel);
        tag.putInt("Cooks", cooks);
        io.save(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        if (tag.contains("MachineLevel", Tag.TAG_INT)) {
            machineLevel = Math.max(1, tag.getInt("MachineLevel"));
            refreshTierStats();
        }
        if (tag.contains("Cooks", Tag.TAG_INT)) {
            cooks = tag.getInt("Cooks");
        }
        io.load(tag);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        machineLevel = Math.max(1, input.getOrDefault(EFDataComponents.MACHINE_LEVEL.get(), 1));
        refreshTierStats();
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(EFDataComponents.MACHINE_LEVEL.get(), machineLevel);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("MachineLevel");
    }
}
