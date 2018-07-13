package io.appery.apperyunit;

/**
 * Exception raised if some AppeyUnit assertion fails..
 */
class ApperyUnitException extends Exception {

    String reason;
    int code;

    ApperyUnitException(int code) {
        this.code = code;
        this.reason = "HTTP status expected: 200, received: " + code;
    }

    ApperyUnitException(String reason) {
        this.reason = reason;
    }
    
    public String toString() {
        return "[ApperyUnitException] Reason: " + reason;
    }
}
