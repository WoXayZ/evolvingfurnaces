package com.evolvingfurnaces.registry;

import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.machine.EvolvingMachineItem;
import com.evolvingfurnaces.tier.EFMachineKind;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class EFItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, EFConstants.MOD_ID);

    public static final RegistryObject<EvolvingMachineItem> FURNACE = ITEMS.register(
            "furnace",
            () -> new EvolvingMachineItem(EFBlocks.FURNACE.get(), new Item.Properties(), EFMachineKind.FURNACE));
    public static final RegistryObject<EvolvingMachineItem> BLAST_FURNACE = ITEMS.register(
            "blast_furnace",
            () -> new EvolvingMachineItem(EFBlocks.BLAST_FURNACE.get(), new Item.Properties(), EFMachineKind.BLAST_FURNACE));
    public static final RegistryObject<EvolvingMachineItem> SMOKER = ITEMS.register(
            "smoker",
            () -> new EvolvingMachineItem(EFBlocks.SMOKER.get(), new Item.Properties(), EFMachineKind.SMOKER));
    public static final RegistryObject<EvolvingMachineItem> BREWING_STAND = ITEMS.register(
            "brewing_stand",
            () -> new EvolvingMachineItem(EFBlocks.BREWING_STAND.get(), new Item.Properties(), EFMachineKind.BREWING_STAND));

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
