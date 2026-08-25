package com.evolvingfurnaces.client;

import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.client.gui.EvolvingBrewingStandScreen;
import com.evolvingfurnaces.client.gui.EvolvingMachineScreen;
import com.evolvingfurnaces.client.render.EvolvingBrewingStandRenderer;
import com.evolvingfurnaces.client.render.EvolvingMachineItemRenderer;
import com.evolvingfurnaces.client.render.EvolvingMachineRenderer;
import com.evolvingfurnaces.client.render.EFMachineTextureCompositor;
import com.evolvingfurnaces.registry.EFBlockEntities;
import com.evolvingfurnaces.registry.EFBlocks;
import com.evolvingfurnaces.registry.EFItems;
import com.evolvingfurnaces.registry.EFMenus;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

@EventBusSubscriber(modid = EFConstants.MOD_ID, value = Dist.CLIENT)
public final class EFClient {

    private EFClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        NeoForge.EVENT_BUS.addListener(EFClientGui::onScreenInit);
        NeoForge.EVENT_BUS.addListener(EFClientGui::onScreenRender);
        NeoForge.EVENT_BUS.addListener(EFClientGui::onMousePressed);
        event.enqueueWork(() -> ItemBlockRenderTypes.setRenderLayer(EFBlocks.BREWING_STAND.get(), RenderType.cutout()));
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(EFMenus.MACHINE.get(), EvolvingMachineScreen::new);
        event.register(EFMenus.BREWING_STAND.get(), EvolvingBrewingStandScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(EFBlockEntities.MACHINE.get(), EvolvingMachineRenderer::new);
        event.registerBlockEntityRenderer(EFBlockEntities.BREWING_STAND.get(), EvolvingBrewingStandRenderer::new);
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        IClientItemExtensions extensions = new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return EvolvingMachineItemRenderer.instance();
            }
        };
        event.registerItem(extensions, EFItems.FURNACE.get(), EFItems.BLAST_FURNACE.get(), EFItems.SMOKER.get(), EFItems.BREWING_STAND.get());
    }

    @SubscribeEvent
    public static void registerReloaders(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> EFMachineTextureCompositor.clear());
    }
}
