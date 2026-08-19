package com.evolvingfurnaces.compat.jade;

import com.evolvingfurnaces.machine.EvolvingBrewingStandBlock;
import com.evolvingfurnaces.machine.EvolvingMachineBlock;
import com.evolvingfurnaces.registry.EFBlocks;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public final class EFJadePlugin implements IWailaPlugin {

    public EFJadePlugin() {
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.usePickedResult(EFBlocks.FURNACE.get());
        registration.usePickedResult(EFBlocks.BLAST_FURNACE.get());
        registration.usePickedResult(EFBlocks.SMOKER.get());
        registration.usePickedResult(EFBlocks.BREWING_STAND.get());
        registration.registerBlockComponent(EFJadeNameProvider.INSTANCE, EvolvingMachineBlock.class);
        registration.registerBlockComponent(EFJadeNameProvider.INSTANCE, EvolvingBrewingStandBlock.class);
    }
}
