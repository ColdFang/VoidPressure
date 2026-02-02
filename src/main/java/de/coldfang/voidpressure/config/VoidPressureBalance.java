package de.coldfang.voidpressure.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class VoidPressureBalance {

    private VoidPressureBalance() {
    }

    private static long toMilli(double value) {
        return Math.round(value * 1000.0);
    }

    private static volatile int overridesHash = 0;
    private static volatile Map<ResourceLocation, Long> playerKillOverridesMilli = Collections.emptyMap();

    private static int computeOverridesHash(List<? extends String> lines) {
        int h = 1;
        for (String s : lines) {
            h = 31 * h + (s == null ? 0 : s.hashCode());
        }
        return h;
    }

    private static void ensureOverridesUpToDate() {
        List<? extends String> lines = CommonConfig.GAIN_PLAYER_KILL_OVERRIDES.get();
        int h = computeOverridesHash(lines);
        if (h == overridesHash) return;

        Map<ResourceLocation, Long> map = new HashMap<>();

        for (String raw : lines) {
            if (raw == null) continue;
            String line = raw.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("#") || line.startsWith("//")) continue;

            int eq = line.indexOf('=');
            if (eq < 0) continue;

            String left = line.substring(0, eq).trim();
            String right = line.substring(eq + 1).trim();

            if (left.isEmpty() || right.isEmpty()) continue;

            ResourceLocation id;
            try {
                id = ResourceLocation.parse(left);
            } catch (Exception ignored) {
                continue;
            }

            double val;
            try {
                val = Double.parseDouble(right);
            } catch (Exception ignored) {
                continue;
            }

            if (val < 0.0) val = 0.0;

            map.put(id, toMilli(val));
        }

        playerKillOverridesMilli = Collections.unmodifiableMap(map);
        overridesHash = h;
    }

    public static long playerKillGainMilli() {
        return toMilli(CommonConfig.GAIN_PLAYER_KILL.get());
    }

    public static long playerKillGainMilli(@Nullable EntityType<?> type) {
        if (type == null) return playerKillGainMilli();

        ensureOverridesUpToDate();

        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        Long override = playerKillOverridesMilli.get(key);
        return (override != null) ? override : playerKillGainMilli();
    }

    public static long playerKillGainMilli(@Nullable Mob mob) {
        if (mob == null) return playerKillGainMilli();
        return playerKillGainMilli(mob.getType());
    }

    @SuppressWarnings("unused")
    public static long playerKillGainMilli(@Nullable Entity entity) {
        if (entity == null) return playerKillGainMilli();
        return playerKillGainMilli(entity.getType());
    }

    @SuppressWarnings("unused")
    public static long playerKillGainMilli(@Nullable String entityId) {
        if (entityId == null || entityId.isBlank()) return playerKillGainMilli();

        ensureOverridesUpToDate();

        try {
            ResourceLocation id = ResourceLocation.parse(entityId.trim());
            Long override = playerKillOverridesMilli.get(id);
            return (override != null) ? override : playerKillGainMilli();
        } catch (Exception ignored) {
            return playerKillGainMilli();
        }
    }

    public static long passiveKillGainMilli() {
        return toMilli(CommonConfig.GAIN_PASSIVE_KILL.get());
    }
}
