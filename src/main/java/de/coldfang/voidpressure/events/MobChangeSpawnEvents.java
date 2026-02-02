package de.coldfang.voidpressure.events;

import de.coldfang.voidpressure.VoidPressure;
import de.coldfang.voidpressure.data.VoidPressureSavedData;
import de.coldfang.voidpressure.events.pressure.PressureEventDefinition;
import de.coldfang.voidpressure.events.pressure.PressureEventManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@EventBusSubscriber(modid = VoidPressure.MODID)
@SuppressWarnings("unused")
public class MobChangeSpawnEvents {

    private MobChangeSpawnEvents() {}

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getEntity() instanceof Mob mob)) return;

        if (event.loadedFromDisk()) return;

        if (level.getDifficulty() == Difficulty.PEACEFUL) return;

        var data = VoidPressureSavedData.get(level);
        long pressureMilli = data.getPressureMilli();

        for (PressureEventDefinition def : PressureEventManager.getEventsForSync()) {
            if (!"mob_change".equalsIgnoreCase(def.type())) continue;

            if (!isMobChangeActive(def, pressureMilli, data)) continue;

            MobChangeValue parsed = MobChangeValue.parse(def.value());
            if (parsed == null) continue;

            if (!parsed.target().matches(mob)) continue;

            applyRules(mob, def, parsed.rules());
        }
    }

    private static boolean isMobChangeActive(PressureEventDefinition def, long pressureMilli, VoidPressureSavedData data) {
        long t = def.thresholdMilli();
        if (t <= 0) return false;

        // isConsistent=true => gilt ab Freischaltung dauerhaft (nach Relog)
        if (def.isConsistent()) {
            return data.isEventTriggered(def.id());
        }

        // isConsistent=false = "live": gilt nur, wenn aktuell >= Schwelle
        // (und nach Relog weg, weil nicht gespeichert)
        return pressureMilli >= t;
    }

    // value => [target];rule1,rule2,...

    private record MobChangeValue(Target target, List<Rule> rules) {
        static MobChangeValue parse(String raw) {
            if (raw == null) return null;
            String s = raw.trim();
            if (s.isEmpty()) return null;

            String targetPart = null;
            String rulesPart = s;

            int semi = s.indexOf(';');
            if (semi >= 0) {
                targetPart = s.substring(0, semi).trim();
                rulesPart = s.substring(semi + 1).trim();
            }

            Target target = Target.parse(targetPart);
            List<Rule> rules = Rule.parseList(rulesPart);

            if (rules.isEmpty()) return null;
            return new MobChangeValue(target, rules);
        }
    }

    private sealed interface Target permits TargetAll, TargetMob, TargetTag, TargetCategory {
        boolean matches(Mob mob);

        static Target parse(String raw) {
            if (raw == null || raw.isBlank() || "all".equalsIgnoreCase(raw.trim())) {
                return new TargetAll();
            }

            String s = raw.trim();
            String lower = s.toLowerCase(Locale.ROOT);

            if (lower.startsWith("mob:")) {
                String id = s.substring("mob:".length()).trim();
                ResourceLocation rl = ResourceLocation.tryParse(id);
                return rl != null ? new TargetMob(rl) : new TargetAll();
            }

            if (lower.startsWith("tag:")) {
                String id = s.substring("tag:".length()).trim();
                ResourceLocation rl = ResourceLocation.tryParse(id);
                return rl != null ? TargetTag.of(rl) : new TargetAll();
            }

            if (lower.startsWith("category:")) {
                String cat = s.substring("category:".length()).trim().toLowerCase(Locale.ROOT);
                return new TargetCategory(cat);
            }

            return new TargetAll();
        }
    }

    private record TargetAll() implements Target {
        @Override public boolean matches(Mob mob) { return true; }
    }

    private record TargetMob(ResourceLocation mobId) implements Target {
        @Override
        public boolean matches(Mob mob) {
            ResourceLocation actual = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
            return mobId.equals(actual);
        }
    }

    private record TargetTag(TagKey<net.minecraft.world.entity.EntityType<?>> tag) implements Target {

        static TargetTag of(ResourceLocation tagId) {
            return new TargetTag(TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, tagId));
        }

        @Override
        public boolean matches(Mob mob) {
            return mob.getType().is(tag);
        }
    }

    private record TargetCategory(String cat) implements Target {
        @Override
        public boolean matches(Mob mob) {
            MobCategory c = mob.getType().getCategory();
            return switch (cat) {
                case "monster" -> c == MobCategory.MONSTER;
                case "creature" -> c == MobCategory.CREATURE;
                case "ambient" -> c == MobCategory.AMBIENT;
                case "water_creature" -> c == MobCategory.WATER_CREATURE;
                case "underground_water_creature" -> c == MobCategory.UNDERGROUND_WATER_CREATURE;
                case "water_ambient" -> c == MobCategory.WATER_AMBIENT;
                case "misc" -> c == MobCategory.MISC;
                default -> false;
            };
        }
    }

    // Rules

    private sealed interface Rule permits RuleHealth, RuleArmor, RuleSpeed, RuleHelmet {
        void apply(Mob mob, PressureEventDefinition def);

        static List<Rule> parseList(String raw) {
            List<Rule> out = new ArrayList<>();
            if (raw == null || raw.isBlank()) return out;

            for (String token : raw.split(",")) {
                Rule r = parseOne(token.trim());
                if (r != null) out.add(r);
            }
            return out;
        }

        static Rule parseOne(String token) {
            if (token == null || token.isBlank()) return null;
            String lower = token.toLowerCase(Locale.ROOT);

            if (lower.startsWith("health:")) {
                Double v = parseDouble(token.substring("health:".length()));
                return v == null ? null : new RuleHealth(v);
            }

            if (lower.startsWith("armor:")) {
                Double v = parseDouble(token.substring("armor:".length()));
                return v == null ? null : new RuleArmor(v);
            }

            if (lower.startsWith("speed:")) {
                Double v = parseDouble(token.substring("speed:".length()));
                return v == null ? null : new RuleSpeed(v);
            }

            if (lower.startsWith("helmet:")) {
                String mat = token.substring("helmet:".length()).trim().toLowerCase(Locale.ROOT);
                return new RuleHelmet(mat);
            }

            return null;
        }

        static Double parseDouble(String raw) {
            if (raw == null) return null;
            String s = raw.trim();
            if (s.isEmpty()) return null;
            try {
                return Double.parseDouble(s);
            } catch (Exception e) {
                return null;
            }
        }
    }

    private record RuleHealth(double add) implements Rule {
        @Override
        public void apply(Mob mob, PressureEventDefinition def) {
            ResourceLocation attrId = ResourceLocation.tryParse("minecraft:generic.max_health");
            if (attrId == null) return;

            var holder = BuiltInRegistries.ATTRIBUTE.getHolder(attrId).orElse(null);
            if (holder == null) return;

            AttributeInstance inst = mob.getAttributes().getInstance(holder);
            if (inst == null) return;

            ResourceLocation modId = ResourceLocation.fromNamespaceAndPath("voidpressure", def.id() + "_health");
            inst.removeModifier(modId);
            inst.addTransientModifier(new AttributeModifier(modId, add, AttributeModifier.Operation.ADD_VALUE));

            mob.setHealth((float) inst.getValue());
        }
    }

    private record RuleArmor(double add) implements Rule {
        @Override
        public void apply(Mob mob, PressureEventDefinition def) {
            ResourceLocation attrId = ResourceLocation.tryParse("minecraft:generic.armor");
            if (attrId == null) return;

            var holder = BuiltInRegistries.ATTRIBUTE.getHolder(attrId).orElse(null);
            if (holder == null) return;

            AttributeInstance inst = mob.getAttributes().getInstance(holder);
            if (inst == null) return;

            ResourceLocation modId = ResourceLocation.fromNamespaceAndPath("voidpressure", def.id() + "_armor");
            inst.removeModifier(modId);
            inst.addTransientModifier(new AttributeModifier(modId, add, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private record RuleSpeed(double add) implements Rule {
        @Override
        public void apply(Mob mob, PressureEventDefinition def) {
            ResourceLocation attrId = ResourceLocation.tryParse("minecraft:generic.movement_speed");
            if (attrId == null) return;

            var holder = BuiltInRegistries.ATTRIBUTE.getHolder(attrId).orElse(null);
            if (holder == null) return;

            AttributeInstance inst = mob.getAttributes().getInstance(holder);
            if (inst == null) return;

            ResourceLocation modId = ResourceLocation.fromNamespaceAndPath("voidpressure", def.id() + "_speed");
            inst.removeModifier(modId);
            inst.addTransientModifier(new AttributeModifier(modId, add, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private record RuleHelmet(String material) implements Rule {
        @Override
        public void apply(Mob mob, PressureEventDefinition def) {
            Item item = switch (material) {
                case "leather" -> BuiltInRegistries.ITEM.get(ResourceLocation.tryParse("minecraft:leather_helmet"));
                case "iron" -> BuiltInRegistries.ITEM.get(ResourceLocation.tryParse("minecraft:iron_helmet"));
                case "diamond" -> BuiltInRegistries.ITEM.get(ResourceLocation.tryParse("minecraft:diamond_helmet"));
                case "netherite" -> BuiltInRegistries.ITEM.get(ResourceLocation.tryParse("minecraft:netherite_helmet"));
                default -> null;
            };
            if (item == null) return;

            mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(item));
        }
    }

    private static void applyRules(Mob mob, PressureEventDefinition def, List<Rule> rules) {
        for (Rule r : rules) r.apply(mob, def);
    }
}
