package com.evolvingfurnaces.registry;

import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.machine.EvolvingMachineItem;
import com.evolvingfurnaces.tier.EFMachineKind;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EFItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EFConstants.MOD_ID);

    public static final DeferredItem<EvolvingMachineItem> FURNACE = ITEMS.registerItem(
            "furnace",
            props -> new EvolvingMachineItem(EFBlocks.FURNACE.get(), props, EFMachineKind.FURNACE));
    public static final DeferredItem<EvolvingMachineItem> BLAST_FURNACE = ITEMS.registerItem(
            "blast_furnace",
            props -> new EvolvingMachineItem(EFBlocks.BLAST_FURNACE.get(), props, EFMachineKind.BLAST_FURNACE));
    public static final DeferredItem<EvolvingMachineItem> SMOKER = ITEMS.registerItem(
            "smoker",
            props -> new EvolvingMachineItem(EFBlocks.SMOKER.get(), props, EFMachineKind.SMOKER));
    public static final DeferredItem<EvolvingMachineItem> BREWING_STAND = ITEMS.registerItem(
            "brewing_stand",
            props -> new EvolvingMachineItem(EFBlocks.BREWING_STAND.get(), props, EFMachineKind.BREWING_STAND));

    private EFItems() {
    }

    public static EvolvingMachineItem item(EFMachineKind kind) {
        return switch (kind) {
            case FURNACE -> FURNACE.get();
            case BLAST_FURNACE -> BLAST_FURNACE.get();
            case SMOKER -> SMOKER.get();
            case BREWING_STAND -> BREWING_STAND.get();
        };
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
