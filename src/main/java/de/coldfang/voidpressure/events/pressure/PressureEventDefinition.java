package de.coldfang.voidpressure.events.pressure;

public record PressureEventDefinition(
        String id,
        String type,
        long thresholdMilli,
        boolean isAbsolute,
        String value,
        String announce,
        String soundId,
        boolean isConsistent
) {}

