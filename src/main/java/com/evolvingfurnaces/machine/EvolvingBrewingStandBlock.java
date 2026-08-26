package com.evolvingfurnaces.machine;

import com.evolvingfurnaces.registry.EFBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class EvolvingBrewingStandBlock extends BrewingStandBlock {

    public EvolvingBrewingStandBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof EvolvingBrewingStandBlockEntity machine && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, machine, pos);
            player.awardStat(Stats.INTERACT_WITH_BREWINGSTAND);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EvolvingBrewingStandBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, EFBlockEntities.BREWING_STAND.get(), EvolvingBrewingStandBlockEntity::serverTick);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        ItemStack stack = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof EvolvingBrewingStandBlockEntity machine) {
            return EvolvingMachineItem.withLevel(asItem(), machine.machineLevel());
        }
        return stack;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof EvolvingBrewingStandBlockEntity machine) {
            machine.setMachineLevel(EvolvingMachineItem.levelOf(stack));
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof EvolvingBrewingStandBlockEntity machine
                && !machine.convertingToVanilla()) {
            Containers.dropContents(level, pos, machine);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.4 + random.nextDouble() * 0.2;
        double y = pos.getY() + 0.7 + random.nextDouble() * 0.3;
        double z = pos.getZ() + 0.4 + random.nextDouble() * 0.2;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
    }
}
