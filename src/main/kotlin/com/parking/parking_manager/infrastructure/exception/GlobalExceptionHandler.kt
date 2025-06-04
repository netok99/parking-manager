package com.parking.parking_manager.infrastructure.exception

import com.parking.parking_manager.infrastructure.adapters.model.ApiErrorResponse
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<ApiErrorResponse> {
        logger.warn("Validation error: ${ex.message}")
        val errors = ex.bindingResult.fieldErrors.map { error -> "${error.field}: ${error.defaultMessage}" }
        return ResponseEntity
            .badRequest()
            .body(ApiErrorResponse(message = "Validation failed", errors = errors))
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(ex: ConstraintViolationException): ResponseEntity<ApiErrorResponse> {
        logger.warn("Constraint violation: ${ex.message}")
        val errors = ex.constraintViolations.map { violation -> "${violation.propertyPath}: ${violation.message}" }
        return ResponseEntity
            .badRequest()
            .body(ApiErrorResponse(message = "Validation failed", errors = errors))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(ex: HttpMessageNotReadableException): ResponseEntity<ApiErrorResponse> {
        logger.warn("Invalid JSON format: ${ex.message}")
        return ResponseEntity
            .badRequest()
            .body(ApiErrorResponse(message = "Invalid JSON format or missing required fields"))
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatchException(ex: MethodArgumentTypeMismatchException): ResponseEntity<ApiErrorResponse> {
        logger.warn("Type mismatch: ${ex.message}")
        return ResponseEntity
            .badRequest()
            .body(
                ApiErrorResponse(
                    message = "Invalid parameter type: ${ex.name} should be of type ${ex.requiredType?.simpleName}"
                )
            )
    }

    @ExceptionHandler(SpotNotFoundException::class)
    fun handleSpotNotFoundException(ex: SpotNotFoundException): ResponseEntity<ApiErrorResponse> {
        val message = "Spot not found"
        logger.warn("$message: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiErrorResponse(message = ex.message ?: "Spot not found"))
    }

    @ExceptionHandler(VehicleNotFoundException::class)
    fun handleVehicleNotFoundException(exception: VehicleNotFoundException): ResponseEntity<ApiErrorResponse> {
        val message = "Vehicle not found"
        logger.warn("$message: ${exception.message}")
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiErrorResponse(message = exception.message ?: message))
    }

    @ExceptionHandler(SectorNotFoundException::class)
    fun handleGenericException(exception: SectorNotFoundException): ResponseEntity<ApiErrorResponse> {
        val message = "Sector not found"
        logger.error(message, exception)
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiErrorResponse(message = exception.message ?: message))
    }

    @ExceptionHandler(SpotAlreadyOccupiedException::class)
    fun handleGenericException(exception: SpotAlreadyOccupiedException): ResponseEntity<ApiErrorResponse> {
        val message = "Spot already occupied"
        logger.error(message, exception)
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiErrorResponse(message = exception.message ?: message))
    }

    @ExceptionHandler(SectorFullException::class)
    fun handleGenericException(exception: SectorFullException): ResponseEntity<ApiErrorResponse> {
        val message = "Sector full"
        logger.error(message, exception)
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiErrorResponse(message = exception.message ?: message))
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(exception: Exception): ResponseEntity<ApiErrorResponse> {
        logger.error("Unexpected error: ", exception)
        return ResponseEntity
            .internalServerError()
            .body(
                ApiErrorResponse(
                    message = "Internal server error"
                )
            )
    }

    @ExceptionHandler(ParkingSessionNotFoundException::class)
    fun handleParkingSessionNotFoundException(
        exception: ParkingSessionNotFoundException
    ): ResponseEntity<ApiErrorResponse> {
        val message = "Parking session not found"
        logger.error("$message: ", exception)
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiErrorResponse(message = exception.message ?: message))
    }
}
