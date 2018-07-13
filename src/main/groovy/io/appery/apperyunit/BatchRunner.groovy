package io.appery.apperyunit;

import static io.appery.apperyunit.Utils.*;

import java.util.List;
import javax.swing.*;

class BatchRunner extends SwingWorker<Void, String> {
    
    ApperyService appery;
    BatchRunnerMode mode;
    
    @Override
    protected Void doInBackground() {
        switch (mode) {
            case BatchRunnerMode.downloadMode:
                appery.processDownload();
                break;
            case BatchRunnerMode.runMode:
                appery.processRun();
                break;
            case BatchRunnerMode.echoMode:
                appery.processEcho();
                break;
            case BatchRunnerMode.testMode:
                appery.processTest();
                break;
            case BatchRunnerMode.logsMode:
                appery.processLogs();
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
