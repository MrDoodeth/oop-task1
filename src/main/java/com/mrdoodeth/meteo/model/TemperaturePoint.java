package com.mrdoodeth.meteo.model;

import java.time.Instant;
import java.util.Objects;

/** Точка суточного хода без усреднения между станциями.
 * @param stationId станция
 * @param timestamp момент измерения
 * @param temperature температура в градусах Цельсия
 */
public record TemperaturePoint(String stationId, Instant timestamp, double temperature) {
    /** Проверяет обязательные поля.
     * @throws IllegalArgumentException при неверной температуре или пустом ID, включая null
     * @throws NullPointerException при отсутствующем моменте измерения
     */
    public TemperaturePoint {
        if (stationId == null || stationId.isBlank() || !Double.isFinite(temperature)) {
            throw new IllegalArgumentException("Invalid temperature point");
        }
        Objects.requireNonNull(timestamp, "timestamp");
    }
}
