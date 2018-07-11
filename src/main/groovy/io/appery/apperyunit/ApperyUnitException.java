package io.appery.apperyunit;

/**
 * Exception raised if some AppeyUnit assertion fails..
 */
class ApperyUnitException extends RuntimeException {

    String reason;

    ApperyUnitException(String reason) {
        this.reason = reason;
    }
    
    public String toString() {
        return "[ApperyUnitException] Reason: " + reason;
    }
}
