package com.mrdoodeth.meteo;

import com.mrdoodeth.meteo.model.District;
import com.mrdoodeth.meteo.model.DistrictStatistics;
import com.mrdoodeth.meteo.model.DailyTemperatureCalculation;
import com.mrdoodeth.meteo.model.Observation;
import com.mrdoodeth.meteo.model.ObservationCalculation;
import com.mrdoodeth.meteo.model.PrecipitationDaysCalculation;
import com.mrdoodeth.meteo.model.Station;
import com.mrdoodeth.meteo.model.TemperaturePoint;
import com.mrdoodeth.meteo.model.WeatherMetric;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/** Хранилище метеоданных в памяти и расчёты по районам. */
public final class WeatherJournal {
    private final Set<District> registeredDistricts = new HashSet<>();
    private final Set<Station> registeredStations = new HashSet<>();
    private final List<Observation> registeredObservations = new ArrayList<>();

    /** Создаёт пустой журнал. */
    public WeatherJournal() {
    }

    /** Регистрирует район.
     * @param district район с уникальным ID
     * @throws IllegalArgumentException если ID уже зарегистрирован
     */
    public void addDistrict(District district) {
        Objects.requireNonNull(district, "district");
        if (!registeredDistricts.add(district)) {
            throw new IllegalArgumentException("District already exists: " + district.id());
        }
    }

    /** Регистрирует станцию по ID района и его поясу.
     * Равная по данным копия района допускается; журнал сохраняет станцию со своим экземпляром района.
     * @param station станция с глобально уникальным ID
     * @throws IllegalArgumentException при повторном ID, неизвестном районе или несовпадении пояса
     */
    public void addStation(Station station) {
        Objects.requireNonNull(station, "station");
        var registeredDistrict = districtById(station.district().id());
        if (!registeredDistrict.zone().equals(station.district().zone())) {
            throw new IllegalArgumentException("District zone does not match registered district: "
                    + station.district().id());
        }
        if (!registeredStations.add(new Station(station.id(), registeredDistrict))) {
            throw new IllegalArgumentException("Station already exists: " + station.id());
        }
    }

    /** Добавляет наблюдение известной станции.
     * @param observation показание станции
     * @throws IllegalArgumentException если станция неизвестна или момент на ней уже занят
     */
    public void addObservation(Observation observation) {
        Objects.requireNonNull(observation, "observation");
        validateObservation(observation, registeredObservations);
        registeredObservations.add(observation);
    }

    /** Возвращает неизменяемый снимок множества станций.
     * @return зарегистрированные станции
     */
    public Set<Station> stations() {
        return Set.copyOf(registeredStations);
    }

    /** Возвращает статистику всех показаний района в [from, to), либо пустой Optional.
     * @param districtId ID района
     * @param from включённое начало
     * @param to исключённый конец
     * @return статистика или пустой Optional при отсутствии данных
     * @throws IllegalArgumentException при неизвестном районе или from не раньше to
     */
    public Optional<DistrictStatistics> statistics(String districtId, Instant from, Instant to) {
        districtById(districtId);
        validatePeriod(from, to);
        var selected = observationsInDistrict(districtId, from, to);
        if (selected.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new DistrictStatistics(
                calculate(selected, WeatherMetric.TEMPERATURE),
                calculate(selected, WeatherMetric.PRESSURE),
                calculate(selected, WeatherMetric.WIND_SPEED)
        ));
    }

    /** Возвращает неизменяемый суточный ряд станций района, отсортированный по времени и ID.
     * День определяется поясом района; значения разных станций не усредняются.
     * @param districtId ID района
     * @param day календарная дата района
     * @return отсортированные точки, либо пустой список
     * @throws IllegalArgumentException если район неизвестен
     */
    public List<TemperaturePoint> dailyTemperature(String districtId, LocalDate day) {
        var district = districtById(districtId);
        Objects.requireNonNull(day, "day");
        var from = day.atStartOfDay(district.zone()).toInstant();
        var to = day.plusDays(1).atStartOfDay(district.zone()).toInstant();
        return calculate(observationsInDistrict(districtId, from, to), DailyTemperatureCalculation.INSTANCE);
    }

    /** Возвращает уникальные отсортированные дни осадков среди показаний в [from, to).
     * @param districtId ID района
     * @param from включённое начало
     * @param to исключённый конец
     * @return неизменяемый список дат
     * @throws IllegalArgumentException при неизвестном районе или неверном периоде
     */
    public List<LocalDate> precipitationDays(String districtId, Instant from, Instant to) {
        var district = districtById(districtId);
        validatePeriod(from, to);
        return calculate(observationsInDistrict(districtId, from, to),
                new PrecipitationDaysCalculation(district.zone()));
    }

