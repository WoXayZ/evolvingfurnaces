package com.evolvingfurnaces.machine;

import com.evolvingfurnaces.registry.EFBlockEntities;
import com.evolvingfurnaces.registry.EFDataComponents;
import com.evolvingfurnaces.tier.EFMachineKind;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class EvolvingMachineBlock extends AbstractFurnaceBlock {

    private final EFMachineKind kind;
    private final MapCodec<EvolvingMachineBlock> codec;

    public EvolvingMachineBlock(Properties properties, EFMachineKind kind) {
        super(properties);
        this.kind = kind;
        this.codec = simpleCodec(props -> new EvolvingMachineBlock(props, kind));
    }

    public EFMachineKind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends AbstractFurnaceBlock> codec() {
        return codec;
    }

    @Override
    protected void openContainer(Level level, BlockPos pos, Player player) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof EvolvingMachineBlockEntity machine) {
            player.openMenu(machine, pos);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EvolvingMachineBlockEntity(kind, pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, EFBlockEntities.MACHINE.get(), EvolvingMachineBlockEntity::serverTick);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        ItemStack stack = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof EvolvingMachineBlockEntity machine) {
            stack.set(EFDataComponents.MACHINE_LEVEL.get(), machine.machineLevel());
        }
        return stack;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return getCloneItemStack(state, null, level, pos, null);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof EvolvingMachineBlockEntity machine) {
            machine.awardRecipesAndPopExperience(serverLevel, Vec3.atCenterOf(pos));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;
        if (random.nextDouble() < 0.1) {
            level.playLocalSound(x, y, z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
        }
        Direction facing = state.getValue(FACING);
        Direction.Axis axis = facing.getAxis();
        double offset = random.nextDouble() * 0.6 - 0.3;
        double dx = axis == Direction.Axis.X ? facing.getStepX() * 0.52 : offset;
        double dy = random.nextDouble() * 6.0 / 16.0;
        double dz = axis == Direction.Axis.Z ? facing.getStepZ() * 0.52 : offset;
        level.addParticle(ParticleTypes.SMOKE, x + dx, y + dy, z + dz, 0.0, 0.0, 0.0);
        level.addParticle(ParticleTypes.FLAME, x + dx, y + dy, z + dz, 0.0, 0.0, 0.0);
    }
}
