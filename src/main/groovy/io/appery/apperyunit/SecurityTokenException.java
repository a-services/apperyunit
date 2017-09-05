package io.appery.apperyunit;

/**
 * Exception raised when getting Appery security token.
 */
class SecurityTokenException extends RuntimeException {

    String reason;

    SecurityTokenException(String reason) {
        this.reason = reason;
    }
}
