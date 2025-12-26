package ru.alekseev.myapplication.domain.exception

/**
 * Base class for all domain exceptions.
 * Domain exceptions represent business rule violations and expected error conditions.
 */
sealed class DomainException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Thrown when a requested resource (message, summary, conversation) is not found.
 */
class ResourceNotFoundException(
    resourceType: String,
    resourceId: String
) : DomainException("$resourceType not found: $resourceId")

/**
 * Thrown when invalid input is provided to domain operations.
 */
class ValidationException(
    fieldName: String,
    invalidValue: String,
    reason: String
) : DomainException("Validation failed for '$fieldName': $reason (value: '$invalidValue')")

/**
 * Thrown when an external gateway (Claude API, RAG service) fails.
 */
class GatewayException(
    gatewayName: String,
    message: String,
    cause: Throwable? = null
) : DomainException("$gatewayName error: $message", cause)

/**
 * Thrown when a business rule is violated.
 */
class BusinessRuleException(
    rule: String,
    message: String
) : DomainException("Business rule violated [$rule]: $message")
