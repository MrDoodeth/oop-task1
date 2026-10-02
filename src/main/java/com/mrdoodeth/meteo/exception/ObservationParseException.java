package com.mrdoodeth.meteo.exception;

import java.io.Serial;

/** Проверяемая ошибка загрузки с номером исходной строки. */
public class ObservationParseException extends Exception {
    /** Номер ошибочной строки исходного массива. */
    private final int lineNumber;

    /** Создаёт ошибку с номером строки от 1 и исходной причиной.
     * @param lineNumber номер строки с ошибкой, начиная с 1
     * @param reason объяснение ошибки
     * @param cause исходная причина
     */
    public ObservationParseException(int lineNumber, String reason, Throwable cause) {
        super("Invalid observation at line " + lineNumber + ": " + reason, cause);
        this.lineNumber = lineNumber;
    }

    /** Возвращает номер ошибочной строки.
     * @return номер строки, начиная с 1
     */
    public int lineNumber() {
        return lineNumber;
    }
}
