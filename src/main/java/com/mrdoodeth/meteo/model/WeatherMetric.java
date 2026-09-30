package com.mrdoodeth.meteo.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.function.ToDoubleFunction;

/** Расчёты статистики для трёх измеряемых показателей. */
public enum WeatherMetric implements ObservationCalculation<MetricStatistics> {
    /** Статистика температуры в градусах Цельсия. */
    TEMPERATURE(Observation::temperature),
    /** Статистика давления в гектопаскалях. */
    PRESSURE(Observation::pressureHpa),
    /** Статистика скорости ветра в метрах в секунду. */
    WIND_SPEED(Observation::windSpeed);

    private final ToDoubleFunction<Observation> metricFunction;

    WeatherMetric(ToDoubleFunction<Observation> metricFunction) {
        this.metricFunction = metricFunction;
    }

    /** {@inheritDoc}
     * @throws IllegalArgumentException если выборка пуста
     */
    @Override
    public MetricStatistics calculate(List<Observation> observations) {
        if (observations.isEmpty()) {
            throw new IllegalArgumentException("Cannot calculate statistics for an empty sample");
        }
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        var sum = BigDecimal.ZERO;
        for (var observation : observations) {
            double current = metricFunction.applyAsDouble(observation);
            min = Math.min(min, current);
            max = Math.max(max, current);
            sum = sum.add(BigDecimal.valueOf(current));
        }
        double average = sum.divide(BigDecimal.valueOf(observations.size()), MathContext.DECIMAL128)
                .doubleValue();
        return new MetricStatistics(min, max, Math.clamp(average, min, max), observations.size());
    }
}
