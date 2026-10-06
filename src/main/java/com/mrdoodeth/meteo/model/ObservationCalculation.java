package com.mrdoodeth.meteo.model;

import java.util.List;

/** Расчёт результата по предварительно отобранным метеонаблюдениям.
 * @param <R> тип результата расчёта
 */
public interface ObservationCalculation<R> {
    /** Вычисляет результат по наблюдениям.
     * @param observations наблюдения
     * @return результат расчёта
     */
    R calculate(List<Observation> observations);
}
