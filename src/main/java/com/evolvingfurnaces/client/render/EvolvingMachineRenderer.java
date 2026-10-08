package com.evolvingfurnaces.client.render;

import com.evolvingfurnaces.machine.EFEnergyModule;
import com.evolvingfurnaces.machine.EvolvingMachineBlockEntity;
import com.evolvingfurnaces.tier.EFMachineKind;
import com.evolvingfurnaces.tier.EFMachineTier;
import com.evolvingfurnaces.tier.EFTierManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EvolvingMachineRenderer implements BlockEntityRenderer<EvolvingMachineBlockEntity, EvolvingMachineRenderer.State> {

    private static final float EXPAND = 0.002F;

    public EvolvingMachineRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            EvolvingMachineBlockEntity machine,
            State state,
            float partialTick,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(machine, state, partialTick, cameraPosition, breakProgress);
        state.kind = machine.kind();
        state.machineLevel = machine.machineLevel();
        state.blockState = machine.getBlockState();
        state.module = machine.energyModule();
        // A solid furnace stores no light in its own cell. The model shades each face from the block in front of it.
        BlockPos origin = machine.getBlockPos();
        Level level = machine.getLevel();
        for (Direction direction : Direction.values()) {
            state.faceLight[direction.get3DDataValue()] = level == null
                    ? state.lightCoords
                    : LevelRenderer.getLightCoords(level, origin.relative(direction));
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        renderOverlay(
                state.kind,
                state.machineLevel,
                state.blockState,
                state.module,
                poseStack,
                collector,
                state.faceLight,
                0,
                OverlayTexture.NO_OVERLAY);
    }

    public static void renderOverlay(
            EFMachineKind kind,
            int machineLevel,
            BlockState state,
            EFEnergyModule module,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int packedLight,
            int packedOverlay
    ) {
        renderOverlay(kind, machineLevel, state, module, poseStack, collector, null, packedLight, packedOverlay);
    }

    private static void renderOverlay(
            EFMachineKind kind,
            int machineLevel,
            BlockState state,
            EFEnergyModule module,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int[] faceLight,
            int packedLight,
            int packedOverlay
    ) {
        EFMachineTier tier = EFTierManager.tierClient(kind, machineLevel).orElse(null);
        if (tier == null) {
            return;
        }
        boolean lit = state.hasProperty(AbstractFurnaceBlock.LIT) && state.getValue(AbstractFurnaceBlock.LIT);
        Direction facing = state.hasProperty(AbstractFurnaceBlock.FACING) ? state.getValue(AbstractFurnaceBlock.FACING) : Direction.NORTH;
        float rotY = switch (facing) {
            case SOUTH -> 180F;
            case WEST -> 270F;
            case EAST -> 90F;
            default -> 0F;
        };

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotY));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        float x0 = -EXPAND;
        float y0 = -EXPAND;
        float z0 = -EXPAND;
        float x1 = 1F + EXPAND;
        float y1 = 1F + EXPAND;
        float z1 = 1F + EXPAND;
        for (Direction face : Direction.values()) {
            Identifier texture = EFMachineTextureCompositor.texture(kind, tier, face, lit, module);
            int light = lightFor(face, facing, faceLight, packedLight);
            collector.submitCustomGeometry(poseStack, EFOverlayRenderType.offset(texture), (pose, consumer) ->
                    renderFace(pose, consumer, face, facing, x0, y0, z0, x1, y1, z1, light));
        }
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public EFMachineKind kind = EFMachineKind.FURNACE;
        public int machineLevel = 1;
        public BlockState blockState;
        public EFEnergyModule module = EFEnergyModule.NONE;
        public final int[] faceLight = new int[6];
    }

    private static int lightFor(Direction modelFace, Direction blockFacing, int[] faceLight, int packedLight) {
        if (faceLight == null) {
            return packedLight;
        }
        return faceLight[worldFace(modelFace, blockFacing).get3DDataValue()];
    }

    private static void renderFace(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            Direction face,
            Direction blockFacing,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            int light
    ) {
        int shade = faceShade(worldFace(face, blockFacing));
        switch (face) {
            case DOWN -> {
                vertex(pose, consumer, x0, y0, z1, 0, 0, light, shade);
                vertex(pose, consumer, x0, y0, z0, 0, 1, light, shade);
                vertex(pose, consumer, x1, y0, z0, 1, 1, light, shade);
                vertex(pose, consumer, x1, y0, z1, 1, 0, light, shade);
            }
            case UP -> {
                vertex(pose, consumer, x0, y1, z0, 0, 0, light, shade);
                vertex(pose, consumer, x0, y1, z1, 0, 1, light, shade);
                vertex(pose, consumer, x1, y1, z1, 1, 1, light, shade);
                vertex(pose, consumer, x1, y1, z0, 1, 0, light, shade);
            }
            case NORTH -> {
                vertex(pose, consumer, x1, y1, z0, 0, 0, light, shade);
                vertex(pose, consumer, x1, y0, z0, 0, 1, light, shade);
                vertex(pose, consumer, x0, y0, z0, 1, 1, light, shade);
                vertex(pose, consumer, x0, y1, z0, 1, 0, light, shade);
            }
            case SOUTH -> {
                vertex(pose, consumer, x0, y1, z1, 0, 0, light, shade);
                vertex(pose, consumer, x0, y0, z1, 0, 1, light, shade);
                vertex(pose, consumer, x1, y0, z1, 1, 1, light, shade);
                vertex(pose, consumer, x1, y1, z1, 1, 0, light, shade);
            }
            case WEST -> {
                vertex(pose, consumer, x0, y1, z0, 0, 0, light, shade);
                vertex(pose, consumer, x0, y0, z0, 0, 1, light, shade);
                vertex(pose, consumer, x0, y0, z1, 1, 1, light, shade);
                vertex(pose, consumer, x0, y1, z1, 1, 0, light, shade);
            }
            case EAST -> {
                vertex(pose, consumer, x1, y1, z1, 0, 0, light, shade);
                vertex(pose, consumer, x1, y0, z1, 0, 1, light, shade);
                vertex(pose, consumer, x1, y0, z0, 1, 1, light, shade);
                vertex(pose, consumer, x1, y1, z0, 1, 0, light, shade);
            }
        }
    }

    private static Direction worldFace(Direction modelFace, Direction blockFacing) {
        if (modelFace.getAxis() == Direction.Axis.Y) {
            return modelFace;
        }
        int turns = switch (blockFacing) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        Direction world = modelFace;
        for (int i = 0; i < turns; i++) {
            world = world.getClockWise();
        }
        return world;
    }

    private static int faceShade(Direction face) {
        float shade = switch (face) {
            case DOWN -> 0.5F;
            case UP -> 1.0F;
            case NORTH, SOUTH -> 0.8F;
            case WEST, EAST -> 0.6F;
        };
        return Math.round(shade * 255.0F);
    }

    private static void vertex(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float x,
            float y,
            float z,
            float u,
            float v,
            int light,
            int shade
    ) {
        consumer.addVertex(pose, x, y, z)
                .setColor(shade, shade, shade, 255)
                .setUv(u, v)
                .setLight(light);
    }
}
