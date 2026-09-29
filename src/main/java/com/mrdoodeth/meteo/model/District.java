package com.mrdoodeth.meteo.model;

import java.time.ZoneId;
import java.util.Objects;

public record District(String id, String name, ZoneId zone) {
    public District {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("District ID must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("District name must not be blank");
        }
        Objects.requireNonNull(zone, "zone");
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof District district)) return false;
        return id.equals(district.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
