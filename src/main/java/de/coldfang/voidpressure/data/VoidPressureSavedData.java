package de.coldfang.voidpressure.data;

import de.coldfang.voidpressure.config.CommonConfig;
import de.coldfang.voidpressure.events.pressure.PressureEventManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VoidPressureSavedData extends SavedData {

    private static final String NAME = "void_pressure";

    // Stored as milli-units (1 = 0.001)
    private long pressureMilli;

    // Global altar ritual cooldown (game time ticks)
    private long lastAltarRitualGameTime = -1L;

    // One-time triggers (isConsistent=true)
    private final Set<String> triggeredEvents = new HashSet<>();

    // Repeat thresholds (kept for future use)
    private final Map<String, Long> nextRepeatTrigger = new HashMap<>();

    // History (persisted)
    public record HistoryEntry(
            long timeMs,
            long pressureMilli,
            String eventId,
            String type,
            String summary
    ) {
    }

    private static final int HISTORY_MAX = 200;
    private final Deque<HistoryEntry> history = new ArrayDeque<>();

    // Rolling window (last 60s game time = 1200 ticks), runtime-only
    private static final long WINDOW_TICKS = 20L * 60L;

    private record DeltaEntry(long gameTick, long deltaMilli) {
    }

    private final Deque<DeltaEntry> recentDeltas = new ArrayDeque<>();
    private long recentGainSumMilli = 0L;
    private long recentLossSumMilli = 0L;
    private long lastRollingWindowPruneTick = Long.MIN_VALUE;

    // Pressure getters

    public long getPressureMilli() {
        return pressureMilli;
    }

    public double getPressure() {
        return pressureMilli / 1000.0;
    }

    public double getGainLast60s() {
        return recentGainSumMilli / 1000.0;
    }

    public double getLossLast60s() {
        return recentLossSumMilli / 1000.0;
    }

    public double getNetLast60s() {
        return (recentGainSumMilli - recentLossSumMilli) / 1000.0;
    }

    // Rolling window tick entrypoint

    @SuppressWarnings("unused")
    public void updateRollingWindow(ServerLevel level) {
        tickRollingWindow(level);
    }

    public void tickRollingWindow(ServerLevel level) {
        if (level == null) return;

        long nowTick = level.getGameTime();
        if (nowTick == lastRollingWindowPruneTick) return;

        lastRollingWindowPruneTick = nowTick;
        pruneOldDeltas(nowTick);
    }

    // Global altar cooldown

    public long getLastAltarRitualGameTime() {
        return lastAltarRitualGameTime;
    }

    public void setLastAltarRitualGameTime(long t) {
        if (lastAltarRitualGameTime != t) {
            lastAltarRitualGameTime = t;
            setDirty();
        }
    }

    // Pressure mutations

    @SuppressWarnings("UnusedReturnValue")
    public long removePressureMilli(ServerLevel level, long milli) {
        if (milli <= 0) return pressureMilli;

        long before = pressureMilli;
        long after = Math.max(0L, before - milli);
        if (after == before) return pressureMilli;

        long removed = before - after;

        pressureMilli = after;
        setDirty();

        recordDelta(level, -removed);
        PressureEventManager.onPressureChanged(level, before, after);
        return after;
    }

    @SuppressWarnings({"unused", "UnusedReturnValue"})
    public long removePressureMilli(long milli) {
        if (milli <= 0) return pressureMilli;

        long before = pressureMilli;
        pressureMilli = Math.max(0L, pressureMilli - milli);

        if (pressureMilli != before) {
            setDirty();
        }

        return pressureMilli;
    }

    @SuppressWarnings("UnusedReturnValue")
    public long addPressureMilliAndTrigger(ServerLevel level, long milli) {
        if (milli <= 0) return pressureMilli;

        long allowed = applyPerMinuteCap(level, milli);
        if (allowed <= 0) return pressureMilli;

        return addPressureInternal(level, allowed);
    }

    @SuppressWarnings("UnusedReturnValue")
    public long addPressureUncapped(ServerLevel level, double amount) {
        if (amount <= 0) return pressureMilli;

        long milli = Math.round(amount * 1000.0);
        if (milli <= 0) return pressureMilli;

        return addPressureInternal(level, milli);
    }

    // Adds pressure, records rolling-window delta, and fires the pressure-changed hook
    private long addPressureInternal(ServerLevel level, long milli) {
        long before = pressureMilli;
        pressureMilli += milli;
        setDirty();

        recordDelta(level, milli);

        long after = pressureMilli;
        PressureEventManager.onPressureChanged(level, before, after);
        return after;
    }

    public void setPressure(double value) {
        pressureMilli = Math.max(0, Math.round(value * 1000.0));
        setDirty();
    }

    @SuppressWarnings("unused")
    public void setPressure(ServerLevel level, double value) {
        long newMilli = Math.max(0, Math.round(value * 1000.0));
        long before = pressureMilli;
        if (newMilli == before) return;

        pressureMilli = newMilli;
        setDirty();

        long delta = newMilli - before;
        recordDelta(level, delta);

        PressureEventManager.onPressureChanged(level, before, newMilli);
    }

    // One-time triggers

    public boolean isEventTriggered(String id) {
        return triggeredEvents.contains(id);
    }

    public void markEventTriggered(String id) {
        if (triggeredEvents.add(id)) {
            setDirty();
        }
    }

    @SuppressWarnings("unused")
    public void clearTriggeredEvents() {
        if (!triggeredEvents.isEmpty()) {
            triggeredEvents.clear();
            setDirty();
        }
    }

    @SuppressWarnings("unused")
    public Set<String> getTriggeredEventsView() {
        return Collections.unmodifiableSet(triggeredEvents);
    }

    // History

    public void addHistory(long timeMs, long pressureMilli, String eventId, String type, String summary) {
        history.addLast(new HistoryEntry(timeMs, pressureMilli, eventId, type, summary));
        while (history.size() > HISTORY_MAX) {
            history.removeFirst();
        }
        setDirty();
    }

    public List<HistoryEntry> getHistoryNewestFirst() {
        if (history.isEmpty()) return List.of();
        List<HistoryEntry> out = new ArrayList<>(history);
        Collections.reverse(out);
        return out;
    }

    @SuppressWarnings("unused")
    public void clearHistory() {
        if (!history.isEmpty()) {
            history.clear();
            setDirty();
        }
    }

    // Rolling window internals

    private void recordDelta(ServerLevel level, long deltaMilli) {
        if (deltaMilli == 0) return;
        if (level == null) return;

        long nowTick = level.getGameTime();

        pruneOldDeltas(nowTick);
        lastRollingWindowPruneTick = nowTick;

        recentDeltas.addLast(new DeltaEntry(nowTick, deltaMilli));
        applyDeltaToWindowSums(deltaMilli);
    }

    private void pruneOldDeltas(long nowTick) {
        long minTick = nowTick - WINDOW_TICKS;

        while (!recentDeltas.isEmpty()) {
            DeltaEntry first = recentDeltas.peekFirst();
            if (first.gameTick >= minTick) break;

            recentDeltas.removeFirst();
            removeDeltaFromWindowSums(first.deltaMilli);
        }
    }

    private void applyDeltaToWindowSums(long deltaMilli) {
        if (deltaMilli > 0) {
            recentGainSumMilli += deltaMilli;
        } else if (deltaMilli < 0) {
            recentLossSumMilli += (-deltaMilli);
        }

        if (recentGainSumMilli < 0) recentGainSumMilli = 0;
        if (recentLossSumMilli < 0) recentLossSumMilli = 0;
    }

    private void removeDeltaFromWindowSums(long deltaMilli) {
        if (deltaMilli > 0) {
            recentGainSumMilli -= deltaMilli;
        } else if (deltaMilli < 0) {
            recentLossSumMilli -= (-deltaMilli);
        }

        if (recentGainSumMilli < 0) recentGainSumMilli = 0;
        if (recentLossSumMilli < 0) recentLossSumMilli = 0;
    }

    // Per-minute gain cap (rolling 60s window)

    private long applyPerMinuteCap(ServerLevel level, long requestedMilli) {
        double capPerMinute = CommonConfig.MAX_PRESSURE_PER_MINUTE.get();
        if (capPerMinute <= 0.0) return requestedMilli;
        if (level == null) return requestedMilli;

        long capMilli = Math.round(capPerMinute * 1000.0);

        long nowTick = level.getGameTime();
        pruneOldDeltas(nowTick);
        lastRollingWindowPruneTick = nowTick;

        long remaining = capMilli - recentGainSumMilli;
        if (remaining <= 0) return 0;

        return Math.min(requestedMilli, remaining);
    }

    // SavedData boilerplate

    public static VoidPressureSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new Factory<>(VoidPressureSavedData::new, VoidPressureSavedData::load),
                NAME
        );
    }

    public static VoidPressureSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        VoidPressureSavedData data = new VoidPressureSavedData();
        data.pressureMilli = tag.getLong("pressureMilli");
        data.lastAltarRitualGameTime = tag.getLong("lastAltarRitualGameTime");

        ListTag triggered = tag.getList("triggeredEvents", Tag.TAG_STRING);
        for (int i = 0; i < triggered.size(); i++) {
            data.triggeredEvents.add(triggered.getString(i));
        }

        CompoundTag nextTag = tag.getCompound("nextRepeatTrigger");
        for (String key : nextTag.getAllKeys()) {
            data.nextRepeatTrigger.put(key, nextTag.getLong(key));
        }

        ListTag hist = tag.getList("history", Tag.TAG_COMPOUND);
        loadHistoryInto(data.history, hist);

        while (data.history.size() > HISTORY_MAX) data.history.removeFirst();

        return data;
    }

    private static void loadHistoryInto(Deque<HistoryEntry> target, ListTag hist) {
        for (int i = 0; i < hist.size(); i++) {
            CompoundTag e = hist.getCompound(i);
            HistoryEntry he = readHistoryEntry(e);
            if (he != null) {
                target.addLast(he);
            }
        }
    }

    private static HistoryEntry readHistoryEntry(CompoundTag e) {
        String eventId = e.getString("eventId");
        if (eventId.isEmpty()) return null;

        long timeMs = e.getLong("timeMs");
        long p = e.getLong("pressureMilli");
        String type = e.getString("type");
        String summary = e.getString("summary");

        return new HistoryEntry(timeMs, p, eventId, type, summary);
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        tag.putLong("pressureMilli", pressureMilli);
        tag.putLong("lastAltarRitualGameTime", lastAltarRitualGameTime);

        ListTag triggered = new ListTag();
        for (String id : triggeredEvents) {
            triggered.add(StringTag.valueOf(id));
        }
        tag.put("triggeredEvents", triggered);

        CompoundTag nextTag = new CompoundTag();
        for (Map.Entry<String, Long> e : nextRepeatTrigger.entrySet()) {
            nextTag.putLong(e.getKey(), e.getValue());
        }
        tag.put("nextRepeatTrigger", nextTag);

        ListTag hist = new ListTag();
        for (HistoryEntry e : history) {
            hist.add(writeHistoryEntry(e));
        }
        tag.put("history", hist);

        return tag;
    }

    private static CompoundTag writeHistoryEntry(HistoryEntry e) {
        CompoundTag t = new CompoundTag();
        t.putLong("timeMs", e.timeMs);
        t.putLong("pressureMilli", e.pressureMilli);
        t.putString("eventId", e.eventId);
        t.putString("type", e.type == null ? "" : e.type);
        t.putString("summary", e.summary == null ? "" : e.summary);
        return t;
    }
}
