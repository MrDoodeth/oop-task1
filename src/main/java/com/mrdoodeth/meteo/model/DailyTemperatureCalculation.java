package com.mrdoodeth.meteo.model;

import java.util.Comparator;
import java.util.List;

/** Формирует суточный ход температуры по уже отобранному местному дню. */
public enum DailyTemperatureCalculation implements ObservationCalculation<List<TemperaturePoint>> {
    /** Единственный экземпляр расчёта суточного ряда. */
    INSTANCE;

    /** {@inheritDoc} */
    @Override
    public List<TemperaturePoint> calculate(List<Observation> observations) {
        return observations.stream()
                .map(observation -> new TemperaturePoint(observation.stationId(),
                        observation.timestamp(), observation.temperature()))
                .sorted(Comparator.comparing(TemperaturePoint::timestamp)
                        .thenComparing(TemperaturePoint::stationId))
                .toList();
    }
}
