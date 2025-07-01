package uk.gov.justice.digital.hmpps.hmppsworkload.config

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import uk.gov.justice.digital.hmpps.hmppsworkload.client.WorkloadFailedDependencyException
import uk.gov.justice.digital.hmpps.hmppsworkload.client.WorkloadWebClientTimeoutException

@RestControllerAdvice
class HmppsWorkloadExceptionHandler {
  @ExceptionHandler(ValidationException::class)
  suspend fun handleValidationException(e: Exception): ResponseEntity<ErrorResponse> {
    log.error("Validation exception", e)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Validation failure: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(EntityNotFoundException::class)
  suspend fun handleEntityNotFoundException(e: Exception): ResponseEntity<ErrorResponse> {
    log.error("Entity not found", e)
    return ResponseEntity
      .status(NOT_FOUND)
      .body(
        ErrorResponse(
          status = NOT_FOUND,
          userMessage = "Entity not found: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(WorkloadWebClientTimeoutException::class)
  suspend fun handleWorkloadWebClientTimeoutException(e: Exception): ResponseEntity<ErrorResponse> {
    log.error("WebClient Timeout", e)
    return ResponseEntity
      .status(HttpStatus.GATEWAY_TIMEOUT)
      .body(
        ErrorResponse(
          status = HttpStatus.GATEWAY_TIMEOUT,
          userMessage = "Timeout: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(WorkloadFailedDependencyException::class)
  suspend fun handleFailedDependencyException(e: Exception): ResponseEntity<ErrorResponse> {
    log.error("WebClient Dependency Failure", e)
    return ResponseEntity
      .status(HttpStatus.FAILED_DEPENDENCY)
      .body(
        ErrorResponse(
          status = HttpStatus.FAILED_DEPENDENCY,
          userMessage = "Failed Dependency: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(AccessDeniedException::class)
  suspend fun handleAccessDeniedException(e: Exception): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(HttpStatus.FORBIDDEN)
    .body(
      ErrorResponse(
        status = HttpStatus.FORBIDDEN,
        userMessage = "Access is denied",
        developerMessage = e.message,
      ),
    )

  @ExceptionHandler(java.lang.Exception::class)
  suspend fun handleException(e: java.lang.Exception): ResponseEntity<ErrorResponse?>? {
    log.error("Unexpected exception", e)
    return ResponseEntity
      .status(INTERNAL_SERVER_ERROR)
      .body(
        ErrorResponse(
          status = INTERNAL_SERVER_ERROR,
          userMessage = "Unexpected error: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

data class ErrorResponse(
  val status: Int,
  val errorCode: Int? = null,
  val userMessage: String? = null,
  val developerMessage: String? = null,
  val moreInfo: String? = null,
) {
  constructor(
    status: HttpStatus,
    errorCode: Int? = null,
    userMessage: String? = null,
    developerMessage: String? = null,
    moreInfo: String? = null,
  ) :
    this(status.value(), errorCode, userMessage, developerMessage, moreInfo)
}
