package de.coldfang.voidpressure;

import com.mojang.logging.LogUtils;
import de.coldfang.voidpressure.config.ClientConfig;
import de.coldfang.voidpressure.config.CommonConfig;
import de.coldfang.voidpressure.registry.ModBlockEntities;
import de.coldfang.voidpressure.registry.ModBlocks;
import de.coldfang.voidpressure.registry.ModCreativeTabs;
import de.coldfang.voidpressure.registry.ModItems;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(VoidPressure.MODID)
public class VoidPressure {

    public static final String MODID = "voidpressure";
    public static final Logger LOGGER = LogUtils.getLogger();

    public VoidPressure(IEventBus modEventBus, ModContainer modContainer) {

        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);


        modContainer.registerConfig(
                ModConfig.Type.CLIENT,
                ClientConfig.SPEC,
                "voidpressure-client.toml"
        );

        modContainer.registerConfig(
                ModConfig.Type.COMMON,
                CommonConfig.SPEC,
                "voidpressure-common.toml"
        );

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(de.coldfang.voidpressure.client.VoidPressureClientRegistrar::onRegisterKeyMappings);
            modEventBus.addListener(de.coldfang.voidpressure.client.VoidPressureClientRegistrar::onRegisterBlockEntityRenderers);
        }


        LOGGER.info("VoidPressure loaded");
    }
}
