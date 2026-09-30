package com.mrdoodeth.meteo.model;

/**
 * Статистика конкретной метрики.
 *
 * @param min минимум
 * @param max максимум
 * @param average среднее арифметическое наблюдений
 * @param count количество наблюдений
 */
public record MetricStatistics(
        double min,
        double max,
        double average,
        long count) {

    /** Проверяет обязательные поля.
     * @throws IllegalArgumentException при несогласованных значениях статистики
     */
    public MetricStatistics {
        if (!Double.isFinite(min) || !Double.isFinite(max) || !Double.isFinite(average)
                || min > max || average < min || average > max || count <= 0) {
            throw new IllegalArgumentException("Invalid metric statistics");
        }
    }
}
