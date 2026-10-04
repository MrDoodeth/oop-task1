package com.mrdoodeth;

import com.mrdoodeth.meteo.exception.ObservationParseException;
import com.mrdoodeth.meteo.ObservationParser;
import com.mrdoodeth.meteo.WeatherJournal;
import com.mrdoodeth.meteo.model.District;
import com.mrdoodeth.meteo.model.MetricStatistics;
import com.mrdoodeth.meteo.model.Station;
import com.mrdoodeth.meteo.model.WeatherPhenomenon;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Locale;

/** Консольная демонстрация метеожурнала. */
public final class Main {
    private Main() {
    }

    /** Запускает демонстрационный сценарий.
     * @param args аргументы командной строки (не используются)
     */
    public static void main(String[] args) {
        var journal = new WeatherJournal();
        var moscow = ZoneId.of("Europe/Moscow");
        var central = new District("d1", moscow);
        var northern = new District("d2", ZoneId.of("UTC"));
        var eastern = new District("d3", ZoneId.of("Asia/Yekaterinburg"));
        var western = new District("d4", ZoneId.of("Europe/Berlin"));
        var farEastern = new District("d5", ZoneId.of("Asia/Tokyo"));
        var districts = new District[] {central, northern, eastern, western, farEastern};
        var stationIds = new String[][] {
                {"s1", "s2", "s3"}, {"s4", "s5", "s6"}, {"s7", "s8", "s9"},
                {"s10", "s11", "s12"}, {"s13", "s14", "s15"}
        };
        for (int i = 0; i < districts.length; i++) {
            journal.addDistrict(districts[i]);
            for (var stationId : stationIds[i]) {
                journal.addStation(new Station(stationId, districts[i]));
            }
        }
        var parser = new ObservationParser(journal);
        var lines = sampleObservations(districts, stationIds);

        try {
            parser.parse(lines);
            System.out.println("Loaded " + lines.length + " observations from "
                    + journal.stations().size() + " stations in " + districts.length + " districts.");
        } catch (ObservationParseException exception) {
            System.err.println(exception.getMessage());
            return;
        }

        var from = Instant.parse("2026-09-28T00:00:00Z");
        var to = Instant.parse("2026-10-03T00:00:00Z");
        for (var district : districts) {
            var districtId = district.id();
            System.out.println("\nDistrict " + districtId + ": statistics for [" + from + ", " + to + ")");
            journal.statistics(districtId, from, to).ifPresentOrElse(statistics -> {
                printMetric("Temperature (C)", statistics.temperature());
                printMetric("Pressure (hPa)", statistics.pressureHpa());
                printMetric("Wind speed (m/s)", statistics.windSpeed());
            }, () -> System.out.println("No observations in this period"));
            System.out.println("Precipitation days: " + journal.precipitationDays(districtId, from, to));
        }

        System.out.println("\nDaily temperature in district d1 on 2026-09-29 (Europe/Moscow):");
        for (var point : journal.dailyTemperature("d1", LocalDate.of(2026, 9, 29))) {
            System.out.printf(Locale.ROOT, "%s %s: %.1f C%n",
                    point.timestamp().atZone(moscow), point.stationId(), point.temperature());
        }

        System.out.println("\nSame instant, stations in different district time zones:");
        var sameMoment = Instant.parse("2026-09-29T06:00:00Z");
        for (var district : districts) {
            var observation = journal.observationsAt(district.id(), sameMoment).getFirst();
            System.out.println(observation.stationId() + ": " + observation.timestamp().atZone(district.zone())
                    + " = " + observation.timestamp());
        }
        System.out.println("\nDays differing from 10 C by more than 3 C: "
                + journal.abnormalDays("d1", LocalDate.of(2026, 9, 29),
                LocalDate.of(2026, 10, 2), 10, 3));

        try {
            parser.parse(new String[] {
                    "s1;2026-10-03T06:00:00Z;12;1012;2;90;RAIN",
                    "s1;2026-10-03T12:00:00Z;bad;1010;2;90;RAIN"
            });
        } catch (ObservationParseException exception) {
            System.out.println("\nImport rejected: " + exception.getMessage());
            System.out.println("No rows from the failed import were added.");
        }
        System.out.println("Failed import left the new period empty: "
                + journal.statistics("d1", Instant.parse("2026-10-03T00:00:00Z"),
                Instant.parse("2026-10-04T00:00:00Z")).isEmpty());
    }

    private static String[] sampleObservations(District[] districts, String[][] stationIds) {
        var lines = new ArrayList<String>();
        var firstDay = LocalDate.of(2026, 9, 28);
        var typicalTemperatures = new int[] {9, 4, 6, 13, 18};
        var utcHours = new int[] {6, 12, 18};
        for (int districtIndex = 0; districtIndex < districts.length; districtIndex++) {
            for (int dayIndex = 0; dayIndex < 4; dayIndex++) {
                for (int hourIndex = 0; hourIndex < utcHours.length; hourIndex++) {
                    var instant = firstDay.plusDays(dayIndex).atTime(utcHours[hourIndex], 0)
                            .toInstant(ZoneOffset.UTC);
                    var localTime = instant.atZone(districts[districtIndex].zone()).toOffsetDateTime();
                    for (int stationIndex = 0; stationIndex < stationIds[districtIndex].length; stationIndex++) {
                        int temperature = typicalTemperatures[districtIndex] + dayIndex * 2
                                + stationIndex - 1 + (hourIndex - 1) * 2;
                        int pressure = 1007 + districtIndex * 3 + dayIndex + hourIndex;
                        int windSpeed = 1 + (districtIndex + stationIndex + dayIndex + hourIndex) % 7;
                        int azimuth = (districtIndex * 45 + stationIndex * 45 + dayIndex * 30
                                + hourIndex * 90) % 360;
                        var phenomenon = samplePhenomenon(temperature, districtIndex,
                                dayIndex, hourIndex, stationIndex);
                        lines.add(String.format(Locale.ROOT, "%s;%s;%d;%d;%d;%d;%s",
                                stationIds[districtIndex][stationIndex], localTime, temperature,
                                pressure, windSpeed, azimuth, phenomenon));
                    }
                }
            }
        }
        return lines.toArray(String[]::new);
    }

    private static WeatherPhenomenon samplePhenomenon(int temperature, int district,
                                                     int day, int hour, int station) {
        return switch ((day + hour + station) % 6) {
            case 0 -> WeatherPhenomenon.RAIN;
            case 1 -> temperature <= 2 ? WeatherPhenomenon.SNOW : WeatherPhenomenon.CLOUDY;
            case 2 -> WeatherPhenomenon.SUNNY;
            case 3 -> WeatherPhenomenon.FOG;
            case 4 -> district == 0 ? WeatherPhenomenon.HAIL : WeatherPhenomenon.THUNDERSTORM;
            default -> WeatherPhenomenon.NONE;
        };
    }

    private static void printMetric(String name, MetricStatistics statistics) {
        System.out.printf(Locale.ROOT, "%s: min=%.1f, max=%.1f, average=%.2f (n=%d)%n",
                name, statistics.min(), statistics.max(), statistics.average(), statistics.count());
    }
}
