package com.evolvingfurnaces.client.render;

import com.evolvingfurnaces.machine.EvolvingBrewingStandBlockEntity;
import com.evolvingfurnaces.tier.EFMachineTier;
import com.evolvingfurnaces.tier.EFTierManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class EvolvingBrewingStandRenderer implements BlockEntityRenderer<EvolvingBrewingStandBlockEntity> {

    private static final float EXPAND = 0.002F;

    public EvolvingBrewingStandRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            EvolvingBrewingStandBlockEntity machine,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        renderBase(machine.machineLevel(), poseStack, buffers, packedLight, packedOverlay);
    }

    public static void renderBase(
            int machineLevel,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        EFMachineTier tier = EFTierManager.tierClient(com.evolvingfurnaces.tier.EFMachineKind.BREWING_STAND, machineLevel).orElse(null);
        if (tier == null) {
            return;
        }
        ResourceLocation texture = EFMachineTextureCompositor.brewingBase(tier);
        int light = packedLight == 0 ? LightTexture.FULL_BRIGHT : packedLight;
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        PoseStack.Pose pose = poseStack.last();
        // Vanilla brewing_stand.json base feet, slightly expanded so the fused texture covers the vanilla base.
        box(pose, consumer, 9, 0, 5, 15, 2, 11, 9, 5, 15, 11, light, packedOverlay);
        box(pose, consumer, 1, 0, 1, 7, 2, 7, 1, 1, 7, 7, light, packedOverlay);
        box(pose, consumer, 1, 0, 9, 7, 2, 15, 1, 9, 7, 15, light, packedOverlay);
    }

    private static void box(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            int x0p,
            int y0p,
            int z0p,
            int x1p,
            int y1p,
            int z1p,
            int u0,
            int v0,
            int u1,
            int v1,
            int light,
            int overlay
    ) {
        float x0 = x0p / 16.0F - EXPAND;
        float y0 = y0p / 16.0F - EXPAND;
        float z0 = z0p / 16.0F - EXPAND;
        float x1 = x1p / 16.0F + EXPAND;
        float y1 = y1p / 16.0F + EXPAND;
        float z1 = z1p / 16.0F + EXPAND;
        float us = u0 / 16.0F;
        float vs = v0 / 16.0F;
        float ue = u1 / 16.0F;
        float ve = v1 / 16.0F;
        float sideU0 = u0 / 16.0F;
        float sideU1 = u1 / 16.0F;
        float sideV0 = 14 / 16.0F;
        float sideV1 = 1.0F;

        quad(pose, consumer, x0, y1, z0, us, vs, x1, y1, z0, ue, vs, x1, y1, z1, ue, ve, x0, y1, z1, us, ve, light, overlay, 0, 1, 0);
        quad(pose, consumer, x0, y0, z1, us, vs, x1, y0, z1, ue, vs, x1, y0, z0, ue, ve, x0, y0, z0, us, ve, light, overlay, 0, -1, 0);
        quad(pose, consumer, x0, y1, z0, sideU0, sideV0, x1, y1, z0, sideU1, sideV0, x1, y0, z0, sideU1, sideV1, x0, y0, z0, sideU0, sideV1, light, overlay, 0, 0, -1);
        quad(pose, consumer, x1, y1, z1, sideU0, sideV0, x0, y1, z1, sideU1, sideV0, x0, y0, z1, sideU1, sideV1, x1, y0, z1, sideU0, sideV1, light, overlay, 0, 0, 1);
        quad(pose, consumer, x0, y1, z1, sideU0, sideV0, x0, y1, z0, sideU1, sideV0, x0, y0, z0, sideU1, sideV1, x0, y0, z1, sideU0, sideV1, light, overlay, -1, 0, 0);
        quad(pose, consumer, x1, y1, z0, sideU0, sideV0, x1, y1, z1, sideU1, sideV0, x1, y0, z1, sideU1, sideV1, x1, y0, z0, sideU0, sideV1, light, overlay, 1, 0, 0);
    }

    private static void quad(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float x1, float y1, float z1, float u1, float v1,
            float x2, float y2, float z2, float u2, float v2,
            float x3, float y3, float z3, float u3, float v3,
            float x4, float y4, float z4, float u4, float v4,
            int light,
            int overlay,
            float nx,
            float ny,
            float nz
    ) {
        vertex(pose, consumer, x1, y1, z1, u1, v1, light, overlay, nx, ny, nz);
        vertex(pose, consumer, x2, y2, z2, u2, v2, light, overlay, nx, ny, nz);
        vertex(pose, consumer, x3, y3, z3, u3, v3, light, overlay, nx, ny, nz);
        vertex(pose, consumer, x4, y4, z4, u4, v4, light, overlay, nx, ny, nz);
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
