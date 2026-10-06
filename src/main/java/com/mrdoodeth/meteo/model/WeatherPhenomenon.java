package com.mrdoodeth.meteo.model;

/** Погодное явление. */
public enum WeatherPhenomenon {
    NONE,
    SUNNY,
    CLOUDY,
    RAIN,
    SNOW,
    HAIL,
    FOG,
    THUNDERSTORM;

    /** Указывает, является ли само явление осадками. */
    public boolean isPrecipitation() {
        return switch (this) {
            case RAIN, SNOW, HAIL -> true;
            default -> false;
        };
    }
}
