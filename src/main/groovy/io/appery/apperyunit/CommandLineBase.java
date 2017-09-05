package io.appery.apperyunit;

import static io.appery.apperyunit.Utils.*;

/**
 * Parsing command-line parameters.
 */
class CommandLineBase {

    int pos = 0;
    String[] args;

    boolean extractDownloadMode() {
        boolean download_mode = false;
        if (pos < args.length) {
            if (args[pos].equals("download")) {
                pos++;
                download_mode = true;
            }
        }
        return download_mode;
    }

    boolean extractEchoMode() {
        echo_mode = false;
        if (pos < args.length) {
            if (args[pos].equals("echo")) {
                pos++;
                echo_mode = true;
            }
        }
        return echo_mode;
    }

    boolean extractTestMode() {
        test_mode = false;
        if (pos < args.length) {
            if (args[pos].equals("test")) {
                pos++;
                test_mode = true;
                echo_mode = true;
            }
        }
        return test_mode;
    }

    boolean extractColorMode() {
        if (pos < args.length) {
            if (args[pos].equals("color")) {
                pos++;
                color_mode = !color_mode;
            }
        }
        initColors();
        return color_mode;
    }

}
