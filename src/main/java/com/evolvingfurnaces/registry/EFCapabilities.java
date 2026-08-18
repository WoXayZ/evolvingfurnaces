package com.evolvingfurnaces.registry;

import com.evolvingfurnaces.EFConstants;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = EFConstants.MOD_ID)
public final class EFCapabilities {

    private EFCapabilities() {
    }

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                EFBlockEntities.MACHINE.get(),
                (machine, side) -> machine.handler(side));
    }
}
