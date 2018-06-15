package io.appery.apperyunit;

import static io.appery.apperyunit.Utils.*;

import java.util.List;
import javax.swing.*;

class BatchRunner extends SwingWorker<Void, String> {
    
    ApperyClient apperyClient;
    BatchRunnerMode mode;
    
    @Override
    protected Void doInBackground() {
        switch (mode) {
            case BatchRunnerMode.downloadMode:
                apperyClient.processDownload();
                break;
            case BatchRunnerMode.runMode:
                apperyClient.processRun();
                break;
            case BatchRunnerMode.echoMode:
                apperyClient.processEcho();
                break;
            case BatchRunnerMode.testMode:
                apperyClient.processTest();
                break;
            case BatchRunnerMode.logsMode:
                apperyClient.processLogs();
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
