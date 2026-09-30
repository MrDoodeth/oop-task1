package com.mrdoodeth.meteo.model;

import java.time.Instant;
import java.util.Objects;

/** Неизменяемое показание станции в конкретный момент времени.
 * @param stationId ID станции
 * @param timestamp момент UTC
 * @param temperature температура в градусах Цельсия
 * @param pressureHpa давление в гектопаскалях
 * @param windSpeed скорость ветра в метрах в секунду
 * @param windDirection направление, откуда дует ветер
 * @param phenomenon наблюдаемое явление
 */
public record Observation(
        String stationId,
        Instant timestamp,
        double temperature,
        double pressureHpa,
        double windSpeed,
        WindDirection windDirection,
        WeatherPhenomenon phenomenon
) {
    /** Проверяет обязательные поля.
     * @throws IllegalArgumentException при неверных данных или пустом ID
     * @throws NullPointerException при отсутствующем обязательном объекте
     */
    public Observation {
        if (stationId == null || stationId.isBlank()) {
            throw new IllegalArgumentException("Station ID must not be blank");
        }
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(windDirection, "windDirection");
        Objects.requireNonNull(phenomenon, "phenomenon");
        if (!Double.isFinite(temperature)) {
            throw new IllegalArgumentException("Temperature must be finite");
        }
        if (!Double.isFinite(pressureHpa) || pressureHpa <= 0) {
            throw new IllegalArgumentException("Pressure must be positive and finite");
        }
        if (!Double.isFinite(windSpeed) || windSpeed < 0) {
            throw new IllegalArgumentException("Wind speed must be non-negative and finite");
        }
    }
}
