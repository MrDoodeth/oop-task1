package com.mrdoodeth.meteo.model;

public enum WindDirection {
    NORTH(337.5, 22.5),
    NORTHEAST(22.5, 67.5),
    EAST(67.5, 112.5),
    SOUTHEAST(112.5, 157.5),
    SOUTH(157.5, 202.5),
    SOUTHWEST(202.5, 247.5),
    WEST(247.5, 292.5),
    NORTHWEST(292.5, 337.5);

    private final double fromInclusive;
    private final double toExclusive;

    WindDirection(double fromInclusive, double toExclusive) {
        this.fromInclusive = fromInclusive;
        this.toExclusive = toExclusive;
    }

    public static WindDirection fromAzimuth(double azimuth) {
        if (!Double.isFinite(azimuth) || azimuth < 0 || azimuth >= 360) {
            throw new IllegalArgumentException("Azimuth must be in [0, 360)");
        }

        for (var direction : values()) {
            if (direction.contains(azimuth)) {
                return direction;
            }
        }

        throw new IllegalStateException("Rhumb was not found");
    }

    private boolean contains(double azimuth) {
        return fromInclusive < toExclusive
                ? (fromInclusive <= azimuth && azimuth < toExclusive)
                : (fromInclusive <= azimuth || azimuth < toExclusive);
    }
}
