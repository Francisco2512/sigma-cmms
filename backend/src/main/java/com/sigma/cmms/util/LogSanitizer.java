package com.sigma.cmms.util;

import lombok.experimental.UtilityClass;

/** Evita la inyeccion de lineas falsas en los logs a partir de datos del usuario. */
@UtilityClass
public class LogSanitizer {

    private static final int MAX_LENGTH = 100;

    public static String clean(String value) {
        if (value == null) {
            return "";
        }
        String single = value.replaceAll("[\r\n\t]", "_");
        return single.length() > MAX_LENGTH ? single.substring(0, MAX_LENGTH) : single;
    }
}
