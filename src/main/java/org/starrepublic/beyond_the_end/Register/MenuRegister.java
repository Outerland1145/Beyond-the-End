package org.starrepublic.beyond_the_end.Register;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;

import org.starrepublic.beyond_the_end.Beyond_the_end;
import org.starrepublic.beyond_the_end.Items.charm.CharmPouchMenu;

public final class MenuRegister {

    public static final DeferredRegister<MenuType<?>>
            MENUS = DeferredRegister.create(
            Registries.MENU,
            Beyond_the_end.MODID
    );

    public static final DeferredHolder<
            MenuType<?>,
            MenuType<CharmPouchMenu>
            > CHARM_POUCH =
            MENUS.register(
                    "charm_pouch",
                    () -> new MenuType<>(
                            CharmPouchMenu::new,
                            FeatureFlags.DEFAULT_FLAGS
                    )
            );

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }

    private MenuRegister() {
    }
}
