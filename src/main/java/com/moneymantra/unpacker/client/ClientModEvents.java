package com.moneymantra.unpacker.client;

import com.moneymantra.unpacker.Unpacker;
import com.moneymantra.unpacker.registry.ModMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Unpacker.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.UNPACKER_MENU.get(), UnpackerScreen::new);
        event.register(ModMenuTypes.PACKER_MENU.get(), PackerScreen::new);
    }
}
