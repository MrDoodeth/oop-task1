package com.mrdoodeth.meteo.model;

import java.time.ZoneId;
import java.util.Objects;

public record Station(String id, String name, District district, ZoneId zone) {
    public Station {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Station ID must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Station name must not be blank");
        }
        Objects.requireNonNull(district, "district");
        Objects.requireNonNull(zone, "zone");
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Station station)) return false;
        return id.equals(station.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
