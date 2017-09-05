package io.appery.apperyunit;

/**
 * Exception raised when ApperyUnit test fails.
 */
class TestFailedException extends RuntimeException {

    String jsonStr;
    String result;

    TestFailedException(String jsonStr, String result) {
        this.jsonStr = jsonStr;
        this.result = result;
    }
}
