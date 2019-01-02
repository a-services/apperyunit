package io.appery.apperyunit;

import static io.appery.apperyunit.Utils.*;

import java.util.List;
import javax.swing.*;

class BatchRunner extends SwingWorker<Void, String> {

    ApperyService apperyService;
    BatchRunnerMode mode;

    @Override
    protected Void doInBackground() {
        switch (mode) {
            case BatchRunnerMode.downloadMode:
                apperyService.processDownload();
                break;
            case BatchRunnerMode.runMode:
                apperyService.processRun();
                break;
            case BatchRunnerMode.echoMode:
                apperyService.processEcho();
                break;
            case BatchRunnerMode.testMode:
                apperyService.processTest();
                break;
            case BatchRunnerMode.swaggerMode:
                apperyService.processSwagger();
                break;
        }
        return null;
    }

    @Override
    protected void process(List<String> msgs) {
    	StringBuffer sb = new StringBuffer();
        for (String msg: msgs) {
            sb.append(msg + '\n');
        }
        console_area.append(sb.toString());
    }

    public void print(String msg) {
        publish(msg);
    }
}
