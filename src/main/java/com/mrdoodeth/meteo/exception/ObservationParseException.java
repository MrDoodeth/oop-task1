package com.mrdoodeth.meteo.exception;

import java.io.Serial;

/** Исключение парсинга текста с номером исходной строки. */
public class ObservationParseException extends Exception {
    private final int lineNumber;

    /**
     * @param lineNumber номер строки с ошибкой, начиная с 1
     * @param cause исходная причина
     */
    public ObservationParseException(int lineNumber, Throwable cause) {
        super("Invalid observation at line " + lineNumber + ": " + cause.getMessage(), cause);
        this.lineNumber = lineNumber;
    }

    /** Возвращает номер ошибочной строки.
     * @return номер строки, начиная с 1
     */
    public int lineNumber() {
        return lineNumber;
    }
}
