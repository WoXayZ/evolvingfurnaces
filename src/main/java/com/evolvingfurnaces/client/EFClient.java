package com.evolvingfurnaces.client;

import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.client.gui.EvolvingBrewingStandScreen;
import com.evolvingfurnaces.client.gui.EvolvingMachineScreen;
import com.evolvingfurnaces.client.render.EFMachineTextureCompositor;
import com.evolvingfurnaces.client.render.EvolvingBrewingStandRenderer;
import com.evolvingfurnaces.client.render.EvolvingMachineItemRenderer;
import com.evolvingfurnaces.client.render.EvolvingMachineRenderer;
import com.evolvingfurnaces.registry.EFBlockEntities;
import com.evolvingfurnaces.registry.EFMenus;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.neoforge.common.NeoForge;

@EventBusSubscriber(modid = EFConstants.MOD_ID, value = Dist.CLIENT)
public final class EFClient {

    private EFClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        NeoForge.EVENT_BUS.addListener(EFClientGui::onScreenInit);
        NeoForge.EVENT_BUS.addListener(EFClientGui::onScreenRender);
        NeoForge.EVENT_BUS.addListener(EFClientGui::onMousePressed);
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
    public static void registerSpecialModels(RegisterSpecialModelRendererEvent event) {
        event.register(EFConstants.id("machine"), EvolvingMachineItemRenderer.Unbaked.MAP_CODEC);
    }

    @SubscribeEvent
    public static void registerReloaders(AddClientReloadListenersEvent event) {
        event.addListener(EFConstants.id("machine_textures"), (ResourceManagerReloadListener) manager -> EFMachineTextureCompositor.clear());
    }
}
