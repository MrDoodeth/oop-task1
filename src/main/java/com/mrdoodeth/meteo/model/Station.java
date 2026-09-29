package com.mrdoodeth.meteo.model;

import java.time.ZoneId;
import java.util.Objects;

public final class Station {
    private final String id;
    private final String name;
    private final District district;
    private final ZoneId zone;

    public Station(String id, String name, District district, ZoneId zone) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Station ID must not be blank");
        }
        this.id = id;
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Station name must not be blank");
        }
        this.name = name;
        this.district = Objects.requireNonNull(district, "district");
        this.zone = Objects.requireNonNull(zone, "zone");
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public District district() {
        return district;
    }

    public ZoneId zone() {
        return zone;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Station station)) return false;
        return Objects.equals(id, station.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
