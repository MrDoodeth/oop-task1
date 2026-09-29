package com.mrdoodeth.meteo.model;

import java.time.ZoneId;
import java.util.Objects;

public final class District {
    private final String id;
    private final String name;
    private final ZoneId zone;

    public District(String id, String name, ZoneId zone) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("District ID must not be blank");
        }
        this.id = id;
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("District name must not be blank");
        }
        this.name = name;
        this.zone = Objects.requireNonNull(zone, "zone");
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public ZoneId zone() {
        return zone;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof District district)) return false;
        return Objects.equals(id, district.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
