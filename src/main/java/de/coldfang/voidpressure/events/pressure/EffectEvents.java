package de.coldfang.voidpressure.events.pressure;

import de.coldfang.voidpressure.data.VoidPressureSavedData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.Locale;

public class EffectEvents {

    private EffectEvents() {}

    public static void fire(ServerLevel level, PressureEventDefinition def) {
        String value = def.value();
        if (value == null || value.isBlank()) return;

        String[] parts = value.split(";", -1);
        if (parts.length < 2) return;

        String mode = parts[0].trim().toLowerCase(Locale.ROOT);

        if ("potion".equals(mode)) {
            firePotion(level, def, parts);
        } else if ("attribute".equals(mode)) {
            fireAttribute(level, def, parts);
        }
    }

    public static void syncAllFor(ServerLevel level) {
        var data = VoidPressureSavedData.get(level);

        for (PressureEventDefinition def : PressureEventManager.getEventsForSync()) {
            if (!"effect".equalsIgnoreCase(def.type())) continue;
            if (!def.isConsistent()) continue;
            if (!data.isEventTriggered(def.id())) continue;

            fire(level, def);
        }
    }

    // Applies a mob effect to all online players in the level
    private static void firePotion(ServerLevel level, PressureEventDefinition def, String[] parts) {
        // potion;<effectId>;<durationTicks>;<amplifier>
        if (parts.length < 4) return;

        ResourceLocation effectId = ResourceLocation.tryParse(parts[1].trim());
        if (effectId == null) return;

        int duration;
        int amplifier;
        try {
            duration = Integer.parseInt(parts[2].trim());
            amplifier = Integer.parseInt(parts[3].trim());
        } catch (Exception e) {
            return;
        }

        // Consistent effects ignore duration and use an effectively infinite duration
        if (def.isConsistent()) {
            duration = Integer.MAX_VALUE;
        }

        var effectHolder = BuiltInRegistries.MOB_EFFECT.getHolder(effectId).orElse(null);
        if (effectHolder == null) return;

        MobEffectInstance inst = new MobEffectInstance(effectHolder, duration, amplifier, true, true, true);

        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            p.addEffect(inst);
        }
    }

    // Applies an attribute modifier to all online players in the level
    private static void fireAttribute(ServerLevel level, PressureEventDefinition def, String[] parts) {
        // attribute;<attributeId>;<amount>;<operation>
        if (parts.length < 4) return;

        ResourceLocation attrId = ResourceLocation.tryParse(parts[1].trim());
        if (attrId == null) return;

        double amount;
        try {
            amount = Double.parseDouble(parts[2].trim());
        } catch (Exception e) {
            return;
        }

        AttributeModifier.Operation op = parseOperation(parts[3]);
        if (op == null) return;

        var attrHolder = BuiltInRegistries.ATTRIBUTE.getHolder(attrId).orElse(null);
        if (attrHolder == null) return;

        // Stable id per event to update instead of stacking
        ResourceLocation modId = ResourceLocation.fromNamespaceAndPath("voidpressure", def.id());
        AttributeModifier mod = new AttributeModifier(modId, amount, op);

        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            AttributeInstance inst = p.getAttributes().getInstance(attrHolder);
            if (inst == null) continue;

            inst.removeModifier(modId);
            inst.addTransientModifier(mod);
        }
    }

    private static AttributeModifier.Operation parseOperation(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toLowerCase(Locale.ROOT);

        return switch (s) {
            case "add" -> AttributeModifier.Operation.ADD_VALUE;
            case "mul_base" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case "mul_total" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            default -> null;
        };
    }
}
