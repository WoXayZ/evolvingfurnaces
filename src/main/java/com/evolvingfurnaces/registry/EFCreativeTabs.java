package com.evolvingfurnaces.registry;

import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.EFConfig;
import com.evolvingfurnaces.machine.EvolvingMachineItem;
import com.evolvingfurnaces.tier.EFMachineKind;
import com.evolvingfurnaces.tier.EFMachineTier;
import com.evolvingfurnaces.tier.EFTierManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EFCreativeTabs {

    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EFConstants.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.evolvingfurnaces"))
                    .icon(() -> EvolvingMachineItem.withLevel(EFItems.FURNACE.get(), 1))
                    .displayItems((params, output) -> {
                        for (EFMachineKind kind : EFMachineKind.values()) {
                            if (!EFConfig.enabled(kind)) {
                                continue;
                            }
                            for (EFMachineTier tier : EFTierManager.listClient(kind).levels()) {
                                output.accept(EvolvingMachineItem.withLevel(EFItems.item(kind), tier.level()));
                            }
                        }
                    })
                    .build());

    private EFCreativeTabs() {
    }

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
