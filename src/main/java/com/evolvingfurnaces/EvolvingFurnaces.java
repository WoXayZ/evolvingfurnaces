package com.evolvingfurnaces;

import com.evolvingfurnaces.network.EFNetwork;
import com.evolvingfurnaces.registry.EFBlockEntities;
import com.evolvingfurnaces.registry.EFBlocks;
import com.evolvingfurnaces.registry.EFCreativeTabs;
import com.evolvingfurnaces.registry.EFItems;
import com.evolvingfurnaces.registry.EFMenus;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(EFConstants.MOD_ID)
public final class EvolvingFurnaces {

    public static final Logger LOGGER = LogUtils.getLogger();

    public EvolvingFurnaces() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, EFConfig.SPEC);
        EFBlocks.register(bus);
        EFItems.register(bus);
        EFBlockEntities.register(bus);
        EFMenus.register(bus);
        EFCreativeTabs.register(bus);
        EFNetwork.register();
        LOGGER.info("Evolving Furnaces initialised.");
    }
}
