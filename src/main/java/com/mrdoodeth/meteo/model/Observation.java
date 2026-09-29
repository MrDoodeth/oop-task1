package com.mrdoodeth.meteo.model;

import java.time.Instant;
import java.util.Objects;

public record Observation(
        String stationId,
        Instant timestamp,
        double temperatureCelsius,
        double pressureHpa,
        double windSpeedMetersPerSecond,
        WindDirection windDirection,
        WeatherPhenomenon phenomenon
) {
    public Observation {
        if (stationId == null || stationId.isBlank()) {
            throw new IllegalArgumentException("Station ID must not be blank");
        }
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(windDirection, "windDirection");
        Objects.requireNonNull(phenomenon, "phenomenon");
        if (!Double.isFinite(temperatureCelsius)) {
            throw new IllegalArgumentException("Temperature must be finite");
        }
        if (!Double.isFinite(pressureHpa) || pressureHpa <= 0) {
            throw new IllegalArgumentException("Pressure must be positive and finite");
        }
        if (!Double.isFinite(windSpeedMetersPerSecond) || windSpeedMetersPerSecond < 0) {
            throw new IllegalArgumentException("Wind speed must be non-negative and finite");
        }
    }
}
