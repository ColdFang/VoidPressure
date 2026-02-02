package de.coldfang.voidpressure.block;

import com.mojang.serialization.MapCodec;
import de.coldfang.voidpressure.blockentity.PressureAltarBlockEntity;
import de.coldfang.voidpressure.config.CommonConfig;
import de.coldfang.voidpressure.data.VoidPressureSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PressureAltarBlock extends BaseEntityBlock {

    public static final MapCodec<PressureAltarBlock> CODEC = simpleCodec(PressureAltarBlock::new);

    public PressureAltarBlock(Properties properties) {
        super(properties);
    }

    // BaseEntityBlock plumbing

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new PressureAltarBlockEntity(pos, state);
    }

    @Override
    public void onRemove(
            @NotNull BlockState state,
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull BlockState newState,
            boolean isMoving
    ) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PressureAltarBlockEntity altar) {
                if (!level.isClientSide) {
                    ItemStack offered = altar.removeOffered();
                    if (!offered.isEmpty()) {
                        Block.popResource(level, pos, offered);
                    }
                }
            }
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    // Interaction

    @Override
    public @NotNull InteractionResult useWithoutItem(
            @NotNull BlockState state,
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull Player player,
            @NotNull BlockHitResult hit
    ) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof PressureAltarBlockEntity altar)) return InteractionResult.PASS;

        if (altar.hasOffered()) {
            giveBackOffered(player, altar);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    @SuppressWarnings("SameReturnValue")
    protected @NotNull ItemInteractionResult useItemOn(
            @NotNull ItemStack stack,
            @NotNull BlockState state,
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull Player player,
            @NotNull InteractionHand hand,
            @NotNull BlockHitResult hit
    ) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof PressureAltarBlockEntity altar)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // Ritual: start tool + offered present
        if (altar.hasOffered() && isStartTool(stack)) {
            return tryRitual(level, pos, player, hand, altar);
        }

        // If an item is already present, always return it
        if (altar.hasOffered()) {
            giveBackOffered(player, altar);
            return ItemInteractionResult.CONSUME;
        }

        // If empty, only allow configured offerings
        if (!stack.isEmpty()) {
            long reduceMilli = getOfferingReduceMilli(stack);
            if (reduceMilli <= 0) {
                player.displayClientMessage(Component.literal("The Void rejects this offering."), true);
                return ItemInteractionResult.CONSUME;
            }

            boolean ok = altar.tryPlaceOne(stack);
            if (ok) {
                stack.shrink(1);
            }
            return ItemInteractionResult.CONSUME;
        }

        return ItemInteractionResult.CONSUME;
    }

    // Start tool

    private static boolean isStartTool(@NotNull ItemStack held) {
        String id = CommonConfig.ALTAR_START_ITEM.get();
        if (id.isBlank()) {
            return held.is(net.minecraft.world.item.Items.FLINT_AND_STEEL);
        }

        ResourceLocation rl = ResourceLocation.tryParse(id.trim());
        if (rl == null) {
            return held.is(net.minecraft.world.item.Items.FLINT_AND_STEEL);
        }

        if (!BuiltInRegistries.ITEM.containsKey(rl)) {
            return held.is(net.minecraft.world.item.Items.FLINT_AND_STEEL);
        }

        Item configured = BuiltInRegistries.ITEM.get(rl);
        return held.is(configured);
    }

    // Ritual

    @SuppressWarnings("SameReturnValue")
    private static @NotNull ItemInteractionResult tryRitual(
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull Player player,
            @NotNull InteractionHand hand,
            @NotNull PressureAltarBlockEntity altar
    ) {
        if (!(level instanceof ServerLevel sl)) {
            return ItemInteractionResult.CONSUME;
        }

        VoidPressureSavedData data = VoidPressureSavedData.get(sl);

        ItemStack offered = altar.getOffered();
        if (offered.isEmpty()) {
            return ItemInteractionResult.CONSUME;
        }

        long reduceMilli = getOfferingReduceMilli(offered);
        if (reduceMilli <= 0) {
            player.displayClientMessage(Component.literal("The Void rejects this offering."), true);
            giveBackOffered(player, altar);
            return ItemInteractionResult.CONSUME;
        }

        int cooldownSeconds = CommonConfig.ALTAR_COOLDOWN_SECONDS.get();
        if (cooldownSeconds > 0) {
            long now = level.getGameTime();
            long last = data.getLastAltarRitualGameTime();
            long cooldownTicks = cooldownSeconds * 20L;

            if (last >= 0 && (now - last) < cooldownTicks) {
                long remainingTicks = cooldownTicks - (now - last);
                long remainingSeconds = (remainingTicks + 19) / 20;

                player.displayClientMessage(
                        Component.literal("The altar is dormant. The Void will answer in " + remainingSeconds + "s."),
                        true
                );
                return ItemInteractionResult.CONSUME;
            }
        }

        boolean costsXp = CommonConfig.ALTAR_COSTS_XP.get();
        int xpLevels = Math.max(0, CommonConfig.ALTAR_XP_LEVELS_PER_RITUAL.get());

        if (costsXp && xpLevels > 0) {
            if (player.experienceLevel < xpLevels) {
                player.displayClientMessage(Component.literal("The Void demands experience"), true);
                return ItemInteractionResult.CONSUME;
            }
            player.giveExperienceLevels(-xpLevels);
        }

        data.removePressureMilli(sl, reduceMilli);

        altar.removeOffered();

        data.setLastAltarRitualGameTime(level.getGameTime());

        ItemStack tool = player.getItemInHand(hand);
        EquipmentSlot slot = (hand == InteractionHand.MAIN_HAND) ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        tool.hurtAndBreak(1, player, slot);

        sl.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8f, 1.0f);
        sl.sendParticles(
                ParticleTypes.SMOKE,
                pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                14,
                0.15, 0.15, 0.15,
                0.0
        );
        sl.sendParticles(
                ParticleTypes.FLAME,
                pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5,
                8,
                0.10, 0.05, 0.10,
                0.0
        );

        return ItemInteractionResult.CONSUME;
    }

    // Offerings config helpers

    private static long getOfferingReduceMilli(@NotNull ItemStack stack) {
        if (stack.isEmpty()) return 0L;

        Map<Item, Long> offerings = buildOfferingsMap(CommonConfig.ALTAR_OFFERINGS.get());
        Long milli = offerings.get(stack.getItem());
        return (milli == null) ? 0L : milli;
    }

    private static @NotNull Map<Item, Long> buildOfferingsMap(@NotNull List<? extends String> raw) {
        Map<Item, Long> map = new HashMap<>();
        if (raw.isEmpty()) return map;

        for (String line : raw) {
            if (line == null) continue;

            String s = line.trim();
            if (s.isEmpty()) continue;

            String[] parts = s.split("\\|", 2);
            if (parts.length != 2) continue;

            String itemId = parts[0].trim();
            String pressureStr = parts[1].trim();

            ResourceLocation rl = ResourceLocation.tryParse(itemId);
            if (rl == null) continue;
            if (!BuiltInRegistries.ITEM.containsKey(rl)) continue;

            double p;
            try {
                p = Double.parseDouble(pressureStr);
            } catch (Exception ignored) {
                continue;
            }

            if (p <= 0.0) continue;

            long milli = Math.round(p * 1000.0);
            if (milli <= 0) continue;

            map.put(BuiltInRegistries.ITEM.get(rl), milli);
        }

        return map;
    }

    // Inventory helpers

    private static void giveBackOffered(@NotNull Player player, @NotNull PressureAltarBlockEntity altar) {
        ItemStack offered = altar.removeOffered();
        if (offered.isEmpty()) return;

        boolean added = player.getInventory().add(offered);
        if (!added) {
            player.drop(offered, false);
        }
    }
}
