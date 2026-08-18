package com.evolvingfurnaces;

import com.evolvingfurnaces.network.EFNetwork;
import com.evolvingfurnaces.registry.EFBlockEntities;
import com.evolvingfurnaces.registry.EFBlocks;
import com.evolvingfurnaces.registry.EFCreativeTabs;
import com.evolvingfurnaces.registry.EFDataComponents;
import com.evolvingfurnaces.registry.EFItems;
import com.evolvingfurnaces.registry.EFMenus;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(EFConstants.MOD_ID)
public final class EvolvingFurnaces {

    public static final Logger LOGGER = LogUtils.getLogger();

    public EvolvingFurnaces(IEventBus modEventBus, ModContainer modContainer) {
        EFDataComponents.register(modEventBus);
        EFBlocks.register(modEventBus);
        EFItems.register(modEventBus);
        EFBlockEntities.register(modEventBus);
        EFMenus.register(modEventBus);
        EFCreativeTabs.register(modEventBus);
        modEventBus.addListener(EFNetwork::register);
        LOGGER.info("Evolving Furnaces initialised.");
    }
}
