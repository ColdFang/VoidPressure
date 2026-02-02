package de.coldfang.voidpressure.client;

import de.coldfang.voidpressure.VoidPressure;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = VoidPressure.MODID, value = Dist.CLIENT)
@SuppressWarnings("unused")
public class VoidPressureClientEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // "consumeClick" sorgt dafür, dass es nur 1x pro Tastendruck auslöst
        while (VoidPressureKeybinds.OPEN_MENU != null && VoidPressureKeybinds.OPEN_MENU.consumeClick()) {

            // Permission check: OP / Cheats / Commands
            if (!mc.player.hasPermissions(2)) {
                mc.player.displayClientMessage(
                        Component.literal("You need command permissions to open Void Pressure.")
                                .withStyle(ChatFormatting.RED),
                        false
                );
                return;
            }

            mc.setScreen(new VoidPressureScreen());
        }
    }
}
