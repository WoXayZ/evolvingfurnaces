package com.evolvingfurnaces.client;

import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.client.gui.EvolvingBrewingStandScreen;
import com.evolvingfurnaces.client.gui.EvolvingMachineScreen;
import com.evolvingfurnaces.client.render.EvolvingBrewingStandRenderer;
import com.evolvingfurnaces.client.render.EvolvingMachineRenderer;
import com.evolvingfurnaces.client.render.EFMachineTextureCompositor;
import com.evolvingfurnaces.registry.EFBlockEntities;
import com.evolvingfurnaces.registry.EFBlocks;
import com.evolvingfurnaces.registry.EFMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = EFConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class EFClient {

    private EFClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.addListener(EFClientGui::onScreenInit);
        MinecraftForge.EVENT_BUS.addListener(EFClientGui::onScreenRender);
        MinecraftForge.EVENT_BUS.addListener(EFClientGui::onMousePressed);
        event.enqueueWork(() -> {
            MenuScreens.register(EFMenus.MACHINE.get(), EvolvingMachineScreen::new);
            MenuScreens.register(EFMenus.BREWING_STAND.get(), EvolvingBrewingStandScreen::new);
            ItemBlockRenderTypes.setRenderLayer(EFBlocks.BREWING_STAND.get(), RenderType.cutout());
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(EFBlockEntities.MACHINE.get(), EvolvingMachineRenderer::new);
        event.registerBlockEntityRenderer(EFBlockEntities.BREWING_STAND.get(), EvolvingBrewingStandRenderer::new);
    }

    @SubscribeEvent
    public static void registerReloaders(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> EFMachineTextureCompositor.clear());
    }
}