    /** Возвращает наблюдения района в один момент UTC, упорядоченные по ID станции.
     * @param districtId ID района
     * @param timestamp момент на общей шкале
     * @return неизменяемый список наблюдений
     * @throws IllegalArgumentException если район неизвестен
     */
    public List<Observation> observationsAt(String districtId, Instant timestamp) {
        districtById(districtId);
        Objects.requireNonNull(timestamp, "timestamp");
        return registeredObservations.stream()
                .filter(observation -> observation.timestamp().equals(timestamp))
                .filter(observation -> stationById(observation.stationId()).district().id().equals(districtId))
                .sorted(Comparator.comparing(Observation::stationId))
                .toList();
    }

    /** Ищет дни, где средняя температура отличается от заданной нормы строго больше порога.
     * Использует полные дни района в [fromDay, toDay), дни без наблюдений пропускает.
     * @param districtId ID района
     * @param fromDay первый включённый день
     * @param toDay первый исключённый день
     * @param normalTemperature заданная среднесуточная норма в градусах Цельсия
     * @param threshold конечный неотрицательный порог в градусах Цельсия
     * @return неизменяемый отсортированный список дат
     * @throws IllegalArgumentException при неверных параметрах или неизвестном районе
     */
    public List<LocalDate> abnormalDays(String districtId, LocalDate fromDay, LocalDate toDay,
                                        double normalTemperature, double threshold) {
        var district = districtById(districtId);
        Objects.requireNonNull(fromDay, "fromDay");
        Objects.requireNonNull(toDay, "toDay");
        if (!fromDay.isBefore(toDay)) {
            throw new IllegalArgumentException("Start day must be before end day");
        }
        if (!Double.isFinite(normalTemperature)) {
            throw new IllegalArgumentException("Normal temperature must be finite");
        }
        if (!Double.isFinite(threshold)) {
            throw new IllegalArgumentException("Threshold must be finite");
        }
        if (threshold < 0) {
            throw new IllegalArgumentException("Threshold must be non-negative");
        }

        var from = fromDay.atStartOfDay(district.zone()).toInstant();
        var to = toDay.atStartOfDay(district.zone()).toInstant();
        var byDay = new TreeMap<LocalDate, List<Observation>>();
        var neededObservations = observationsInDistrict(districtId, from, to);

        for (var observation : neededObservations) {
            var day = observation.timestamp().atZone(district.zone()).toLocalDate();
            byDay.computeIfAbsent(day, key -> new ArrayList<>()).add(observation);
        }

        var result = new ArrayList<LocalDate>();

        for (var entry : byDay.entrySet()) {
            var average = calculate(entry.getValue(), WeatherMetric.TEMPERATURE).average();
            var deviation = BigDecimal.valueOf(average).subtract(BigDecimal.valueOf(normalTemperature)).abs();
            if (deviation.compareTo(BigDecimal.valueOf(threshold)) > 0) {
                result.add(entry.getKey());
            }
        }

        return List.copyOf(result);
    }

    void validateImportedObservation(Observation observation, List<Observation> pending) {
        validateObservation(observation, registeredObservations);
        validateObservation(observation, pending);
    }

    void addImportedObservations(List<Observation> parsed) {
        registeredObservations.addAll(parsed);
    }

    private void validateObservation(Observation observation, List<Observation> existing) {
        stationById(observation.stationId());
        if (existing.stream().anyMatch(other -> other.stationId().equals(observation.stationId())
                && other.timestamp().equals(observation.timestamp()))) {
            throw new IllegalArgumentException("Observation already exists for station "
                    + observation.stationId() + " at " + observation.timestamp());
        }
    }

    private District districtById(String id) {
        return registeredDistricts.stream()
                .filter(district -> district.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown district: " + id));
    }

    private Station stationById(String id) {
        return registeredStations.stream()
                .filter(station -> station.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown station: " + id));
    }

    private List<Observation> observationsInDistrict(String districtId, Instant from, Instant to) {
        return registeredObservations.stream()
                .filter(observation -> stationById(observation.stationId()).district().id().equals(districtId))
                .filter(observation -> !observation.timestamp().isBefore(from)
                        && observation.timestamp().isBefore(to))
                .toList();
    }

    private static void validatePeriod(Instant from, Instant to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("Period start must be before period end");
        }
    }

    private static <R> R calculate(List<Observation> selected, ObservationCalculation<R> calculation) {
        return calculation.calculate(selected);
    }
}
