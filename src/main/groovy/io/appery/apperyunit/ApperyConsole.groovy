package io.appery.apperyunit

import static io.appery.apperyunit.Utils.*

class ApperyConsole {
    void log(String... msgs) {
        StringBuffer sb = new StringBuffer()
        sb.append('  [CONSOLE]  ')
        for (String msg in msgs) {
            sb.append(msg + ' ')
        }
        console cyan + sb.toString() + norm
    }
}
