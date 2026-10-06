package com.mrdoodeth.meteo;

import com.mrdoodeth.meteo.exception.ObservationParseException;
import com.mrdoodeth.meteo.model.Observation;
import com.mrdoodeth.meteo.model.WeatherPhenomenon;
import com.mrdoodeth.meteo.model.WindDirection;

import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Objects;

/** Разбирает строки наблюдений и загружает их в переданный журнал. */
public final class ObservationParser {
    private final WeatherJournal journal;

    /** Создаёт парсер, связанный с журналом.
     * @param journal журнал для загрузки наблюдений
     * @throws NullPointerException если журнал равен null
     */
    public ObservationParser(WeatherJournal journal) {
        this.journal = Objects.requireNonNull(journal, "journal");
    }

    /** Загружает строки формата
     * {@code stationId;ISO_OFFSET_DATE_TIME;temperatureC;pressureHpa;windSpeed;azimuth;PHENOMENON}.
     * @param lines строки наблюдений без заголовка
     * @throws ObservationParseException при ошибке строки, включая неизвестную станцию и дубликат;
     * @throws NullPointerException если сам массив равен null
     */
    public void parse(String[] lines) throws ObservationParseException {
        Objects.requireNonNull(lines, "lines");

        var parsed = new ArrayList<Observation>();

        for (int i = 0; i < lines.length; i++) {
            try {
                var observation = parseLine(lines[i]);
                journal.validateImportedObservation(observation, parsed);
                parsed.add(observation);
            } catch (IllegalArgumentException | DateTimeException exception) {
                throw new ObservationParseException(i + 1, exception);
            }
        }
        journal.addImportedObservations(parsed);
    }

    private Observation parseLine(String line) {
        if (line == null) {
            throw new IllegalArgumentException("Line must not be null");
        }
        var fields = line.split(";", -1);
        if (fields.length != 7) {
            throw new IllegalArgumentException("Expected 7 semicolon-separated fields");
        }
        for (int i = 0; i < fields.length; i++) {
            fields[i] = fields[i].trim();
        }
        return new Observation(
                fields[0],
                OffsetDateTime.parse(fields[1]).toInstant(),
                Double.parseDouble(fields[2]),
                Double.parseDouble(fields[3]),
                Double.parseDouble(fields[4]),
                WindDirection.fromAzimuth(Double.parseDouble(fields[5])),
                WeatherPhenomenon.valueOf(fields[6])
        );
    }
}
