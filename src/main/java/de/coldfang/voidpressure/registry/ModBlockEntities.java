package de.coldfang.voidpressure.registry;

import de.coldfang.voidpressure.VoidPressure;
import de.coldfang.voidpressure.blockentity.PressureAltarBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, VoidPressure.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PressureAltarBlockEntity>> PRESSURE_ALTAR =
            BLOCK_ENTITIES.register("pressure_altar", () -> {
                @SuppressWarnings("DataFlowIssue")
                BlockEntityType<PressureAltarBlockEntity> type = BlockEntityType.Builder
                        .of(PressureAltarBlockEntity::new, ModBlocks.PRESSURE_ALTAR.get())
                        .build(null);
                return type;
            });
}
