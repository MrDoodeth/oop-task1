package com.mrdoodeth.meteo.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.function.ToDoubleFunction;

/** Расчёты статистики для трёх измеряемых показателей. */
public enum WeatherMetric implements ObservationCalculation<MetricStatistics> {
    TEMPERATURE(Observation::temperature),
    PRESSURE(Observation::pressureHpa),
    WIND_SPEED(Observation::windSpeed);

    private final ToDoubleFunction<Observation> metricFunction;

    WeatherMetric(ToDoubleFunction<Observation> metricFunction) {
        this.metricFunction = metricFunction;
    }

    /** {@inheritDoc}*/
    @Override
    public MetricStatistics calculate(List<Observation> observations) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        double sum = 0;
        for (var observation : observations) {
            double current = metricFunction.applyAsDouble(observation);
            min = Math.min(min, current);
            max = Math.max(max, current);
            sum += current;
        }
        double average = sum / observations.size();
        return new MetricStatistics(min, max, Math.clamp(average, min, max), observations.size());
    }
}
