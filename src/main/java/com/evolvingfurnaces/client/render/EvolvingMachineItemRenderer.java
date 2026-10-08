package com.evolvingfurnaces.client.render;

import com.evolvingfurnaces.machine.EvolvingMachineItem;
import com.evolvingfurnaces.registry.EFBlocks;
import com.evolvingfurnaces.tier.EFMachineKind;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Brightness;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class EvolvingMachineItemRenderer implements SpecialModelRenderer<ItemStack> {

    private static final Vector3fc MIN = new Vector3f(0.0F, 0.0F, 0.0F);
    private static final Vector3fc MAX = new Vector3f(1.0F, 1.0F, 1.0F);

    @Override
    public ItemStack extractArgument(ItemStack stack) {
        return stack;
    }

    @Override
    public void submit(
            ItemStack stack,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int packedLight,
            int packedOverlay,
            boolean foil,
            int outlineColor
    ) {
        if (!(stack.getItem() instanceof EvolvingMachineItem item)) {
            return;
        }
        EFMachineKind kind = item.kind();
        BlockState state = kind == EFMachineKind.BREWING_STAND
                ? EFBlocks.block(kind).defaultBlockState()
                : EFBlocks.block(kind).defaultBlockState().setValue(AbstractFurnaceBlock.FACING, Direction.NORTH);
        BlockModelRenderState model = new BlockModelRenderState();
        Minecraft.getInstance().getBlockModelResolver().update(model, state, BlockDisplayContext.create());
        int overlay = packedOverlay == 0 ? OverlayTexture.NO_OVERLAY : packedOverlay;
        int light = packedLight == 0 ? Brightness.FULL_BRIGHT.pack() : packedLight;
        model.submit(poseStack, collector, light, overlay, outlineColor);
        if (kind == EFMachineKind.BREWING_STAND) {
            EvolvingBrewingStandRenderer.renderBase(EvolvingMachineItem.levelOf(stack), poseStack, collector, light, overlay);
            return;
        }
        EvolvingMachineRenderer.renderOverlay(
                kind,
                EvolvingMachineItem.levelOf(stack),
                state,
                EvolvingMachineItem.moduleOf(stack),
                poseStack,
                collector,
                light,
                overlay);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(MIN);
        output.accept(MAX);
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<ItemStack> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<ItemStack> bake(SpecialModelRenderer.BakingContext context) {
            return new EvolvingMachineItemRenderer();
        }
    }
}
