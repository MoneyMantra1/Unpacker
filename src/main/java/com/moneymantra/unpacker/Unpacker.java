package com.moneymantra.unpacker;

import com.moneymantra.unpacker.registry.ModBlockEntities;
import com.moneymantra.unpacker.registry.ModBlocks;
import com.moneymantra.unpacker.registry.ModMenuTypes;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(Unpacker.MOD_ID)
public class Unpacker {
    public static final String MOD_ID = "unpacker";

    public Unpacker(IEventBus modEventBus) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);

        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(this::addCreativeTabContents);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.UNPACKER.get(),
                (blockEntity, side) -> blockEntity.getItemHandler(side)
        );
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if(event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModBlocks.UNPACKER_ITEM.get());
        }
    }
}
