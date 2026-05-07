package com.moneymantra.unpacker.registry;

import com.moneymantra.unpacker.Unpacker;
import com.moneymantra.unpacker.menu.UnpackerMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, Unpacker.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<UnpackerMenu>> UNPACKER_MENU = MENU_TYPES.register(
            "unpacker",
            () -> IMenuTypeExtension.create(UnpackerMenu::new)
    );
}
