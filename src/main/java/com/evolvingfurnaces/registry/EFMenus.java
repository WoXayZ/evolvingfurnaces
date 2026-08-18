package com.evolvingfurnaces.registry;

import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.machine.EvolvingMachineMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EFMenus {

    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, EFConstants.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<EvolvingMachineMenu>> MACHINE =
            MENUS.register("machine", () -> IMenuTypeExtension.create(EvolvingMachineMenu::new));

    private EFMenus() {
    }

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
