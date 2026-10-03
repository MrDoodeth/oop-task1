package com.mrdoodeth.meteo.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModelTest {
    @ParameterizedTest
    @CsvSource({"0,NORTH", "22.499,NORTH", "22.5,NORTHEAST", "67.5,EAST",
            "112.5,SOUTHEAST", "157.5,SOUTH", "202.5,SOUTHWEST", "247.5,WEST",
            "292.5,NORTHWEST", "337.499,NORTHWEST", "337.5,NORTH", "359.999,NORTH"})
    void windSectorsIncludeLowerAndExcludeUpperBoundary(double azimuth, WindDirection expected) {
        assertEquals(expected, WindDirection.fromAzimuth(azimuth));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, 360, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void invalidAzimuthIsRejected(double azimuth) {
        assertThrows(IllegalArgumentException.class, () -> WindDirection.fromAzimuth(azimuth));
    }

    @Test
    void districtEqualityIsReflexiveSymmetricTransitiveAndConsistentWithHash() {
        var a = new District("d1", ZoneId.of("UTC"));
        var b = new District("d1", ZoneId.of("Europe/Moscow"));
        var c = new District("d1", ZoneId.of("Asia/Tokyo"));
        assertEquals(a, a);
        assertEquals(a, b);
        assertEquals(b, a);
        assertEquals(b, c);
        assertEquals(a, c);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, null);
        assertNotEquals(a, "d1");
        assertNotEquals(a, new District("d2", a.zone()));
        var set = new HashSet<District>();
        set.add(a);
        set.add(b);
        assertEquals(1, set.size());
    }

    @Test
    void stationIdentityUsesIdAcrossDistricts() {
        var a = new Station("s1", new District("d1", ZoneId.of("UTC")));
        var b = new Station("s1", new District("d2", ZoneId.of("Asia/Tokyo")));
        var c = new Station("s1", a.district());
        assertEquals(a, a);
        assertEquals(a, b);
        assertEquals(b, a);
        assertEquals(b, c);
        assertEquals(a, c);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, null);
        assertNotEquals(a, a.district());
        assertNotEquals(a, new Station("s2", a.district()));
        var map = new HashMap<Station, String>();
        map.put(a, "registered");
        assertEquals("registered", map.get(b));
    }

    @Test
    void identifiersAndRequiredReferencesAreValidated() {
        var zone = ZoneId.of("UTC");
        var district = new District("d1", zone);
        assertThrows(IllegalArgumentException.class, () -> new District(" ", zone));
        assertThrows(IllegalArgumentException.class, () -> new District(null, zone));
        assertThrows(NullPointerException.class, () -> new District("d1", null));
        assertThrows(IllegalArgumentException.class, () -> new Station("", district));
        assertThrows(NullPointerException.class, () -> new Station("s1", null));
        assertEquals(zone, new Station("s1", district).district().zone());
    }

    @Test
    void zeroWindAndNegativeTemperatureAreValid() {
        var observation = new Observation("s1", Instant.EPOCH, -30, 1000, 0,
                WindDirection.NORTH, WeatherPhenomenon.NONE);
        assertEquals(0, observation.windSpeed());
        assertEquals(-30, observation.temperature());
    }

    @Test
    void observationRejectsInvalidNumbersAndMissingObjects() {
        assertThrows(IllegalArgumentException.class, () -> observation(Double.NaN, 1000, 1));
        assertThrows(IllegalArgumentException.class, () -> observation(Double.POSITIVE_INFINITY, 1000, 1));
        assertThrows(IllegalArgumentException.class, () -> observation(10, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> observation(10, Double.POSITIVE_INFINITY, 1));
        assertThrows(IllegalArgumentException.class, () -> observation(10, 1000, -1));
        assertThrows(IllegalArgumentException.class, () -> observation(10, 1000, Double.NaN));
        assertThrows(NullPointerException.class, () -> new Observation("s1", null, 10, 1000, 1,
                WindDirection.NORTH, WeatherPhenomenon.NONE));
        assertThrows(NullPointerException.class, () -> new Observation("s1", Instant.EPOCH, 10, 1000, 1,
                null, WeatherPhenomenon.NONE));
        assertThrows(NullPointerException.class, () -> new Observation("s1", Instant.EPOCH, 10, 1000, 1,
                WindDirection.NORTH, null));
    }

    @Test
    void differentCalculationsWorkThroughTypedInterface() {
        var observation = observation(12, 1010, 3);
        var data = List.of(observation);
        ObservationCalculation<MetricStatistics> metric = WeatherMetric.TEMPERATURE;
        assertEquals(new MetricStatistics(12, 12, 12, 1), metric.calculate(data));
        metric = WeatherMetric.PRESSURE;
        assertEquals(1010, metric.calculate(data).average());
        metric = WeatherMetric.WIND_SPEED;
        assertEquals(3, metric.calculate(data).average());
        ObservationCalculation<List<TemperaturePoint>> series = DailyTemperatureCalculation.INSTANCE;
        assertEquals(List.of(new TemperaturePoint("s1", Instant.EPOCH, 12)), series.calculate(data));
        ObservationCalculation<List<java.time.LocalDate>> days =
                new PrecipitationDaysCalculation(ZoneId.of("UTC"));
        assertTrue(days.calculate(data).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> WeatherMetric.TEMPERATURE.calculate(List.of()));
        assertThrows(NullPointerException.class, () -> new PrecipitationDaysCalculation(null));
    }

    @Test
    void precipitationClassificationIsExplicit() {
        assertTrue(WeatherPhenomenon.RAIN.isPrecipitation());
        assertTrue(WeatherPhenomenon.SNOW.isPrecipitation());
        assertTrue(WeatherPhenomenon.HAIL.isPrecipitation());
        assertFalse(WeatherPhenomenon.THUNDERSTORM.isPrecipitation());
        assertFalse(WeatherPhenomenon.NONE.isPrecipitation());
        assertFalse(WeatherPhenomenon.SUNNY.isPrecipitation());
    }

    @Test
    void outputRecordsProtectTheirInvariants() {
        assertThrows(IllegalArgumentException.class, () -> new MetricStatistics(20, 10, 15, 1));
        assertThrows(IllegalArgumentException.class, () -> new MetricStatistics(10, 20, Double.NaN, 1));
        assertThrows(IllegalArgumentException.class, () -> new MetricStatistics(10, 20, 30, 1));
        assertThrows(IllegalArgumentException.class, () -> new MetricStatistics(10, 20, 15, 0));
        var single = new MetricStatistics(1, 1, 1, 1);
        var pair = new MetricStatistics(1, 1, 1, 2);
        assertThrows(IllegalArgumentException.class, () -> new DistrictStatistics(single, single, pair));
        assertThrows(NullPointerException.class, () -> new DistrictStatistics(null, single, single));
        assertThrows(IllegalArgumentException.class, () -> new TemperaturePoint("", Instant.EPOCH, 10));
        assertThrows(IllegalArgumentException.class, () -> new TemperaturePoint("s1", Instant.EPOCH, Double.NaN));
        assertThrows(NullPointerException.class, () -> new TemperaturePoint("s1", null, 10));
    }

    private Observation observation(double temperature, double pressure, double wind) {
        return new Observation("s1", Instant.EPOCH, temperature, pressure, wind,
                WindDirection.NORTH, WeatherPhenomenon.NONE);
    }
}
