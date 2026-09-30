package com.mrdoodeth.meteo.model;

import java.util.Objects;

/** Статистика метрик по выборке района.
 * @param temperature температура по Цельсию
 * @param pressureHpa атмосферное давление в гПа
 * @param windSpeed скорость ветра в метрах в секунду
 */
public record DistrictStatistics(
        MetricStatistics temperature,
        MetricStatistics pressureHpa,
        MetricStatistics windSpeed) {
    /** Проверяет обязательные поля.
     * @throws NullPointerException при отсутствующей статистике любой из метрик
     * @throws IllegalArgumentException если показатели рассчитаны по выборкам разного размера
     */
    public DistrictStatistics {
        Objects.requireNonNull(temperature, "temperature");
        Objects.requireNonNull(pressureHpa, "pressureHpa");
        Objects.requireNonNull(windSpeed, "windSpeed");
        if (temperature.count() != pressureHpa.count() || temperature.count() != windSpeed.count()) {
            throw new IllegalArgumentException("Metrics must use the same sample size");
        }
    }
}
