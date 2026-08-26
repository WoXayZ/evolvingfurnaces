package com.evolvingfurnaces.machine;

import com.evolvingfurnaces.registry.EFBlockEntities;
import com.evolvingfurnaces.tier.EFMachineKind;
import com.evolvingfurnaces.tier.EFMachineTier;
import com.evolvingfurnaces.tier.EFTierManager;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.RecipeHolder;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public class EvolvingMachineBlockEntity extends BaseContainerBlockEntity
        implements WorldlyContainer, RecipeHolder, StackedContentsCompatible, EFIoHost {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FUEL = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int DATA_COUNT = 6;

    private static final int[] SLOT_NONE = new int[0];
    private static final int[] SLOT_INPUT_ONLY = {SLOT_INPUT};
    private static final int[] SLOT_FUEL_ONLY = {SLOT_FUEL};
    private static final int[] SLOT_OUTPUT_ONLY = {SLOT_OUTPUT};

    private final EFMachineKind kind;
    private NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private final EFMachineIo io = new EFMachineIo();
    private final EFMachineIoHandler[] handlers = new EFMachineIoHandler[7];
    @SuppressWarnings("unchecked")
    private final LazyOptional<IItemHandler>[] lazy = new LazyOptional[7];
    private int machineLevel = 1;
    private int cooks;
    private int litTime;
    private int litDuration;
    private int cookingProgress;
    private int cookingTotalTime;
    private int cachedCookTime = 200;
    private int cachedBatchSize = 1;
    private boolean saveThisTick;
    private boolean convertingToVanilla;
    @Nullable
    private Item cycleInputItem;
    private final Object2IntOpenHashMap<ResourceLocation> recipesUsed = new Object2IntOpenHashMap<>();
    private final RecipeManager.CachedCheck<Container, AbstractCookingRecipe> quickCheck;
    @Nullable
    private Recipe<?> usedRecipe;

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
        String path = state.getBlock().builtInRegistryHolder().key().location().getPath();
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
        this.cookingTotalTime = cachedCookTime;
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public IItemHandler handler(@Nullable Direction side) {
        int index = side == null ? 6 : side.ordinal();
        EFMachineIoHandler handler = handlers[index];
        if (handler == null) {
            handler = new EFMachineIoHandler(this, side);
            handlers[index] = handler;
        }
        return handler;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER && !isRemoved()) {
            int index = side == null ? 6 : side.ordinal();
            if (lazy[index] == null) {
                lazy[index] = LazyOptional.of(() -> handler(side));
            }
            return lazy[index].cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        for (int i = 0; i < lazy.length; i++) {
            if (lazy[i] != null) {
                lazy[i].invalidate();
                lazy[i] = null;
            }
        }
    }

    /**
     * At most one batch completion per game tick. Never catch-up-loop, never {@code progress += speed}.
     * A netherite furnace with batch 8 is one vanilla smelt's worth of work, then {@code grow(8)}.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, EvolvingMachineBlockEntity machine) {
        if (level instanceof ServerLevel server && EFDisableConverter.convertIfDisabled(server, pos, machine)) {
            return;
        }
        boolean wasLit = machine.isLit();
        boolean changed = false;
        if (wasLit) {
            machine.litTime--;
        }

        ItemStack input = machine.items.get(SLOT_INPUT);
        ItemStack fuel = machine.items.get(SLOT_FUEL);

        if (!machine.isLit() && machine.cookingProgress == 0 && (input.isEmpty() || fuel.isEmpty())) {
            if (wasLit) {
                state = state.setValue(AbstractFurnaceBlock.LIT, false);
                level.setBlock(pos, state, 3);
                setChanged(level, pos, state);
            }
            return;
        }

        if (machine.isLit()
                && machine.cookingProgress > 0
                && !input.isEmpty()
                && input.getItem() == machine.cycleInputItem) {
            machine.cookingProgress++;
            if (machine.cookingProgress >= machine.cookingTotalTime) {
                machine.cookingProgress = 0;
                changed |= machine.completeCycle(level);
            }
        } else {
            boolean cooking = machine.tryCook(level, fuel, input);
            changed |= machine.saveThisTick;
            if (!cooking && machine.cookingProgress > 0) {
                machine.cookingProgress = Mth.clamp(machine.cookingProgress - 2, 0, machine.cookingTotalTime);
                machine.cycleInputItem = null;
            }
        }

        if (wasLit != machine.isLit()) {
            changed = true;
            state = state.setValue(AbstractFurnaceBlock.LIT, machine.isLit());
            level.setBlock(pos, state, 3);
        }
        if (changed) {
            setChanged(level, pos, state);
        }
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
        AbstractCookingRecipe recipe = quickCheck.getRecipeFor(this, level).orElse(null);
        if (recipe == null) {
            return false;
        }
        ItemStack result = recipe.assemble(this, level.registryAccess());
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
            if (fuel.hasCraftingRemainingItem()) {
                setItem(SLOT_FUEL, fuel.getCraftingRemainingItem());
            } else {
                ItemStack remaining = fuel.copy();
                remaining.shrink(1);
                setItem(SLOT_FUEL, remaining.isEmpty() ? fuel.getCraftingRemainingItem() : remaining);
            }
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

    private boolean completeCycle(Level level) {
        ItemStack input = items.get(SLOT_INPUT);
        if (input.isEmpty()) {
            cycleInputItem = null;
            return false;
        }
        AbstractCookingRecipe recipe = quickCheck.getRecipeFor(this, level).orElse(null);
        if (recipe == null) {
            cycleInputItem = null;
            return false;
        }
        ItemStack result = recipe.assemble(this, level.registryAccess());
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
        } else if (ItemStack.isSameItemSameTags(output, result)) {
            outputRoom = output.getMaxStackSize() - output.getCount();
        } else {
            return 0;
        }
        int byOutput = outputRoom / resultCount;
        if (byOutput < 1) {
            return 0;
        }
        return Math.min(Math.min(cachedBatchSize, input.getCount()), byOutput);
    }

    private void finishCook(AbstractCookingRecipe recipe, ItemStack result, int batch) {
        ItemStack input = items.get(SLOT_INPUT).copy();
        ItemStack output = items.get(SLOT_OUTPUT).copy();
        int produced = result.getCount() * batch;
        if (output.isEmpty()) {
            setItem(SLOT_OUTPUT, EFStacks.copyWithCount(result, produced));
        } else {
            output.grow(produced);
            setItem(SLOT_OUTPUT, output);
        }
        input.shrink(batch);
        setItem(SLOT_INPUT, input.isEmpty() ? ItemStack.EMPTY : input);
        recipesUsed.addTo(recipe.getId(), batch);
        cooks += batch;
    }

    private void refreshTierStats() {
        EFMachineTier current = tier();
        cachedCookTime = current.cookTime();
        cachedBatchSize = current.batchSize();
    }

    protected int getBurnDuration(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        return ForgeHooks.getBurnTime(stack, kind.recipeType());
    }

    public boolean isLit() {
        return litTime > 0;
    }

    public void loadVanillaData(CompoundTag tag, ItemStack[] stacks, int newLevel) {
        load(tag);
        for (int i = 0; i < Math.min(stacks.length, items.size()); i++) {
            items.set(i, stacks[i]);
        }
        this.machineLevel = newLevel;
        refreshTierStats();
        this.cookingTotalTime = cachedCookTime;
        this.io.resetDefaults();
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public List<Recipe<?>> awardRecipesAndPopExperience(ServerLevel level, Vec3 pos) {
        List<Recipe<?>> awarded = new ArrayList<>();
        for (var entry : recipesUsed.object2IntEntrySet()) {
            level.getRecipeManager().byKey(entry.getKey()).ifPresent(recipe -> {
                awarded.add(recipe);
                if (recipe instanceof AbstractCookingRecipe cooking) {
                    createExperience(level, pos, entry.getIntValue(), cooking.getExperience());
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
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new EvolvingMachineMenu(containerId, inventory, this, dataAccess, kind);
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ContainerHelper.removeItem(items, slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_OUTPUT) {
            return false;
        }
        if (slot == SLOT_FUEL) {
            ItemStack fuel = items.get(SLOT_FUEL);
            return getBurnDuration(stack) > 0 || stack.is(Items.BUCKET) && !fuel.is(Items.BUCKET);
        }
        return level != null && quickCheck.getRecipeFor(new SimpleContainer(stack), level).isPresent();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return switch (io.get(side)) {
            case INPUT -> SLOT_INPUT_ONLY;
            case FUEL -> SLOT_FUEL_ONLY;
            case OUTPUT -> SLOT_OUTPUT_ONLY;
            case DISABLED -> SLOT_NONE;
        };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        if (direction != null && io.get(direction) == com.evolvingfurnaces.tier.EFIoRole.DISABLED) {
            return false;
        }
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return switch (io.get(direction)) {
            case OUTPUT -> slot == SLOT_OUTPUT;
            case FUEL -> slot == SLOT_FUEL && !stack.is(Items.LAVA_BUCKET);
            case INPUT, DISABLED -> false;
        };
    }

    @Override
    public void fillStackedContents(StackedContents contents) {
        for (ItemStack stack : items) {
            contents.accountStack(stack);
        }
    }

    @Override
    public void setRecipeUsed(@Nullable Recipe<?> recipe) {
        usedRecipe = recipe;
    }

    @Override
    @Nullable
    public Recipe<?> getRecipeUsed() {
        return usedRecipe;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Items", Tag.TAG_LIST)) {
            items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag, items);
        }
        litTime = tag.getInt("BurnTime");
        cookingProgress = tag.getInt("CookTime");
        cookingTotalTime = tag.getInt("CookTimeTotal");
        litDuration = tag.getInt("LitDuration");
        if (litDuration <= 0) {
            litDuration = getBurnDuration(items.get(SLOT_FUEL));
        }
        machineLevel = Math.max(1, tag.getInt("MachineLevel"));
        cooks = Math.max(0, tag.getInt("Cooks"));
        refreshTierStats();
        if (cookingTotalTime < 1) {
            cookingTotalTime = cachedCookTime;
        }
        if (cookingProgress > 0 && !items.get(SLOT_INPUT).isEmpty()) {
            cycleInputItem = items.get(SLOT_INPUT).getItem();
        }
        io.load(tag);
        recipesUsed.clear();
        CompoundTag used = tag.getCompound("RecipesUsed");
        for (String key : used.getAllKeys()) {
            recipesUsed.put(new ResourceLocation(key), used.getInt(key));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("BurnTime", litTime);
        tag.putInt("CookTime", cookingProgress);
        tag.putInt("CookTimeTotal", cookingTotalTime);
        tag.putInt("LitDuration", litDuration);
        tag.putInt("MachineLevel", machineLevel);
        tag.putInt("Cooks", cooks);
        ContainerHelper.saveAllItems(tag, items);
        io.save(tag);
        CompoundTag used = new CompoundTag();
        recipesUsed.forEach((id, count) -> used.putInt(id.toString(), count));
        tag.put("RecipesUsed", used);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt("MachineLevel", machineLevel);
        tag.putInt("Cooks", cooks);
        io.save(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
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
}
