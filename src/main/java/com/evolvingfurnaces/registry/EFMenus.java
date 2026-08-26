package com.evolvingfurnaces.registry;

import com.evolvingfurnaces.EFConstants;
import com.evolvingfurnaces.machine.EvolvingBrewingStandMenu;
import com.evolvingfurnaces.machine.EvolvingMachineMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class EFMenus {

    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, EFConstants.MOD_ID);

    public static final RegistryObject<MenuType<EvolvingMachineMenu>> MACHINE =
            MENUS.register("machine", () -> IForgeMenuType.create(EvolvingMachineMenu::new));
    public static final RegistryObject<MenuType<EvolvingBrewingStandMenu>> BREWING_STAND =
            MENUS.register("brewing_stand", () -> IForgeMenuType.create(EvolvingBrewingStandMenu::new));

    private EFMenus() {
    }

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
