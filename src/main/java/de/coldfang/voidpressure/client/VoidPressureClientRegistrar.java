package de.coldfang.voidpressure.client;

import de.coldfang.voidpressure.client.render.PressureAltarBlockEntityRenderer;
import de.coldfang.voidpressure.registry.ModBlockEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

public final class VoidPressureClientRegistrar {

    private VoidPressureClientRegistrar() {}

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        VoidPressureKeybinds.register(event);
    }

    public static void onRegisterBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.PRESSURE_ALTAR.get(), PressureAltarBlockEntityRenderer::new);
    }

}
