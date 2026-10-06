package com.mrdoodeth.meteo;

import com.mrdoodeth.meteo.exception.ObservationParseException;
import com.mrdoodeth.meteo.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class WeatherJournalTest {
    private static final Instant START = Instant.parse("2026-09-29T00:00:00Z");
    private static final Instant END = Instant.parse("2026-09-30T00:00:00Z");
    private WeatherJournal journal;
    private ObservationParser parser;
    private District district;

    @BeforeEach
    void setup() {
        journal = new WeatherJournal();
        parser = new ObservationParser(journal);
        district = new District("d1", ZoneId.of("UTC"));
        var other = new District("d2", ZoneId.of("UTC"));
        journal.addDistrict(district);
        journal.addDistrict(other);
        journal.addStation(new Station("s1", district));
        journal.addStation(new Station("s2", district));
        journal.addStation(new Station("s3", other));
    }

    private Observation observation(String station, Instant time, double temperature) {
        return new Observation(station, time, temperature, 1000, 2, WindDirection.NORTH, WeatherPhenomenon.NONE);
    }

    @Test
    void statisticsCoverAllMetricsAndExcludeOtherDistrict() throws Exception {
        parser.parse(new String[] {
                "s1;2026-09-29T00:00:00Z;10;1000;0;0;NONE",
                "s2;2026-09-29T01:00:00Z;20;1020;4;90;RAIN",
                "s3;2026-09-29T02:00:00Z;100;1100;80;180;SNOW"
        });
        var result = journal.statistics("d1", START, END).orElseThrow();
        assertEquals(new MetricStatistics(10, 20, 15, 2), result.temperature());
        assertEquals(new MetricStatistics(1000, 1020, 1010, 2), result.pressureHpa());
        assertEquals(new MetricStatistics(0, 4, 2, 2), result.windSpeed());
    }

    @Test
    void periodIncludesStartButExcludesEnd() {
        journal.addObservation(observation("s1", START.minusSeconds(1), -100));
        journal.addObservation(observation("s1", START, 10));
        journal.addObservation(observation("s1", END, 100));
        assertEquals(new MetricStatistics(10, 10, 10, 1),
                journal.statistics("d1", START, END).orElseThrow().temperature());
    }

    @Test
    void emptyQueriesReturnEmptyResults() {
        assertTrue(journal.statistics("d1", START, END).isEmpty());
        assertTrue(journal.dailyTemperature("d1", LocalDate.of(2026, 9, 29)).isEmpty());
        assertTrue(journal.precipitationDays("d1", START, END).isEmpty());
        assertTrue(journal.observationsAt("d1", START).isEmpty());
    }

    @Test
    void invalidPeriodsAndUnknownDistrictAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> journal.statistics("d1", START, START));
        assertThrows(IllegalArgumentException.class, () -> journal.statistics("d1", END, START));
        assertThrows(IllegalArgumentException.class, () -> journal.precipitationDays("d1", END, START));
        assertThrows(IllegalArgumentException.class, () -> journal.statistics("missing", START, END));
    }

    @Test
    void stationRegistrySnapshotIsImmutableAndDetached() {
        var snapshot = journal.stations();
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        journal.addStation(new Station("s4", district));
        assertEquals(3, snapshot.size());
        assertEquals(4, journal.stations().size());
    }

    @Test
    void duplicateIdsAndUnknownOrConflictingDistrictAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> journal.addDistrict(new District("d1", ZoneId.of("Asia/Tokyo"))));
        assertThrows(IllegalArgumentException.class,
                () -> journal.addStation(new Station("s1", district)));
        assertThrows(IllegalArgumentException.class,
                () -> journal.addStation(new Station("s1", new District("d2", district.zone()))));
        assertThrows(IllegalArgumentException.class,
                () -> journal.addStation(new Station("s4", new District("unknown", district.zone()))));
        assertThrows(IllegalArgumentException.class,
                () -> journal.addStation(new Station("s4", new District("d1", ZoneId.of("Asia/Tokyo")))));
        assertEquals(3, journal.stations().size());
    }

    @Test
    void equalDistrictCopyIsAcceptedAndInputStationIsStored() {
        var copy = new District("d1", district.zone());
        assertNotSame(district, copy);
        var station = new Station("s4", copy);
        journal.addStation(station);
        var registered = journal.stations().stream()
                .filter(value -> value.id().equals("s4"))
                .findFirst().orElseThrow();
        assertSame(station, registered);
        assertSame(copy, registered.district());
        assertEquals(district, registered.district());
        journal.addObservation(observation("s4", START, 8));
        assertEquals(8, journal.statistics("d1", START, END).orElseThrow().temperature().average());
    }

    @Test
    void unknownStationAndDuplicateMomentAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> journal.addObservation(observation("missing", START, 10)));
        journal.addObservation(observation("s1", START, 10));
        assertThrows(IllegalArgumentException.class,
                () -> journal.addObservation(observation("s1", START, 20)));
        journal.addObservation(observation("s2", START, 20));
        assertEquals(2, journal.observationsAt("d1", START).size());
    }

    @Test
    void dailySeriesIsSortedAndImmutable() {
        journal.addObservation(observation("s1", START.plusSeconds(3600), 20));
        journal.addObservation(observation("s2", START, 15));
        journal.addObservation(observation("s1", START, 10));
        var points = journal.dailyTemperature("d1", LocalDate.of(2026, 9, 29));
        assertEquals(new TemperaturePoint("s1", START, 10), points.get(0));
        assertEquals(new TemperaturePoint("s2", START, 15), points.get(1));
        assertEquals(START.plusSeconds(3600), points.get(2).timestamp());
        assertThrows(UnsupportedOperationException.class, points::clear);
    }

    @Test
    void districtDayUsesDistrictZoneRatherThanUtc() {
        var east = new District("east", ZoneId.of("Asia/Tokyo"));
        journal.addDistrict(east);
        journal.addStation(new Station("s4", east));
        journal.addObservation(observation("s4", Instant.parse("2026-09-28T15:00:00Z"), 10));
        journal.addObservation(observation("s4", Instant.parse("2026-09-29T15:00:00Z"), 20));
        var points = journal.dailyTemperature("east", LocalDate.of(2026, 9, 29));
        assertEquals(1, points.size());
        assertEquals(10, points.getFirst().temperature());
    }

    @Test
    void daylightSavingDayHasCorrectEndBoundary() {
        var berlin = new District("berlin", ZoneId.of("Europe/Berlin"));
        journal.addDistrict(berlin);
        journal.addStation(new Station("s4", berlin));
        journal.addObservation(observation("s4", Instant.parse("2026-03-28T23:00:00Z"), 1));
        journal.addObservation(observation("s4", Instant.parse("2026-03-29T21:59:59Z"), 2));
        journal.addObservation(observation("s4", Instant.parse("2026-03-29T22:00:00Z"), 3));
        assertEquals(2, journal.dailyTemperature("berlin", LocalDate.of(2026, 3, 29)).size());
    }

    @Test
    void precipitationDaysAreSortedUniqueAndFiltered() throws Exception {
        parser.parse(new String[] {
                "s1;2026-09-30T01:00:00Z;5;1000;2;0;SNOW",
                "s1;2026-09-29T02:00:00Z;10;1000;2;0;RAIN",
                "s2;2026-09-29T03:00:00Z;10;1000;2;0;HAIL",
                "s1;2026-10-01T03:00:00Z;10;1000;2;0;THUNDERSTORM",
                "s3;2026-09-28T03:00:00Z;10;1000;2;0;RAIN"
        });
        assertEquals(List.of(LocalDate.of(2026, 9, 29), LocalDate.of(2026, 9, 30)),
                journal.precipitationDays("d1", START.minusSeconds(86400), END.plusSeconds(172800)));
    }

    @Test
    void offsetTimesIdentifyTheSameInstant() throws Exception {
        parser.parse(new String[] {
                "s1;2026-09-29T03:00:00+03:00;10;1000;2;0;NONE",
                "s2;2026-09-29T05:00:00+05:00;20;1000;2;0;NONE"
        });
        var sameTime = journal.observationsAt("d1", START);
        assertEquals(2, sameTime.size());
        assertEquals(sameTime.get(0).timestamp(), sameTime.get(1).timestamp());
        assertThrows(UnsupportedOperationException.class, sameTime::clear);
    }

    @Test
    void parseFailureContainsLineAndCauseAndIsAtomic() {
        journal.addObservation(observation("s1", START.minusSeconds(1), 1));
        var exception = assertThrows(ObservationParseException.class, () -> parser.parse(new String[] {
                "s1;2026-09-29T00:00:00Z;10;1000;2;0;NONE",
                "s2;2026-09-29T00:00:00Z;bad;1000;2;0;NONE"
        }));
        assertEquals(2, exception.lineNumber());
        assertTrue(exception.getMessage().contains("line 2"));
        assertInstanceOf(NumberFormatException.class, exception.getCause());
        assertTrue(journal.statistics("d1", START, END).isEmpty());
        assertEquals(1, journal.observationsAt("d1", START.minusSeconds(1)).size());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "s1;date", "s1;2026-09-29T00:00:00Z;10;1000;2;0;",
            "s1;2026-09-29T00:00:00;10;1000;2;0;NONE",
            "s1;2026-09-29T00:00:00Z;NaN;1000;2;0;NONE",
            "s1;2026-09-29T00:00:00Z;10;0;2;0;NONE",
            "s1;2026-09-29T00:00:00Z;10;1000;-1;0;NONE",
            "s1;2026-09-29T00:00:00Z;10;1000;2;NaN;NONE",
            "s1;2026-09-29T00:00:00Z;10;1000;2;360;NONE",
            "s1;2026-09-29T00:00:00Z;10;1000;2;0;UNKNOWN",
            "missing;2026-09-29T00:00:00Z;10;1000;2;0;NONE"
    })
    void malformedRowsUseCustomException(String line) {
        assertEquals(1, assertThrows(ObservationParseException.class,
                () -> parser.parse(new String[] {line})).lineNumber());
    }

    @Test
    void nullRowIsReportedWithItsNumber() {
        assertEquals(1, assertThrows(ObservationParseException.class,
                () -> parser.parse(new String[] {null})).lineNumber());
        assertThrows(NullPointerException.class, () -> parser.parse(null));
        assertThrows(NullPointerException.class, () -> new ObservationParser(null));
    }

    @Test
    void duplicateRowsWithinImportAndAgainstJournalAreRejected() throws Exception {
        var line = "s1;2026-09-29T00:00:00Z;10;1000;2;0;NONE";
        assertEquals(2, assertThrows(ObservationParseException.class,
                () -> parser.parse(new String[] {line, line})).lineNumber());
        assertTrue(journal.observationsAt("d1", START).isEmpty());
        parser.parse(new String[] {line});
        assertEquals(1, assertThrows(ObservationParseException.class,
                () -> parser.parse(new String[] {line})).lineNumber());
    }

    @Test
    void whitespaceIsTrimmedAndEmptyArrayIsAllowed() throws Exception {
        parser.parse(new String[0]);
        parser.parse(new String[] {" s1 ; 2026-09-29T00:00:00Z ; 10 ; 1000 ; 2 ; 0 ; NONE "});
        assertEquals(1, journal.observationsAt("d1", START).size());
    }

    @Test
    void abnormalDaysUseDailyMeanAndStrictThreshold() {
        journal.addObservation(observation("s1", START, 10));
        journal.addObservation(observation("s2", START, 20));
        journal.addObservation(observation("s1", END, 16));
        journal.addObservation(observation("s2", END, 18));
        journal.addObservation(observation("s1", END.plusSeconds(86400), 100));
        assertEquals(List.of(LocalDate.of(2026, 9, 30)),
                journal.abnormalDays("d1", LocalDate.of(2026, 9, 29), LocalDate.of(2026, 10, 1), 10, 5));
        assertTrue(journal.abnormalDays("d2", LocalDate.of(2026, 9, 29),
                LocalDate.of(2026, 10, 1), 10, 0).isEmpty());
    }

    @Test
    void invalidNormAndThresholdAreRejected() {
        var day = LocalDate.of(2026, 9, 29);
        assertThrows(IllegalArgumentException.class, () -> journal.abnormalDays("d1", day, day, 10, 1));
        assertThrows(IllegalArgumentException.class, () -> journal.abnormalDays("d1", day, day.plusDays(1), 10, -1));
        assertThrows(IllegalArgumentException.class, () -> journal.abnormalDays("d1", day, day.plusDays(1), Double.NaN, 1));
        assertThrows(IllegalArgumentException.class, () -> journal.abnormalDays("d1", day, day.plusDays(1), 10, Double.POSITIVE_INFINITY));
    }

    @Test
    void averageOfLargeFiniteValuesDoesNotOverflow() {
        journal.addObservation(observation("s1", START, Double.MAX_VALUE));
        journal.addObservation(observation("s2", START, Double.MAX_VALUE));
        assertEquals(Double.MAX_VALUE,
                journal.statistics("d1", START, END).orElseThrow().temperature().average());
    }

    @Test
    void repeatedLocalHourContainsTwoDistinctInstants() throws Exception {
        var berlin = new District("berlin", ZoneId.of("Europe/Berlin"));
        journal.addDistrict(berlin);
        journal.addStation(new Station("s4", berlin));
        parser.parse(new String[] {
                "s4;2026-10-25T02:30:00+02:00;10;1000;2;0;RAIN",
                "s4;2026-10-25T02:30:00+01:00;12;1000;2;0;RAIN",
                "s4;2026-10-25T23:00:00Z;14;1000;2;0;NONE"
        });
        var points = journal.dailyTemperature("berlin", LocalDate.of(2026, 10, 25));
        assertEquals(2, points.size());
        assertEquals(3600, points.get(1).timestamp().getEpochSecond() - points.get(0).timestamp().getEpochSecond());
    }

    @Test
    void alternateOffsetsCannotBypassDuplicateDetection() {
        var exception = assertThrows(ObservationParseException.class, () -> parser.parse(new String[] {
                "s1;2026-09-29T03:00:00+03:00;10;1000;2;0;NONE",
                "s1;2026-09-29T00:00:00Z;12;1000;2;0;NONE"
        }));
        assertEquals(2, exception.lineNumber());
        assertTrue(journal.observationsAt("d1", START).isEmpty());
    }

    @Test
    void coldAbnormalDaysAndPrecipitationUseLocalDate() throws Exception {
        var east = new District("east", ZoneId.of("Asia/Tokyo"));
        journal.addDistrict(east);
        journal.addStation(new Station("s4", east));
        parser.parse(new String[] {"s4;2026-09-28T16:00:00Z;2;1000;2;0;RAIN"});
        var day = LocalDate.of(2026, 9, 29);
        assertEquals(List.of(day), journal.abnormalDays("east", day, day.plusDays(1), 10, 5));
        assertEquals(List.of(day), journal.precipitationDays("east", START.minusSeconds(86400), END));
        assertTrue(journal.abnormalDays("east", day, day.plusDays(1), 2, 0).isEmpty());
    }

    @Test
    void inputArrayIsNotRetained() throws Exception {
        var lines = new String[] {"s1;2026-09-29T00:00:00Z;10;1000;2;0;NONE"};
        parser.parse(lines);
        lines[0] = "broken";
        assertEquals(10, journal.observationsAt("d1", START).getFirst().temperature());
    }

    @Test
    void stationsInDifferentDistrictZonesShareOneInstant() throws Exception {
        var east = new District("east", ZoneId.of("Asia/Yekaterinburg"));
        journal.addDistrict(east);
        var station = new Station("s4", east);
        journal.addStation(station);
        parser.parse(new String[] {
                "s1;2026-09-29T00:00:00Z;10;1000;2;0;NONE",
                "s4;2026-09-29T05:00:00+05:00;12;1000;2;0;NONE"
        });
        var first = journal.observationsAt("d1", START).getFirst();
        var second = journal.observationsAt("east", START).getFirst();
        assertEquals(first.timestamp(), second.timestamp());
        assertEquals(0, first.timestamp().atZone(district.zone()).getHour());
        assertEquals(5, second.timestamp().atZone(station.district().zone()).getHour());
    }

    @Test
    void stationsInSameDistrictShareCalendarDespiteDifferentInputOffsets() throws Exception {
        journal.addStation(new Station("s4", district));
        parser.parse(new String[] {
                "s1;2026-09-29T00:00:00Z;10;1000;2;0;RAIN",
                "s4;2026-09-29T09:00:00+09:00;12;1000;2;0;SNOW"
        });
        assertEquals(2, journal.observationsAt("d1", START).size());
        assertEquals(district.zone(), journal.stations().stream()
                .filter(station -> station.id().equals("s4")).findFirst().orElseThrow().district().zone());
        assertEquals(0, START.atZone(district.zone()).getHour());
        assertEquals(2, journal.dailyTemperature("d1", LocalDate.of(2026, 9, 29)).size());
        assertEquals(List.of(LocalDate.of(2026, 9, 29)), journal.precipitationDays("d1", START, END));
    }

    @Test
    void importedObservationsAcrossFiveDistrictsWithThreeStationsEachStayIndependent() throws Exception {
        var populated = new WeatherJournal();
        var zones = new String[] {"UTC", "Europe/Moscow", "Asia/Yekaterinburg",
                "Europe/Berlin", "Asia/Tokyo"};
        var rows = new ArrayList<String>();
        for (int districtIndex = 0; districtIndex < zones.length; districtIndex++) {
            var area = new District("area" + districtIndex, ZoneId.of(zones[districtIndex]));
            populated.addDistrict(area);
            for (int stationIndex = 0; stationIndex < 3; stationIndex++) {
                var stationId = "area" + districtIndex + "-station" + stationIndex;
                populated.addStation(new Station(stationId, area));
                for (int hourIndex = 0; hourIndex < 4; hourIndex++) {
                    var time = START.plusSeconds(hourIndex * 6L * 3600)
                            .atZone(area.zone()).toOffsetDateTime();
                    rows.add(String.format(Locale.ROOT, "%s;%s;%d;1000;2;90;%s", stationId,
                            time, districtIndex * 10 + stationIndex + hourIndex,
                            hourIndex == 1 ? "RAIN" : "CLOUDY"));
                }
            }
        }
        new ObservationParser(populated).parse(rows.toArray(String[]::new));

        assertEquals(15, populated.stations().size());
        assertEquals(60, rows.size());
        for (int districtIndex = 0; districtIndex < zones.length; districtIndex++) {
            var districtId = "area" + districtIndex;
            var temperature = populated.statistics(districtId, START, END).orElseThrow().temperature();
            assertEquals(new MetricStatistics(districtIndex * 10, districtIndex * 10 + 5,
                    districtIndex * 10 + 2.5, 12), temperature);
            assertEquals(3, populated.observationsAt(districtId, START).size());
            assertEquals(List.of(LocalDate.of(2026, 9, 29)),
                    populated.precipitationDays(districtId, START, END));
        }
    }

    @Test
    void thunderstormAloneDoesNotCountAsPrecipitation() {
        journal.addObservation(new Observation("s1", START, 10, 1000, 2,
                WindDirection.NORTH, WeatherPhenomenon.THUNDERSTORM));
        assertTrue(journal.precipitationDays("d1", START, END).isEmpty());
    }
}
