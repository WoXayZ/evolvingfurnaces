package com.evolvingfurnaces.machine;

import com.evolvingfurnaces.EFConfig;
import com.evolvingfurnaces.registry.EFBlockEntities;
import com.evolvingfurnaces.registry.EFDataComponents;
import com.evolvingfurnaces.tier.EFIoRole;
import com.evolvingfurnaces.tier.EFMachineKind;
import com.evolvingfurnaces.tier.EFMachineTier;
import com.evolvingfurnaces.tier.EFTierManager;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

public class EvolvingMachineBlockEntity extends BaseContainerBlockEntity
        implements WorldlyContainer, RecipeCraftingHolder, StackedContentsCompatible, EFIoHost {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FUEL = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int DATA_ENERGY_LOW = 6;
    public static final int DATA_ENERGY_HIGH = 7;
    public static final int DATA_CAPACITY_LOW = 8;
    public static final int DATA_CAPACITY_HIGH = 9;
    public static final int DATA_MODULE = 10;
    public static final int DATA_COUNT = 11;

    private static final int[] SLOT_NONE = new int[0];
    private static final int[] SLOT_INPUT_ONLY = {SLOT_INPUT};
    private static final int[] SLOT_FUEL_ONLY = {SLOT_FUEL};
    private static final int[] SLOT_OUTPUT_ONLY = {SLOT_OUTPUT};

    private final EFMachineKind kind;
    private NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private final EFMachineIo io = new EFMachineIo();
    private final EFMachineIoHandler[] handlers = new EFMachineIoHandler[7];
    private int machineLevel = 1;
    private int cooks;
    private int litTime;
    private int litDuration;
    private int cookingProgress;
    private int cookingTotalTime;
    private int cachedCookTime = 200;
    private int cachedBatchSize = 1;
    private EFEnergyModule energyModule = EFEnergyModule.NONE;
    private final EFMachineEnergy energy = new EFMachineEnergy(this);
    private boolean saveThisTick;
    private boolean convertingToVanilla;
    @Nullable
    private Item cycleInputItem;
    private static final Codec<Map<ResourceKey<Recipe<?>>, Integer>> RECIPES_USED_CODEC = Codec.unboundedMap(Recipe.KEY_CODEC, Codec.INT);
    private final Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> recipesUsed = new Reference2IntOpenHashMap<>();
    private final RecipeManager.CachedCheck<SingleRecipeInput, AbstractCookingRecipe> quickCheck;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> litTime;
                case 1 -> litDuration;
                case 2 -> cookingProgress;
                case 3 -> cookingTotalTime;
                case 4 -> cooks;
                case 5 -> machineLevel;
                case DATA_ENERGY_LOW -> packLow(energy.stored());
                case DATA_ENERGY_HIGH -> packHigh(energy.stored());
                case DATA_CAPACITY_LOW -> packLow(energyCapacity());
                case DATA_CAPACITY_HIGH -> packHigh(energyCapacity());
                case DATA_MODULE -> energyModule.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> litTime = value;
                case 1 -> litDuration = value;
                case 2 -> cookingProgress = value;
                case 3 -> cookingTotalTime = value;
                case 4 -> cooks = Math.max(0, value);
                case 5 -> machineLevel = Math.max(1, value);
                case DATA_ENERGY_LOW -> energy.setStored(unpack(value, packHigh(energy.stored())));
                case DATA_ENERGY_HIGH -> energy.setStored(unpack(packLow(energy.stored()), value));
                case DATA_MODULE -> energyModule = moduleOf(value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public EvolvingMachineBlockEntity(BlockPos pos, BlockState state) {
        this(EFMachineKind.fromVanilla(state.getBlock()) == null
                ? inferKind(state)
                : EFMachineKind.fromVanilla(state.getBlock()), pos, state);
    }

    public EvolvingMachineBlockEntity(EFMachineKind kind, BlockPos pos, BlockState state) {
        super(EFBlockEntities.MACHINE.get(), pos, state);
        this.kind = kind;
        this.quickCheck = RecipeManager.createCheck(kind.recipeType());
    }

    private static EFMachineKind inferKind(BlockState state) {
        String path = state.getBlock().builtInRegistryHolder().key().identifier().getPath();
        if (path.contains("blast")) {
            return EFMachineKind.BLAST_FURNACE;
        }
        if (path.contains("smoker")) {
            return EFMachineKind.SMOKER;
        }
        return EFMachineKind.FURNACE;
    }

    public EFMachineKind kind() {
        return kind;
    }

    public int machineLevel() {
        return machineLevel;
    }

    public int cooks() {
        return cooks;
    }

    public EFMachineIo io() {
        return io;
    }

    public EFEnergyModule energyModule() {
        return energyModule;
    }

    public int energyStored() {
        return energy.stored();
    }

    public int energyCapacity() {
        return Math.max(0, tier().energyCapacity());
    }

    public void onEnergyChanged() {
        setChanged();
    }

    public void markConvertingToVanilla() {
        convertingToVanilla = true;
    }

    public boolean convertingToVanilla() {
        return convertingToVanilla;
    }

    public ContainerData dataAccess() {
        return dataAccess;
    }

    public EFMachineTier tier() {
        return EFTierManager.tier(kind, machineLevel).orElseGet(() -> EFTierManager.list(kind).first().orElseThrow());
    }

    public void setMachineLevel(int level) {
        this.machineLevel = Math.max(1, level);
        refreshTierStats();
        energy.clampToCapacity();
        this.cookingTotalTime = cachedCookTime;
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void setEnergyModule(EFEnergyModule module) {
        if (energyModule == module) {
            return;
        }
        energyModule = module;
        cookingProgress = 0;
        cycleInputItem = null;
        litTime = 0;
        litDuration = 0;
        io.applyModuleDefaults(module);
        setChanged();
        if (this.level != null) {
            this.level.invalidateCapabilities(worldPosition);
            this.level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public ResourceHandler<ItemResource> handler(@Nullable Direction side) {
        int index = side == null ? 6 : side.ordinal();
        EFMachineIoHandler handler = handlers[index];
        if (handler == null) {
            handler = new EFMachineIoHandler(this, side);
            handlers[index] = handler;
        }
        return handler;
    }

    @Nullable
    public EnergyHandler energyHandler(@Nullable Direction side) {
        if (!exposesEnergy(side)) {
            return null;
        }
        return energy;
    }

    public boolean exposesEnergy(@Nullable Direction side) {
        if (!energyModule.installed()) {
            return false;
        }
        if (side == null) {
            return true;
        }
        EFIoRole role = io.get(side);
        if (role == EFIoRole.ENERGY) {
            return true;
        }
        if (energyModule == EFEnergyModule.GENERATOR && role == EFIoRole.OUTPUT) {
            return true;
        }
        return energyModule == EFEnergyModule.ELECTRIC && role == EFIoRole.INPUT;
    }

    /**
     * At most one batch completion per game tick. Never catch-up-loop, never {@code progress += speed}.
     * A netherite furnace with batch 8 is one vanilla smelt's worth of work, then {@code grow(8)}.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, EvolvingMachineBlockEntity machine) {
        if (level instanceof ServerLevel server && EFDisableConverter.convertIfDisabled(server, pos, machine)) {
            return;
        }
        if (machine.energyModule.installed() && !EFConfig.moduleAvailable(machine.kind(), machine.energyModule)) {
            machine.setEnergyModule(EFEnergyModule.NONE);
        }
        boolean changed = switch (machine.energyModule) {
            case GENERATOR -> machine.tickGenerator();
            case ELECTRIC -> machine.tickElectric(level);
            case NONE -> machine.tickFurnace(level);
        };
        if (machine.energyModule == EFEnergyModule.GENERATOR) {
            changed |= machine.pushEnergy();
        }
        boolean lit = machine.isLit();
        if (state.getValue(AbstractFurnaceBlock.LIT) != lit) {
            changed = true;
            state = state.setValue(AbstractFurnaceBlock.LIT, lit);
            level.setBlock(pos, state, 3);
        }
        if (changed) {
            setChanged(level, pos, state);
        }
    }

    private boolean tickFurnace(Level level) {
        boolean wasLit = isLit();
        boolean changed = false;
        if (wasLit) {
            litTime--;
        }

        ItemStack input = items.get(SLOT_INPUT);
        ItemStack fuel = items.get(SLOT_FUEL);

        if (!isLit() && cookingProgress == 0 && (input.isEmpty() || fuel.isEmpty())) {
            return wasLit;
        }

        if (isLit()
                && cookingProgress > 0
                && !input.isEmpty()
                && input.getItem() == cycleInputItem) {
            cookingProgress++;
            if (cookingProgress >= cookingTotalTime) {
                cookingProgress = 0;
                changed |= completeCycle(level);
            }
        } else {
            boolean cooking = tryCook(level, fuel, input);
            changed |= saveThisTick;
            if (!cooking && cookingProgress > 0) {
                cookingProgress = Mth.clamp(cookingProgress - 2, 0, cookingTotalTime);
                cycleInputItem = null;
            }
        }
        return changed || wasLit != isLit();
    }

    private boolean tickGenerator() {
        refreshTierStats();
        boolean changed = false;
        int rate = tier().generatorOutput();
        if (litTime > 0 && energy.hasRoom()) {
            litTime--;
            energy.addInternal(rate);
            changed = true;
        }
        if (litTime <= 0 && energy.hasRoom()) {
            ItemStack fuel = items.get(SLOT_FUEL);
            int burn = generatorBurnDuration(fuel);
            if (burn > 0) {
                consumeFuel(fuel, generatorFuelCount(fuel));
                litDuration = burn;
                litTime = burn;
                energy.addInternal(rate);
                changed = true;
            }
        }
        return changed;
    }

    private boolean pushEnergy() {
        if (level == null || energy.stored() <= 0) {
            return false;
        }
        boolean changed = false;
        for (Direction direction : Direction.values()) {
            if (io.get(direction) != EFIoRole.ENERGY || energy.stored() <= 0) {
                continue;
            }
            EnergyHandler target = level.getCapability(
                    net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK,
                    worldPosition.relative(direction),
                    direction.getOpposite());
            if (target == null) {
                continue;
            }
            try (Transaction transaction = Transaction.openRoot()) {
                int moved = EnergyHandlerUtil.move(energy, target, energy.stored(), transaction);
                if (moved > 0) {
                    transaction.commit();
                    changed = true;
                }
            }
        }
        return changed;
    }

    public void ejectUnusedSlots(Player player, EFEnergyModule module) {
        int[] slots = switch (module) {
            case GENERATOR -> new int[] {SLOT_INPUT, SLOT_OUTPUT};
            case ELECTRIC -> new int[] {SLOT_FUEL};
            case NONE -> new int[0];
        };
        for (int slot : slots) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            setItem(slot, ItemStack.EMPTY);
            EFPlayerCosts.giveOrDrop(player, stack);
        }
    }

    private boolean tickElectric(Level level) {
        ItemStack input = items.get(SLOT_INPUT);
        if (cookingProgress == 0 && input.isEmpty()) {
            litTime = 0;
            return false;
        }
        boolean cooking;
        if (cookingProgress > 0 && !input.isEmpty() && input.getItem() == cycleInputItem) {
            cooking = consumeEnergyForCook();
            if (cooking) {
                cookingProgress++;
                if (cookingProgress >= cookingTotalTime) {
                    cookingProgress = 0;
                    cooking = completeCycle(level) || cooking;
                }
            } else if (cookingProgress > 0) {
                cookingProgress = Mth.clamp(cookingProgress - 2, 0, cookingTotalTime);
                cycleInputItem = null;
            }
        } else {
            cooking = tryCookElectric(level, input);
            if (!cooking && cookingProgress > 0) {
                cookingProgress = Mth.clamp(cookingProgress - 2, 0, cookingTotalTime);
                cycleInputItem = null;
            }
        }
        litTime = cooking ? 1 : 0;
        litDuration = 1;
        return cooking;
    }

    private boolean consumeEnergyForCook() {
        return energy.tryConsume(tier().energyConsume());
    }

    private boolean tryCookElectric(Level level, ItemStack input) {
        saveThisTick = false;
        if (input.isEmpty()) {
            return false;
        }
        if (cookingProgress == 0) {
            refreshTierStats();
            cookingTotalTime = cachedCookTime;
        }
        RecipeHolder<AbstractCookingRecipe> recipe = findRecipe(level, input).orElse(null);
        if (recipe == null) {
            return false;
        }
        ItemStack result = recipe.value().assemble(new SingleRecipeInput(input));
        int batch = computeBatch(result);
        if (batch < 1) {
            return false;
        }
        cycleInputItem = input.getItem();
        if (!consumeEnergyForCook()) {
            cookingProgress = 0;
            cycleInputItem = null;
            return false;
        }
        cookingProgress++;
        if (cookingProgress >= cookingTotalTime) {
            cookingProgress = 0;
            saveThisTick |= completeCycle(level);
        }
        return true;
    }

    private boolean tryCook(Level level, ItemStack fuel, ItemStack input) {
        saveThisTick = false;
        if (input.isEmpty()) {
            return false;
        }
        if (cookingProgress == 0) {
            refreshTierStats();
            cookingTotalTime = cachedCookTime;
        }
        RecipeHolder<AbstractCookingRecipe> recipe = findRecipe(level, input).orElse(null);
        if (recipe == null) {
            return false;
        }
        ItemStack result = recipe.value().assemble(new SingleRecipeInput(input));
        int batch = computeBatch(result);
        if (batch < 1) {
            return false;
        }
        cycleInputItem = input.getItem();
        if (!isLit()) {
            int burn = getBurnDuration(fuel);
            if (burn <= 0) {
                cookingProgress = 0;
                cycleInputItem = null;
                return false;
            }
            litDuration = burn;
            litTime = burn;
            saveThisTick = true;
            consumeFuel(fuel, 1);
        }
        if (!isLit()) {
            cookingProgress = 0;
            cycleInputItem = null;
            return false;
        }
        cookingProgress++;
        if (cookingProgress >= cookingTotalTime) {
            cookingProgress = 0;
            saveThisTick |= completeCycle(level);
        }
        return true;
    }

    private void consumeFuel(ItemStack fuel, int count) {
        ItemStackTemplate remainder = fuel.getCraftingRemainder();
        if (remainder != null) {
            setItem(SLOT_FUEL, remainder.create());
            return;
        }
        ItemStack remaining = fuel.copy();
        remaining.shrink(Math.max(1, count));
        setItem(SLOT_FUEL, remaining.isEmpty() ? ItemStack.EMPTY : remaining);
    }

    private int generatorFuelCount(ItemStack fuel) {
        if (fuel.getCraftingRemainder() != null) {
            return 1;
        }
        return Math.min(Math.max(1, tier().batchSize()), fuel.getCount());
    }

    private boolean completeCycle(Level level) {
        ItemStack input = items.get(SLOT_INPUT);
        if (input.isEmpty()) {
            cycleInputItem = null;
            return false;
        }
        RecipeHolder<AbstractCookingRecipe> recipe = findRecipe(level, input).orElse(null);
        if (recipe == null) {
            cycleInputItem = null;
            return false;
        }
        ItemStack result = recipe.value().assemble(new SingleRecipeInput(input));
        int batch = computeBatch(result);
        if (batch < 1) {
            cycleInputItem = null;
            return false;
        }
        finishCook(recipe, result, batch);
        return true;
    }

    private int computeBatch(ItemStack result) {
        if (result.isEmpty()) {
            return 0;
        }
        ItemStack input = items.get(SLOT_INPUT);
        ItemStack output = items.get(SLOT_OUTPUT);
        int resultCount = result.getCount();
        int outputRoom;
        if (output.isEmpty()) {
            outputRoom = result.getMaxStackSize();
        } else if (ItemStack.isSameItemSameComponents(output, result)) {
            outputRoom = output.getMaxStackSize() - output.getCount();
        } else {
            return 0;
        }
        int byOutput = outputRoom / resultCount;
        if (byOutput < 1) {
            return 0;
        }
        return Math.min(Math.min(tier().batchSize(), input.getCount()), byOutput);
    }

    private void finishCook(RecipeHolder<AbstractCookingRecipe> recipe, ItemStack result, int batch) {
        ItemStack input = items.get(SLOT_INPUT).copy();
        ItemStack output = items.get(SLOT_OUTPUT).copy();
        int produced = result.getCount() * batch;
        if (output.isEmpty()) {
            setItem(SLOT_OUTPUT, result.copyWithCount(produced));
        } else {
            output.grow(produced);
            setItem(SLOT_OUTPUT, output);
        }
        input.shrink(batch);
        setItem(SLOT_INPUT, input.isEmpty() ? ItemStack.EMPTY : input);
        recipesUsed.addTo(recipe.id(), batch);
        cooks += batch;
    }

    private void refreshTierStats() {
        EFMachineTier current = tier();
        cachedCookTime = current.cookTime();
        cachedBatchSize = current.batchSize();
    }

    private int generatorBurnDuration(ItemStack stack) {
        int burn = getBurnDuration(stack);
        if (burn <= 0) {
            return 0;
        }
        return Math.max(1, burn / 4);
    }

    protected int getBurnDuration(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (level == null) {
            return 0;
        }
        return stack.getBurnTime(kind.recipeType(), level.fuelValues());
    }

    public boolean isLit() {
        if (energyModule == EFEnergyModule.GENERATOR) {
            return litTime > 0 && energy.hasRoom();
        }
        return litTime > 0;
    }

    public void loadVanillaData(CompoundTag tag, ItemStack[] stacks, int newLevel, HolderLookup.Provider registries) {
        loadAdditional(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag));
        for (int i = 0; i < Math.min(stacks.length, items.size()); i++) {
            items.set(i, stacks[i]);
        }
        this.machineLevel = newLevel;
        refreshTierStats();
        this.cookingTotalTime = cachedCookTime;
        this.io.resetDefaults();
        this.energyModule = EFEnergyModule.NONE;
        this.energy.setStored(0);
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (!convertingToVanilla && this.level instanceof ServerLevel serverLevel) {
            awardRecipesAndPopExperience(serverLevel, Vec3.atCenterOf(pos));
        }
        super.preRemoveSideEffects(pos, state);
    }

    public List<RecipeHolder<?>> awardRecipesAndPopExperience(ServerLevel level, Vec3 pos) {
        List<RecipeHolder<?>> awarded = new ArrayList<>();
        for (var entry : recipesUsed.reference2IntEntrySet()) {
            level.recipeAccess().byKey(entry.getKey()).ifPresent(holder -> {
                awarded.add(holder);
                if (holder.value() instanceof AbstractCookingRecipe cooking) {
                    createExperience(level, pos, entry.getIntValue(), cooking.experience());
                }
            });
        }
        return awarded;
    }

    private static void createExperience(ServerLevel level, Vec3 pos, int count, float experience) {
        int amount = Mth.floor(count * experience);
        float fraction = Mth.frac(count * experience);
        if (fraction != 0.0F && Math.random() < fraction) {
            amount++;
        }
        ExperienceOrb.award(level, pos, amount);
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
        return new EvolvingMachineMenu(containerId, inventory, this, dataAccess, kind);
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_OUTPUT) {
            return false;
        }
        if (slot == SLOT_FUEL) {
            if (energyModule == EFEnergyModule.ELECTRIC) {
                return false;
            }
            ItemStack fuel = items.get(SLOT_FUEL);
            return getBurnDuration(stack) > 0 || stack.is(Items.BUCKET) && !fuel.is(Items.BUCKET);
        }
        if (energyModule == EFEnergyModule.GENERATOR) {
            return false;
        }
        if (level == null || level.isClientSide()) {
            return !stack.isEmpty();
        }
        return findRecipe(level, stack).isPresent();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return switch (io.get(side)) {
            case INPUT -> SLOT_INPUT_ONLY;
            case FUEL -> SLOT_FUEL_ONLY;
            case OUTPUT -> SLOT_OUTPUT_ONLY;
            case DISABLED, ENERGY -> SLOT_NONE;
        };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        if (direction != null && (io.get(direction) == EFIoRole.DISABLED || io.get(direction) == EFIoRole.ENERGY)) {
            return false;
        }
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return switch (io.get(direction)) {
            case OUTPUT -> slot == SLOT_OUTPUT;
            case FUEL -> slot == SLOT_FUEL && !stack.is(Items.LAVA_BUCKET);
            case INPUT, DISABLED, ENERGY -> false;
        };
    }

    @Override
    public void fillStackedContents(StackedItemContents contents) {
        for (ItemStack stack : items) {
            contents.accountStack(stack);
        }
    }

    @Override
    public void setRecipeUsed(@Nullable RecipeHolder<?> recipe) {
        // recipesUsed is updated in finishCook
    }

    @Override
    @Nullable
    public RecipeHolder<?> getRecipeUsed() {
        return null;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        litTime = input.getIntOr("BurnTime", 0);
        cookingProgress = input.getIntOr("CookTime", 0);
        cookingTotalTime = input.getIntOr("CookTimeTotal", 0);
        litDuration = input.getIntOr("LitDuration", 0);
        if (litDuration <= 0) {
            litDuration = getBurnDuration(items.get(SLOT_FUEL));
        }
        machineLevel = Math.max(1, input.getIntOr("MachineLevel", 1));
        cooks = Math.max(0, input.getIntOr("Cooks", 0));
        energyModule = moduleOf(input.getStringOr("EnergyModule", ""));
        energy.setStored(input.getIntOr("Energy", 0));
        refreshTierStats();
        energy.clampToCapacity();
        if (cookingTotalTime < 1) {
            cookingTotalTime = cachedCookTime;
        }
        if (cookingProgress > 0 && !items.get(SLOT_INPUT).isEmpty()) {
            cycleInputItem = items.get(SLOT_INPUT).getItem();
        }
        io.load(input);
        recipesUsed.clear();
        recipesUsed.putAll(input.read("RecipesUsed", RECIPES_USED_CODEC).orElse(Map.of()));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("BurnTime", litTime);
        output.putInt("CookTime", cookingProgress);
        output.putInt("CookTimeTotal", cookingTotalTime);
        output.putInt("LitDuration", litDuration);
        output.putInt("MachineLevel", machineLevel);
        output.putInt("Cooks", cooks);
        output.putString("EnergyModule", energyModule.getSerializedName());
        output.putInt("Energy", energy.stored());
        ContainerHelper.saveAllItems(output, items);
        io.save(output);
        output.store("RecipesUsed", RECIPES_USED_CODEC, recipesUsed);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        output.putInt("MachineLevel", machineLevel);
        output.putInt("Cooks", cooks);
        output.putString("EnergyModule", energyModule.getSerializedName());
        output.putInt("Energy", energy.stored());
        io.save(output);
        return output.buildResult();
    }

    @Override
    public void onDataPacket(Connection net, ValueInput input) {
        handleUpdateTag(input);
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        if (input.getInt("MachineLevel").isPresent()) {
            machineLevel = Math.max(1, input.getIntOr("MachineLevel", 1));
            refreshTierStats();
        }
        if (input.getInt("Cooks").isPresent()) {
            cooks = input.getIntOr("Cooks", 0);
        }
        if (input.getString("EnergyModule").isPresent()) {
            energyModule = moduleOf(input.getStringOr("EnergyModule", ""));
        }
        if (input.getInt("Energy").isPresent()) {
            energy.setStored(input.getIntOr("Energy", 0));
        }
        if (!input.childrenListOrEmpty("Io").isEmpty()) {
            io.load(input);
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter input) {
        super.applyImplicitComponents(input);
        machineLevel = Math.max(1, input.getOrDefault(EFDataComponents.MACHINE_LEVEL.get(), 1));
        energyModule = input.getOrDefault(EFDataComponents.ENERGY_MODULE.get(), EFEnergyModule.NONE);
        energy.setStored(input.getOrDefault(EFDataComponents.ENERGY_STORED.get(), 0));
        refreshTierStats();
        energy.clampToCapacity();
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(EFDataComponents.MACHINE_LEVEL.get(), machineLevel);
        if (energyModule.installed()) {
            builder.set(EFDataComponents.ENERGY_MODULE.get(), energyModule);
        }
        if (energy.stored() > 0) {
            builder.set(EFDataComponents.ENERGY_STORED.get(), energy.stored());
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("MachineLevel");
        output.discard("EnergyModule");
        output.discard("Energy");
    }

    private Optional<RecipeHolder<AbstractCookingRecipe>> findRecipe(Level level, ItemStack stack) {
        if (stack.isEmpty() || !(level instanceof ServerLevel serverLevel)) {
            return Optional.empty();
        }
        return quickCheck.getRecipeFor(new SingleRecipeInput(stack), serverLevel);
    }

    private static int packLow(int value) {
        return value & 0x7FFF;
    }

    private static int packHigh(int value) {
        return (value >>> 15) & 0x7FFF;
    }

    private static int unpack(int low, int high) {
        return (low & 0x7FFF) | ((high & 0x7FFF) << 15);
    }

    private static EFEnergyModule moduleOf(int ordinal) {
        EFEnergyModule[] values = EFEnergyModule.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return EFEnergyModule.NONE;
        }
        return values[ordinal];
    }

    private static EFEnergyModule moduleOf(String name) {
        if (name == null || name.isEmpty()) {
            return EFEnergyModule.NONE;
        }
        try {
            return EFEnergyModule.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return EFEnergyModule.NONE;
        }
    }
}
