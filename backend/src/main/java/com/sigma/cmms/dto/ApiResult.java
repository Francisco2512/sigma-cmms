package com.sigma.cmms.dto;

import java.time.Instant;

/**
 * Envoltura estandar de las respuestas exitosas de la API.
 *
 * @param <T> tipo del contenido
 */
public record ApiResult<T>(boolean success, T data, String message, Instant timestamp) {

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(true, data, null, Instant.now());
    }

    public static <T> ApiResult<T> ok(T data, String message) {
        return new ApiResult<>(true, data, message, Instant.now());
    }
}
