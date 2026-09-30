package com.mrdoodeth.meteo.model;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/** Вычисляет уникальные даты осадков по часовому поясу района.
 * @param zone часовой пояс для определения календарной даты
 */
public record PrecipitationDaysCalculation(ZoneId zone)
        implements ObservationCalculation<List<LocalDate>> {
    /** Проверяет часовой пояс.
     * @throws NullPointerException если пояс не указан
     */
    public PrecipitationDaysCalculation {
        Objects.requireNonNull(zone, "zone");
    }

    /** {@inheritDoc} */
    @Override
    public List<LocalDate> calculate(List<Observation> observations) {
        var days = new TreeSet<LocalDate>();
        for (var observation : observations) {
            if (observation.phenomenon().isPrecipitation()) {
                days.add(observation.timestamp().atZone(zone).toLocalDate());
            }
        }
        return List.copyOf(days);
    }
}
