package com.evolvingfurnaces.client.render;

import com.evolvingfurnaces.machine.EvolvingMachineItem;
import com.evolvingfurnaces.registry.EFBlocks;
import com.evolvingfurnaces.tier.EFMachineKind;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.state.BlockState;

public class EvolvingMachineItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static EvolvingMachineItemRenderer instance;

    public static EvolvingMachineItemRenderer instance() {
        if (instance == null) {
            instance = new EvolvingMachineItemRenderer();
        }
        return instance;
    }

    public EvolvingMachineItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(
            ItemStack stack,
            ItemDisplayContext context,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        if (!(stack.getItem() instanceof EvolvingMachineItem item)) {
            return;
        }
        EFMachineKind kind = item.kind();
        BlockState state = EFBlocks.block(kind).defaultBlockState().setValue(AbstractFurnaceBlock.FACING, Direction.NORTH);
        int light = packedLight == 0 ? LightTexture.FULL_BRIGHT : packedLight;
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, poseStack, buffers, light, packedOverlay);
        EvolvingMachineRenderer.renderOverlay(kind, EvolvingMachineItem.levelOf(stack), state, poseStack, buffers, light, packedOverlay);
    }
}
