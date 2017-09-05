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
                apperyClient.downloadProcess();
                break;
            case BatchRunnerMode.runMode:
                apperyClient.runProcess();
                break;
            case BatchRunnerMode.echoMode:
                apperyClient.echoProcess();
                break;
            case BatchRunnerMode.testMode:
                apperyClient.testProcess();
                break;
        }
        return null;
    }
    
    @Override
    protected void process(List<String> msgs) {
        String msg = msgs.get(msgs.size()-1);
        console_area.append(msg + '\n');
    }
    
    public void print(String msg) {
        publish(msg);
    }
}
