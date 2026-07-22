package com.meepleday.common

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

class NotFoundException(message: String) : RuntimeException(message)

class BadRequestException(message: String) : RuntimeException(message)

data class ApiError(
    val status: Int,
    val message: String,
    val fieldErrors: Map<String, String?> = emptyMap(),
    val timestamp: Instant = Instant.now(),
)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(ex: NotFoundException): ResponseEntity<ApiError> =
        build(HttpStatus.NOT_FOUND, ex.message ?: "Not found")

    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequest(ex: BadRequestException): ResponseEntity<ApiError> =
        build(HttpStatus.BAD_REQUEST, ex.message ?: "Bad request")

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val fieldErrors = ex.bindingResult.fieldErrors.associate { it.field to it.defaultMessage }
        return build(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors)
    }

    private fun build(
        status: HttpStatus,
        message: String,
        fieldErrors: Map<String, String?> = emptyMap(),
    ): ResponseEntity<ApiError> =
        ResponseEntity.status(status).body(ApiError(status.value(), message, fieldErrors))
}
