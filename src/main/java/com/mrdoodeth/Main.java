package com.mrdoodeth;

import com.mrdoodeth.meteo.WeatherJournal;
import com.mrdoodeth.meteo.model.District;
import com.mrdoodeth.meteo.model.MetricStatistics;
import com.mrdoodeth.meteo.model.Observation;
import com.mrdoodeth.meteo.model.Station;
import com.mrdoodeth.meteo.model.WeatherPhenomenon;
import com.mrdoodeth.meteo.model.WindDirection;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

public final class Main {
    public static void main(String[] args) {
        var journal = new WeatherJournal();
        var moscow = ZoneId.of("Europe/Moscow");
        var central = new District("d1", moscow);
        var northern = new District("d2", ZoneId.of("UTC"));
        var eastern = new District("d3", ZoneId.of("Asia/Yekaterinburg"));
        var western = new District("d4", ZoneId.of("Europe/Berlin"));
        var farEastern = new District("d5", ZoneId.of("Asia/Tokyo"));
        var districts = new District[] { central, northern, eastern, western, farEastern };
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
        var morning = Instant.parse("2026-09-29T06:00:00Z");
        var afternoon = Instant.parse("2026-09-29T12:00:00Z");
        var nextMorning = Instant.parse("2026-09-30T06:00:00Z");

        // По три наблюдения каждой станции: утро, день и утро следующего дня.
        journal.addObservation(new Observation("s1", morning, 10, 1012, 2, WindDirection.NORTH, WeatherPhenomenon.RAIN));
        journal.addObservation(new Observation("s1", afternoon, 15, 1010, 4, WindDirection.EAST, WeatherPhenomenon.CLOUDY));
        journal.addObservation(new Observation("s1", nextMorning, 18, 1009, 3, WindDirection.SOUTH, WeatherPhenomenon.SUNNY));
        journal.addObservation(new Observation("s2", morning, 9, 1013, 1, WindDirection.NORTHEAST, WeatherPhenomenon.CLOUDY));
        journal.addObservation(new Observation("s2", afternoon, 14, 1011, 3, WindDirection.SOUTHEAST, WeatherPhenomenon.RAIN));
        journal.addObservation(new Observation("s2", nextMorning, 17, 1010, 5, WindDirection.SOUTHWEST, WeatherPhenomenon.RAIN));
        journal.addObservation(new Observation("s3", morning, 11, 1012, 2, WindDirection.WEST, WeatherPhenomenon.SUNNY));
        journal.addObservation(new Observation("s3", afternoon, 16, 1011, 4, WindDirection.NORTHWEST, WeatherPhenomenon.CLOUDY));
        journal.addObservation(new Observation("s3", nextMorning, 19, 1008, 6, WindDirection.NORTH, WeatherPhenomenon.HAIL));

        journal.addObservation(new Observation("s4", morning, 2, 1020, 3, WindDirection.NORTH, WeatherPhenomenon.SNOW));
        journal.addObservation(new Observation("s4", afternoon, 5, 1019, 4, WindDirection.EAST, WeatherPhenomenon.CLOUDY));
        journal.addObservation(new Observation("s4", nextMorning, 4, 1021, 2, WindDirection.SOUTH, WeatherPhenomenon.FOG));
        journal.addObservation(new Observation("s5", morning, 1, 1021, 2, WindDirection.NORTHEAST, WeatherPhenomenon.SNOW));
        journal.addObservation(new Observation("s5", afternoon, 6, 1018, 5, WindDirection.SOUTHEAST, WeatherPhenomenon.SUNNY));
        journal.addObservation(new Observation("s5", nextMorning, 3, 1020, 3, WindDirection.SOUTHWEST, WeatherPhenomenon.RAIN));
        journal.addObservation(new Observation("s6", morning, 0, 1019, 4, WindDirection.WEST, WeatherPhenomenon.SNOW));
        journal.addObservation(new Observation("s6", afternoon, 4, 1017, 6, WindDirection.NORTHWEST, WeatherPhenomenon.CLOUDY));
        journal.addObservation(new Observation("s6", nextMorning, 2, 1018, 3, WindDirection.NORTH, WeatherPhenomenon.SNOW));

        journal.addObservation(new Observation("s7", morning, 5, 1015, 2, WindDirection.NORTH, WeatherPhenomenon.CLOUDY));
        journal.addObservation(new Observation("s7", afternoon, 10, 1013, 4, WindDirection.EAST, WeatherPhenomenon.RAIN));
        journal.addObservation(new Observation("s7", nextMorning, 8, 1014, 3, WindDirection.SOUTH, WeatherPhenomenon.FOG));
        journal.addObservation(new Observation("s8", morning, 4, 1016, 3, WindDirection.NORTHEAST, WeatherPhenomenon.RAIN));
        journal.addObservation(new Observation("s8", afternoon, 11, 1012, 5, WindDirection.SOUTHEAST, WeatherPhenomenon.SUNNY));
        journal.addObservation(new Observation("s8", nextMorning, 7, 1015, 2, WindDirection.SOUTHWEST, WeatherPhenomenon.CLOUDY));
        journal.addObservation(new Observation("s9", morning, 6, 1014, 2, WindDirection.WEST, WeatherPhenomenon.SUNNY));
        journal.addObservation(new Observation("s9", afternoon, 9, 1013, 4, WindDirection.NORTHWEST, WeatherPhenomenon.CLOUDY));
        journal.addObservation(new Observation("s9", nextMorning, 9, 1012, 3, WindDirection.NORTH, WeatherPhenomenon.RAIN));

        journal.addObservation(new Observation("s10", morning, 12, 1010, 2, WindDirection.NORTH, WeatherPhenomenon.RAIN));
        journal.addObservation(new Observation("s10", afternoon, 18, 1008, 3, WindDirection.EAST, WeatherPhenomenon.SUNNY));
        journal.addObservation(new Observation("s10", nextMorning, 15, 1009, 2, WindDirection.SOUTH, WeatherPhenomenon.CLOUDY));
        journal.addObservation(new Observation("s11", morning, 13, 1011, 3, WindDirection.NORTHEAST, WeatherPhenomenon.CLOUDY));
        journal.addObservation(new Observation("s11", afternoon, 19, 1007, 4, WindDirection.SOUTHEAST, WeatherPhenomenon.SUNNY));
        journal.addObservation(new Observation("s11", nextMorning, 14, 1010, 2, WindDirection.SOUTHWEST, WeatherPhenomenon.RAIN));
        journal.addObservation(new Observation("s12", morning, 11, 1009, 2, WindDirection.WEST, WeatherPhenomenon.FOG));
        journal.addObservation(new Observation("s12", afternoon, 17, 1008, 5, WindDirection.NORTHWEST, WeatherPhenomenon.CLOUDY));
        journal.addObservation(new Observation("s12", nextMorning, 16, 1009, 3, WindDirection.NORTH, WeatherPhenomenon.CLOUDY));

        journal.addObservation(new Observation("s13", morning, 20, 1008, 3, WindDirection.NORTH, WeatherPhenomenon.SUNNY));
        journal.addObservation(new Observation("s13", afternoon, 24, 1006, 4, WindDirection.EAST, WeatherPhenomenon.CLOUDY));
        journal.addObservation(new Observation("s13", nextMorning, 21, 1007, 2, WindDirection.SOUTH, WeatherPhenomenon.RAIN));
        journal.addObservation(new Observation("s14", morning, 19, 1009, 2, WindDirection.NORTHEAST, WeatherPhenomenon.RAIN));
        journal.addObservation(new Observation("s14", afternoon, 25, 1005, 5, WindDirection.SOUTHEAST, WeatherPhenomenon.SUNNY));
        journal.addObservation(new Observation("s14", nextMorning, 22, 1006, 3, WindDirection.SOUTHWEST, WeatherPhenomenon.CLOUDY));
        journal.addObservation(new Observation("s15", morning, 21, 1007, 3, WindDirection.WEST, WeatherPhenomenon.CLOUDY));
        journal.addObservation(new Observation("s15", afternoon, 23, 1006, 4, WindDirection.NORTHWEST, WeatherPhenomenon.SUNNY));
        journal.addObservation(new Observation("s15", nextMorning, 20, 1008, 2, WindDirection.NORTH, WeatherPhenomenon.RAIN));

        var from = Instant.parse("2026-09-29T00:00:00Z");
        var to = Instant.parse("2026-10-01T00:00:00Z");
        for (var district : districts) {
            var districtId = district.id();
            System.out.println("\nDistrict " + districtId + " (" + district.zone() + "):");
            journal.statistics(districtId, from, to).ifPresent(statistics -> {
                printMetric("Temperature (C)", statistics.temperature());
                printMetric("Pressure (hPa)", statistics.pressureHpa());
                printMetric("Wind speed (m/s)", statistics.windSpeed());
            });
            System.out.println("Precipitation days: " + journal.precipitationDays(districtId, from, to));
        }

        System.out.println("\nDaily temperature in district d1 on 2026-09-29 (Europe/Moscow):");
        for (var point : journal.dailyTemperature("d1", LocalDate.of(2026, 9, 29))) {
            System.out.printf(Locale.ROOT, "%s %s: %.1f C%n",
                    point.timestamp().atZone(moscow), point.stationId(), point.temperature());
        }

        System.out.println("\nSame instant, stations in different district time zones:");
        for (var district : districts) {
            var observation = journal.observationsAt(district.id(), morning).getFirst();
            System.out.println(observation.stationId() + ": " + observation.timestamp().atZone(district.zone())
                    + " = " + observation.timestamp());
        }
        System.out.println("\nDays differing from 10 C by more than 3 C: "
                + journal.abnormalDays("d1", LocalDate.of(2026, 9, 29),
                LocalDate.of(2026, 10, 1), 10, 3));
    }

    private static void printMetric(String name, MetricStatistics statistics) {
        System.out.printf(Locale.ROOT, "%s: min=%.1f, max=%.1f, average=%.2f (n=%d)%n",
                name, statistics.min(), statistics.max(), statistics.average(), statistics.count());
    }
}
