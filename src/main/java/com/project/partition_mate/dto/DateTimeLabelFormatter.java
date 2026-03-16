package com.project.partition_mate.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeLabelFormatter {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private DateTimeLabelFormatter() {
    }

    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "미정";
        }
        return DATE_TIME_FORMATTER.format(dateTime);
    }
}
