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

    /** Указывает, является ли само явление осадками.
     * @return true для дождя, снега и града
     */
    public boolean isPrecipitation() {
        return switch (this) {
            case RAIN, SNOW, HAIL -> true;
            default -> false;
        };
    }
}
