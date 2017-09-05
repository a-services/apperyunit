package io.appery.apperyunit;

/**
 * Exception raised for invalid `.params` file.
 */
class ParamsException extends RuntimeException {

    String reason;

    ParamsException(String reason) {
        this.reason = reason;
    }
}
