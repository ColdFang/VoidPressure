package de.coldfang.voidpressure.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

public final class VoidPressureKeybinds {

    public static final String CATEGORY = "key.categories.voidpressure";

    public static KeyMapping OPEN_MENU;

    private VoidPressureKeybinds() {}

    public static void register(RegisterKeyMappingsEvent event) {
        OPEN_MENU = new KeyMapping(
                "key.voidpressure.open_menu",
                KeyConflictContext.IN_GAME,
                KeyModifier.SHIFT,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                CATEGORY
        );

        event.register(OPEN_MENU);
    }
}
