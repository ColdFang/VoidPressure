package de.coldfang.voidpressure.events.pressure;

import de.coldfang.voidpressure.config.CommonConfig;
import de.coldfang.voidpressure.data.VoidPressureSavedData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class PressureEventManager {

    private static List<PressureEventDefinition> cached;

    public static void onPressureChanged(ServerLevel level, long beforeMilli, long afterMilli) {
        if (afterMilli <= beforeMilli) return;

        var data = VoidPressureSavedData.get(level);

        for (PressureEventDefinition def : getEvents()) {
            long t = def.thresholdMilli();
            if (t <= 0) continue;

            boolean shouldTrigger;

            if (def.isAbsolute()) {
                shouldTrigger = beforeMilli < t && afterMilli >= t;
            } else {
                // Intervall: t, 2t, 3t ...
                long beforeK = beforeMilli / t;
                long afterK = afterMilli / t;
                shouldTrigger = afterK > beforeK;
            }

            if (!shouldTrigger) continue;

            // isConsistent = permanent freigeschaltet (einmalig)
            if (def.isConsistent()) {
                if (data.isEventTriggered(def.id())) continue;
                data.markEventTriggered(def.id());
            }
            // History Log: nur wenn es wirklich gefeuert wird
            data.addHistory(
                    System.currentTimeMillis(),
                    data.getPressureMilli(),
                    def.id(),
                    def.type(),
                    def.value()
            );

            fire(level, def);
        }
    }

    private static void fire(ServerLevel level, PressureEventDefinition def) {
        MinecraftServer server = level.getServer();



        // announce
        String announce = def.announce();
        if (announce != null && !announce.isBlank()) {
            server.getPlayerList()
                    .broadcastSystemMessage(Component.literal(announce), false);
        }

        switch (def.type()) {
            case "command" -> {
                String cmd = def.value().startsWith("/")
                        ? def.value().substring(1)
                        : def.value();
                server.getCommands()
                        .performPrefixedCommand(
                                server.createCommandSourceStack(),
                                cmd
                        );
            }
            case "effect" -> EffectEvents.fire(level, def);

            // mob_change:
            // absichtlich leer – wird im Spawn-Event ausgewertet
            case "mob_change" -> {
            }
        }

        // soundId (leer = kein Sound)
        String id = def.soundId();
        if (id != null && !id.isBlank()) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl != null) {
                BuiltInRegistries.SOUND_EVENT.getOptional(rl).ifPresent(se -> {
                    for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                        p.playNotifySound(se, SoundSource.MASTER, 1.0f, 1.0f);
                    }
                });
            }
        }

    }

    public static List<PressureEventDefinition> getEventsForSync() {
        return getEvents();
    }

    // Parsing / Caching

    private static List<PressureEventDefinition> getEvents() {
        if (cached != null) return cached;

        List<PressureEventDefinition> out = new ArrayList<>();

        // COMMAND
        for (String line : CommonConfig.COMMAND_EVENTS.get()) {
            PressureEventDefinition def = parseCommand(line);
            if (def != null) out.add(def);
        }

        // EFFECT
        for (String line : CommonConfig.EFFECT_EVENTS.get()) {
            PressureEventDefinition def = parseEffect(line);
            if (def != null) out.add(def);
        }

        // MOB_CHANGE
        for (String line : CommonConfig.MOB_CHANGE_EVENTS.get()) {
            PressureEventDefinition def = parseMobChange(line);
            if (def != null) out.add(def);
        }

        cached = out;
        return cached;
    }

    private static PressureEventDefinition parseLine(String line, String type, String idPrefix) {
        // pressure|isAbsolute|value|announce|soundId|isConsistent
        if (line == null) return null;

        String[] parts = line.split("\\|", 6);
        if (parts.length < 3) return null;

        long thresholdMilli = parsePressure(parts[0]);
        if (thresholdMilli <= 0) return null;

        boolean isAbsolute = Boolean.parseBoolean(parts[1].trim());
        String value = parts[2].trim();
        String announce = parts.length >= 4 ? parts[3].trim() : "";
        String soundId = parts.length >= 5 ? parts[4].trim() : "";
        boolean isConsistent = parts.length >= 6 && Boolean.parseBoolean(parts[5].trim());

        String id = idPrefix + sha1Hex(line);

        return new PressureEventDefinition(
                id,
                type,
                thresholdMilli,
                isAbsolute,
                value,
                announce,
                soundId,
                isConsistent
        );
    }

    private static PressureEventDefinition parseCommand(String line) {
        // command uses the same schema; value = command string
        return parseLine(line, "command", "command_");
    }

    private static PressureEventDefinition parseEffect(String line) {
        return parseLine(line, "effect", "effect_");
    }

    private static PressureEventDefinition parseMobChange(String line) {
        return parseLine(line, "mob_change", "mob_change_");
    }


    private static long parsePressure(String raw) {
        try {
            double p = Double.parseDouble(raw.trim());
            return Math.round(p * 1000.0);
        } catch (Exception e) {
            return -1;
        }
    }

    private static String sha1Hex(String s) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }

    public static void clearCache() {
        cached = null;
    }
}
