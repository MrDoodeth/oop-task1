package com.mrdoodeth.meteo.model;

import java.time.ZoneId;
import java.util.Objects;

/** Район.
 * @param id идентификатор района
 * @param zone часовой пояс
 */
public record District(String id, ZoneId zone) {
    /** Проверяет обязательные поля.
     * @throws IllegalArgumentException при пустом ID или ID, равном null
     * @throws NullPointerException при отсутствующем часовом поясе
     */
    public District {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("District ID must not be blank");
        }
        Objects.requireNonNull(zone, "zone");
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof District district)) return false;
        return Objects.equals(id, district.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
