package com.factoryworks.core;

import com.factoryworks.core.fluid.BoilerMenu;
import com.factoryworks.core.machine.AssemblingMachineMenu;
import com.factoryworks.core.mining.rig.RigMenu;
import com.factoryworks.core.smelting.FurnaceMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public final class PFMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, FactoryWorksCore.NAMESPACE);

    /**
     * One menu for all three furnace tiers (#155), not one per tier: they differ in whether there
     * is a fuel slot and whether there is an energy bar, and the tier travels in the opening
     * packet so the client can tell.
     */
    public static final Supplier<MenuType<FurnaceMenu>> FURNACE =
            MENUS.register("furnace", () -> IMenuTypeExtension.create(FurnaceMenu::new));

    /**
     * One menu for both mining rigs (#193, #194), for the reason the furnace's is one: they differ
     * in whether there is a fuel slot, and the tier travels in the opening packet so the client can
     * tell without a block entity to ask.
     */
    public static final Supplier<MenuType<RigMenu>> RIG =
            MENUS.register("rig", () -> IMenuTypeExtension.create(RigMenu::new));

    /**
     * The Boiler's menu (#224). One, not a ladder: ADR-0048 authors one boiler tier.
     *
     * <p>No opening data -- there is nothing about a Boiler the client cannot read off the
     * container data, which is why this one is a plain {@code MenuType}.
     */
    public static final Supplier<MenuType<BoilerMenu>> BOILER =
            MENUS.register("boiler", () -> new MenuType<>(BoilerMenu::new,
                    net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));

    /**
     * The Assembling Machine's: the pack's own, since Oritech's screen has no hook for the Held
     * recipe (ADR-0073). The opening packet carries the recipe list, which the client has no
     * recipe manager to read.
     */
    public static final Supplier<MenuType<AssemblingMachineMenu>> ASSEMBLING_MACHINE =
            MENUS.register("assembling_machine", () -> IMenuTypeExtension.create(AssemblingMachineMenu::new));

    private PFMenus() {
    }

    static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
