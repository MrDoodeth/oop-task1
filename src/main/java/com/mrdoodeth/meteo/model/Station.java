package com.mrdoodeth.meteo.model;

import java.util.Objects;

/** Неизменяемая станция; идентичность определяется глобальным ID.
 * @param id идентификатор станции
 * @param district район станции, задающий её часовой пояс
 */
public record Station(String id, District district) {
    /** Проверяет поля станции.
     * @throws IllegalArgumentException если ID пуст
     * @throws NullPointerException если район отсутствует
     */
    public Station {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Station ID must not be blank");
        }
        Objects.requireNonNull(district, "district");
    }

    /** Сравнивает станции по глобальному ID.
     * @param other объект для сравнения
     * @return true при одинаковом ID станции
     */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Station station)) return false;
        return Objects.equals(id, station.id);
    }

    /** Возвращает хеш идентификатора станции.
     * @return хеш ID
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
