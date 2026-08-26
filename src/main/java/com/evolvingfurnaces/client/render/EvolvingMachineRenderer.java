package com.evolvingfurnaces.client.render;

import com.evolvingfurnaces.machine.EFEnergyModule;
import com.evolvingfurnaces.machine.EvolvingMachineBlockEntity;
import com.evolvingfurnaces.tier.EFMachineKind;
import com.evolvingfurnaces.tier.EFMachineTier;
import com.evolvingfurnaces.tier.EFTierManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.state.BlockState;

public class EvolvingMachineRenderer implements BlockEntityRenderer<EvolvingMachineBlockEntity> {

    private static final float EXPAND = 0.002F;

    public EvolvingMachineRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            EvolvingMachineBlockEntity machine,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        renderOverlay(
                machine.kind(),
                machine.machineLevel(),
                machine.getBlockState(),
                machine.energyModule(),
                poseStack,
                buffers,
                packedLight,
                packedOverlay);
    }

    public static void renderOverlay(
            EFMachineKind kind,
            int machineLevel,
            BlockState state,
            EFEnergyModule module,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        EFMachineTier tier = EFTierManager.tierClient(kind, machineLevel).orElse(null);
        if (tier == null) {
            return;
        }
        boolean lit = state.hasProperty(AbstractFurnaceBlock.LIT) && state.getValue(AbstractFurnaceBlock.LIT);
        Direction facing = state.hasProperty(AbstractFurnaceBlock.FACING) ? state.getValue(AbstractFurnaceBlock.FACING) : Direction.NORTH;
        int light = packedLight == 0 ? LightTexture.FULL_BRIGHT : packedLight;
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
        PoseStack.Pose pose = poseStack.last();
        float x0 = -EXPAND;
        float y0 = -EXPAND;
        float z0 = -EXPAND;
        float x1 = 1F + EXPAND;
        float y1 = 1F + EXPAND;
        float z1 = 1F + EXPAND;
        for (Direction face : Direction.values()) {
            ResourceLocation texture = EFMachineTextureCompositor.texture(kind, tier, face, lit, module);
            VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
            renderFace(pose, consumer, face, x0, y0, z0, x1, y1, z1, light, packedOverlay);
        }
        poseStack.popPose();
    }

    private static void renderFace(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            Direction face,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            int light,
            int overlay
    ) {
        switch (face) {
            case DOWN -> {
                vertex(pose, consumer, x0, y0, z1, 0, 0, light, overlay, 0, -1, 0);
                vertex(pose, consumer, x1, y0, z1, 1, 0, light, overlay, 0, -1, 0);
                vertex(pose, consumer, x1, y0, z0, 1, 1, light, overlay, 0, -1, 0);
                vertex(pose, consumer, x0, y0, z0, 0, 1, light, overlay, 0, -1, 0);
            }
            case UP -> {
                vertex(pose, consumer, x0, y1, z0, 0, 0, light, overlay, 0, 1, 0);
                vertex(pose, consumer, x1, y1, z0, 1, 0, light, overlay, 0, 1, 0);
                vertex(pose, consumer, x1, y1, z1, 1, 1, light, overlay, 0, 1, 0);
                vertex(pose, consumer, x0, y1, z1, 0, 1, light, overlay, 0, 1, 0);
            }
            case NORTH -> {
                vertex(pose, consumer, x0, y1, z0, 0, 0, light, overlay, 0, 0, -1);
                vertex(pose, consumer, x1, y1, z0, 1, 0, light, overlay, 0, 0, -1);
                vertex(pose, consumer, x1, y0, z0, 1, 1, light, overlay, 0, 0, -1);
                vertex(pose, consumer, x0, y0, z0, 0, 1, light, overlay, 0, 0, -1);
            }
            case SOUTH -> {
                vertex(pose, consumer, x1, y1, z1, 0, 0, light, overlay, 0, 0, 1);
                vertex(pose, consumer, x0, y1, z1, 1, 0, light, overlay, 0, 0, 1);
                vertex(pose, consumer, x0, y0, z1, 1, 1, light, overlay, 0, 0, 1);
                vertex(pose, consumer, x1, y0, z1, 0, 1, light, overlay, 0, 0, 1);
            }
            case WEST -> {
                vertex(pose, consumer, x0, y1, z0, 0, 0, light, overlay, -1, 0, 0);
                vertex(pose, consumer, x0, y1, z1, 1, 0, light, overlay, -1, 0, 0);
                vertex(pose, consumer, x0, y0, z1, 1, 1, light, overlay, -1, 0, 0);
                vertex(pose, consumer, x0, y0, z0, 0, 1, light, overlay, -1, 0, 0);
            }
            case EAST -> {
                vertex(pose, consumer, x1, y1, z1, 0, 0, light, overlay, 1, 0, 0);
                vertex(pose, consumer, x1, y1, z0, 1, 0, light, overlay, 1, 0, 0);
                vertex(pose, consumer, x1, y0, z0, 1, 1, light, overlay, 1, 0, 0);
                vertex(pose, consumer, x1, y0, z1, 0, 1, light, overlay, 1, 0, 0);
            }
        }
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
            int overlay,
            float nx,
            float ny,
            float nz
    ) {
        consumer.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
