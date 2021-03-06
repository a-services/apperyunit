package io.appery.apperyunit

import groovy.json.*
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import static io.appery.apperyunit.Utils.*

import org.apache.http.impl.client.*
import org.apache.http.client.methods.*
import org.apache.http.*
import org.apache.http.util.*
import org.apache.http.client.*
import org.apache.http.client.utils.*
import org.apache.http.client.entity.*
import org.apache.http.message.*
import java.util.regex.*
import javax.swing.*
import javax.swing.JToggleButton.ToggleButtonModel
import javax.swing.event.TreeSelectionEvent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import org.apache.http.conn.ssl.*
import java.text.*

import javax.script.ScriptException

/**
 * Provides service methods attached to buttons on DashboardFrame.
 */
public class ApperyService {

    /**
     * Maps script name to the list of dependencies.
     */
    Map<String,List<String>> jsonDeps = [:];

    /**
     * Current node selected in DashboardFrame.
     */
    ScriptNode curObj;

    private DashboardFrame dashboardFrame;

    String userName;

    List<ScriptJson> scripts;
    List<FolderJson> folders;

    ApperyClient apperyClient;

    void init(DashboardFrame dashboardFrame) {
        this.dashboardFrame = dashboardFrame
        console_area = dashboardFrame.consoleArea
        apperyClient = new ApperyClient()
        openPasswordDialog();
    }

    /**
     * We need password to set cookies in HttpClient.
     */
    void openPasswordDialog() {
        PasswordDialog dialog = new PasswordDialog(dashboardFrame, true);
        dialog.setApperyClient(apperyClient);
        dialog.setDashboardFrame(dashboardFrame);
        if (userName) {
            dialog.loginField.setText(userName);
        }
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                System.exit(0);
            }
        });
        dialog.setVisible(true);
    }

    /**
     * Download server code script from Appery.
     */
    void downloadScript(ScriptJson script) {
        //def script = scripts.find { it.name==scriptName }
        if (script==null) {
            console "${red}[WARN]${norm} Script not found: ${red}${scriptName}${norm}"
        } else
        if (!script.isDownloaded) {
            script.isDownloaded = true
            String scriptName = script.name
            def details = apperyClient.jsonSlurper.parseText(apperyClient.downloadScript(script.guid));
            String fname = scriptName + '.js'
            File f = script.executable? new File(fname): new File(librariesFolder, fname)
            /*
            if (!overwrite && f.exists()) {
                console "${red}[WARN]${norm} Already exists: ${red}${scriptName}.js${norm}"
                return
            }
            */
            if (f.exists() && f.text==details.sourceCode) {
                console " Not changed: ${bold}${scriptName}.js${norm}"
            } else {
                f.text = details.sourceCode;
                console "Script saved: ${bold}${scriptName}.js${norm}"
            }
            //saveDependencies(script)
            updateJsonDependencies(script)
            saveScriptParams(script, details)
        }
    }

    void saveScriptParams(ScriptJson script, def details) {
        String scriptName = script.name
        File f = new File(paramsFolder, scriptName + ".params")
        if (f.exists() || !script.executable || details.testParams==null) {
            return
        }
        ScriptParams params = new ScriptParams()
        String s = JsonOutput.toJson(details.testParams.urlParameters)
        params.query = s.equals("{}")? "": JsonOutput.prettyPrint(s)
        params.body = details.testParams.body
        saveScriptParamsFile(scriptName, params)
    }
    
    /**
     * Load script logs from Appery and send them to console.
     */
    void loadLogs(ScriptJson script) {
        if (script==null) {
            console "${red}[WARN]${norm} Script not found: ${red}${scriptName}${norm}"
            return
        }
        String scriptName = script.name
        def details = apperyClient.loadScriptLogs(script.guid)

        String outFolder = fixturesFolder + '/' + scriptName
        ensureFolder(outFolder)

        StringBuilder sb = new StringBuilder()
        for (def ln in details) {
            sb.append(logDate(ln.time) + ': ' + ln.text + '\n')
        }
        String result = sb.toString()

        new File(outFolder, scriptName + '.log').text = result
        console result
        console "--- End of log for ${bold}${scriptName}${norm}"
    }

    void loadScriptList() {
        scripts = apperyClient.jsonSlurper.parseText(apperyClient.loadServerCodesList());
    }

    void loadFolders() {
    	folders = apperyClient.jsonSlurper.parseText(apperyClient.loadServerCodesFolders());
    }

    /**
     * Make checkbox read-only.
     * See::
     *   http://stackoverflow.com/questions/21561357/make-a-jcheckbox-unable-to-be-checked
     */
    class ReadOnlyToggleButtonModel extends ToggleButtonModel
    {
        public ReadOnlyToggleButtonModel()
        {
            super();
        }

        public ReadOnlyToggleButtonModel(boolean selected)
        {
            super();
            super.setSelected(selected);
        }

        public val(boolean selected) {
            super.setSelected(selected);
        }

        @Override
        public void setSelected(boolean b)
        {
            // intentionally do nothing
        }
    }

    /**
     * Create checkboxes in `dependenciesPanel`
     */
    void buildDependenciesPanel(JPanel dependenciesPanel) {
        List libs = scripts.findAll { !it.executable }
        libs.sort()
        libs.each {
            JCheckBox cb = new JCheckBox(it.name);
            cb.setModel(new ReadOnlyToggleButtonModel(false));
            cb.setFocusable(false);
            it.checkbox = cb
            dependenciesPanel.add(cb)
        }
        /*
        for (int i=0; i<10; i++) {
            JCheckBox cb = new JCheckBox("test "+i);
            dependenciesPanel.add(cb)
        }
        */
       dependenciesPanel.revalidate()
    }

    /**
     * Find out list of dependencies for `curObj` annd
     * set checkbox values in `dependenciesPanel`
     */
    void setCurrentDependencies() {
        //println "--- curObj: " + curObj
        List deps = curObj.isScript? curObj.data.dependencies: []
        List libs = scripts.findAll { !it.executable }
        int k = 0
        libs.each {
            boolean result = deps.contains(it.guid)
            it.checkbox.model.val(result)
            if (result) {
                k++
            }
        }
        dashboardFrame.depsLabel.text = pluralDeps(k)
        //dashboardFrame.dependenciesPanel.repaint()
    }

    String pluralDeps(int k) {
        if (k==0) {
            return "No dependencies"
        } else
        if (k==1) {
            return "1 dependency"
        } else {
            return k + " dependencies"
        }
    }

    /**
     * Save `.dependencies` file if not exists
     */
    void saveDependencies(obj, boolean downloadJS) {
        String scriptName = obj.name
        List dependencies = findAllDependencies(obj)

        File f = new File(paramsFolder, scriptName + '.dependencies')
        if (dependencies.size()==0) {
            f.delete()
            return
        }
        Writer w = f.newWriter()
        dependencies.each { guid ->
            def s = scripts.find { it.guid==guid }
            w.println s.name + '.js'
            if (downloadJS) {
                downloadScript(s)
            }
        }
        w.close()
        console "Dependencies: ${scriptName}.dependencies"
    }

    void saveDependencies(obj) {
        saveDependencies(obj, true)
    }

    /**
     * Find dependencies of dependencies.
     * @param obj  ScriptNode.data as script
     */
    List<String> findAllDependencies(obj) {
        DepTracker dt = new DepTracker()
        dt.trackDependencies(obj.dependencies)
        return new ArrayList(dt.result)
    }

    /**
     * Find all child scripts of some folder.
     */
    List<String> findAllScripts(folderId) {
        DepTracker dt = new DepTracker()
        dt.trackFolderScripts(folderId)
        return new ArrayList(dt.result)
    }

    /**
     * Fill `jsonDeps` structure with data from `scripts` list.
     */
    void updateJsonDependencies(ScriptJson script) {
        //for (def script in scripts) {
        List depNames = []
        script.dependencies.each { depGuid ->
            def dep = scripts.find { it.guid==depGuid }
            depNames.add(dep.name)
            if (!dep.isDownloaded) {
                downloadScript(dep as ScriptJson)
                dep.isDownloaded = true
            }
        }
        if (depNames.size()>0) {
            jsonDeps.put(script.name, depNames)
        } else {
            jsonDeps.remove(script.name);
        }
        //}
    }

    /**
     * Rewrite dependencies desctiptor file.
     */
    void saveJsonDependencies() {
        new File(librariesFolder, dependenciesJsonFile).text = JsonOutput.prettyPrint(JsonOutput.toJson(jsonDeps))
    }

    void loadJsonDependencies() {
        File f = new File(librariesFolder, dependenciesJsonFile)
        if (f.exists()) {
            jsonDeps = apperyClient.jsonSlurper.parseText(f.text)
        }
    }


    /**
     * Used to collect dependency records recursively,
     * storing them at `result`.
     */
    class DepTracker {

        List<String> result = new LinkedList()

        /**
         * Recursive support for `findAllDependencies()`
         */
        void trackDependencies(List<String> depList) {
            for (String guid in depList) {
                if (!result.contains(guid)) {
                    def obj = scripts.find { it.guid==guid }
                    trackDependencies(obj.dependencies);
                    result.add(guid)
                }
            }
        }

        /**
         * Recursive support for `findAllScripts()`
         */
        void trackFolderScripts(String parentId) {
            List subfolders = folders.findAll { it.parentId==parentId }
            List subscripts = scripts.findAll { it.executable && it.folderId==parentId }
            result.addAll(subscripts)
            for (def folder in subfolders) {
                trackFolderScripts(folder._id);
            }
        }

    }

    DefaultTreeModel buildTree() {
        ScriptNode rootObj = new ScriptNode("Server Code")
        rootObj.isRoot = true

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(rootObj);
        addFolders(root, null)
        DefaultTreeModel model = new DefaultTreeModel(root, true);
        return model
    }

    /* See::
     *   https://docs.oracle.com/javase/7/docs/api/javax/swing/tree/DefaultMutableTreeNode.html
     */
    void addFolders(DefaultMutableTreeNode parent, String parentId) {
        // find all child folders for the given `parentId`
        List list = folders.findAll { it.parentId==parentId }
        // create tree nodes for child folders
        list.each { f ->
            ScriptNode obj = new ScriptNode(f.name)
            obj.data = f
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(obj);
            parent.add(node)
            addFolders(node, f._id)
        }
        // create script nodes for this folder
        List items = scripts.findAll { it.executable && it.folderId==parentId }
        items.sort { it.name }
        items.each {
            ScriptNode obj = new ScriptNode(it.name)
            obj.isScript = true
            obj.data = it
            DefaultMutableTreeNode scriptNode = new DefaultMutableTreeNode(obj, false);
            parent.add(scriptNode)
        }
    }

    void printStats() {
        List items = scripts.findAll { it.executable }
        int k = items.size()
        int n = scripts.size() - items.size()
        console("=== $k scripts and $n libraries found ===")
    }

    /**
     * Called when the node is clicked in the tree.
     */
    void treeNodeSelected(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = dashboardFrame.scriptsTree.getLastSelectedPathComponent();
        if (node == null) {
            return;
        }
        curObj = node.getUserObject();

        setCurrentDependencies()
        reloadButtonStates()
        buildParamList();

        dashboardFrame.scriptNameLabel.setText("<html>${curScriptNameText()}</html>");
        //if (statusBk!=null) {
        dashboardFrame.scriptNamePanel.setBackground(UIManager.getColor("Panel.background"));
        dashboardFrame.scriptNameLabel.setForeground(UIManager.getColor("Panel.foreground"));

        dashboardFrame.downloadButton.setEnabled(true)
        dashboardFrame.swaggerButton.setEnabled(true)
    }
    
    void reloadButtonStates() {
        String scriptName = curObj.name
        if (curObj.isScript) {
            File paramsFile = new File(paramsFolder, scriptName+'.params')
            String text = paramsFile.exists()? paramsFile.text : ""
            String[] params = ServerCode.extractBody(text)
            dashboardFrame.paramsQueryArea.setText(params[0])
            dashboardFrame.paramsQueryArea.setEditable(true)
            dashboardFrame.paramsBodyArea.setText(params[1])
            dashboardFrame.paramsBodyArea.setEditable(true)

            boolean scriptExists = new File(scriptName+'.js').exists()
            dashboardFrame.runButton.setEnabled(scriptExists)

            File successFile = new File(fixturesFolder + '/' + scriptName + '/' + scriptName+'.success.json')
            dashboardFrame.echoButton.setEnabled(successFile.exists())
            dashboardFrame.testButton.setEnabled(successFile.exists())

        } else {
            dashboardFrame.paramsQueryArea.setText("")
            dashboardFrame.paramsQueryArea.setEditable(false)
            dashboardFrame.paramsBodyArea.setText("")
            dashboardFrame.paramsBodyArea.setEditable(false)

            dashboardFrame.runButton.setEnabled(false);
            dashboardFrame.echoButton.setEnabled(false);
            dashboardFrame.testButton.setEnabled(false);
        }
        
        dashboardFrame.saveParamsButton.setEnabled(false)
        dashboardFrame.resetParamsButton.setEnabled(false) 
    }
    
    String curScriptNameText() {
        String type = curObj.isScript? "script" : "folder"
        return "<b>${xmp(curObj.name)}</b> $type"
    }

    String curParamsText() {
        File f = new File(paramsFolder, curObj.name+'.params')
        if (f.exists()) {
            return f.text
        } else {
            return ""
        }
    }

    /**
     * Instead of running `processDownload()` directly on click,
     * we delegate it to `BatchRunner`  that implements `SwingWorker`.
     * @see #downloadProcess()
     */
    void buttonDownload() {
        BatchRunner batchRunner = new BatchRunner()
        batchRunner.apperyService = this
        batchRunner.mode = BatchRunnerMode.downloadMode
        batch_runner = batchRunner
        batchRunner.execute()
        batch_runner = null
    }

    void markAsNotDownloaded(List<ScriptNode> scripts) {
        for (int i=0; i<scripts.size(); i++) {
            scripts[i].isDownloaded = false
        }
    }

    /**
     * `Download` button clicked
     */
    void processDownload() {
        dashboardFrame.downloadButton.setEnabled(false)
        try {
            markAsNotDownloaded(scripts)

            if (curObj.isScript) {
                downloadScript(curObj.data as ScriptJson)
                reloadButtonStates()
            } else
            if (curObj.isRoot) {
                console "--- Downloading all scripts ---"
                for (int i=0; i<scripts.size(); i++) {
                    downloadScript(scripts[i] as ScriptJson)
                }
                console "=== ${scripts.size()} scripts downloaded ---"
            } else {
                // is folder
                List subscripts = findAllScripts(curObj.data._id)
                console "--- Downloading ${subscripts.size()} scripts from `${curObj.name}` folder ---"
                for (def script in subscripts) {
                    downloadScript(script as ScriptJson)
                }
                console "=== ${subscripts.size()} scripts downloaded ---"
            }

            saveJsonDependencies()

        } catch (Exception e) {
            handleException(e, null);
        } finally {
            dashboardFrame.downloadButton.setEnabled(true)
        }
    }

    void handleException(Exception e, ServerCode sc) {
        if (sc!=null && (e instanceof ScriptException)) {
            sc.printScriptError(e)
        } else {
            e.printStackTrace();
        }
    }

    String swaggerOutputFile = "fixtures/swagger.json";
    SwaggerBuilder swagger;
    
    void buttonSwagger() {
        String result = JOptionPane.showInputDialog(dashboardFrame,
             "Swagger definition file to generate:", "Swagger",
             JOptionPane.QUESTION_MESSAGE, null, null, swaggerOutputFile);
        if (result==null) {
            return
        } 
        if (!result.endsWith('.json')) {
            console "[ERROR] Swagger definition file is expected to have `.json` extension"
            return
        } 
        swaggerOutputFile = result;
        swagger = new SwaggerBuilder(swaggerOutputFile, apperyClient, folders)
        
        BatchRunner batchRunner = new BatchRunner()
        batchRunner.apperyService = this
        batchRunner.mode = BatchRunnerMode.swaggerMode
        batch_runner = batchRunner
        batchRunner.execute()
        batch_runner = null
    }

    void processSwagger() {
        dashboardFrame.swaggerButton.setEnabled(false)
        try {
            if (curObj.isScript) {
                swagger.load(curObj.data as ScriptJson)
            } else
            if (curObj.isRoot) {
                for (int i=0; i<scripts.size(); i++) {
                    if (scripts[i].executable) {
                        swagger.load(scripts[i] as ScriptJson)
                    }
                }
            } else {
                // is folder
                List subscripts = findAllScripts(curObj.data._id)
                console "--- Downloading ${subscripts.size()} Swagger definitions from `${curObj.name}` folder ---"
                for (def script in subscripts) {
                    swagger.load(script as ScriptJson)
                }
            }
            swagger.saveResult();

        } catch (Exception e) {
            handleException(e, null);
        } finally {
            dashboardFrame.swaggerButton.setEnabled(true)
        }
    }
   
    /**
     * Starts `ServerCode` script in parallel thread.
     */
    void buttonRun() {
        BatchRunner batchRunner = new BatchRunner()
        batchRunner.apperyService = this
        batchRunner.mode = BatchRunnerMode.runMode
        batch_runner = batchRunner
        batchRunner.execute()
        batch_runner = null
    }

    /**
     * Runs `ServerCode` script in parallel thread.
     */
    void processRun() {
        dashboardFrame.runButton.setEnabled(false)
        ServerCode sc = new ServerCode()
        try {
            String scriptName = curObj.name
            //sc.dependencyList scriptName + '.dependencies'
            sc.addDependencies(scriptName)

            console_area.setText ""
            console "Script name: $scriptName"
            console '-'*80

            String params = curParamsText()
            echo_mode = false
            test_mode = false
            script_name = scriptName
            try {
                sc.run(scriptName+'.js', params)

            } catch (ScriptException e) {
                sc.printScriptError(e)

            } catch (TestFailedException et) {
            }
            dashboardFrame.echoButton.setEnabled(true)
            dashboardFrame.testButton.setEnabled(true)

        } catch (Exception e) {
            handleException(e, sc);
        } finally {
            dashboardFrame.runButton.setEnabled(true)
        }
    }

    /**
     * Starts `ServerCode` echo in parallel thread.
     */
    void buttonEcho() {
        BatchRunner batchRunner = new BatchRunner()
        batchRunner.apperyService = this
        batchRunner.mode = BatchRunnerMode.echoMode
        batch_runner = batchRunner
        batchRunner.execute()
        batch_runner = null
    }

    /**
     * Runs `ServerCode` echo in parallel thread.
     */
    void processEcho() {
        dashboardFrame.echoButton.setEnabled(false)
        ServerCode sc = new ServerCode()
        try {
            String scriptName = curObj.name
            sc.dependencyList scriptName + '.dependencies'

            console_area.setText ""
            console "Script name: $scriptName"
            console '-'*80

            String params = curParamsText()
            echo_mode = true
            test_mode = false
            script_name = scriptName
            try {
                sc.run(scriptName+'.js', params)

            } catch (ScriptException e) {
                sc.printScriptError(e)

            } catch (TestFailedException et) {
            }

        } catch (Exception e) {
            handleException(e, sc);
        } finally {
            dashboardFrame.echoButton.setEnabled(true)
        }
    }

    /**
     * Starts `ServerCode` test in parallel thread.
     */
    void buttonTest() {
        BatchRunner batchRunner = new BatchRunner()
        batchRunner.apperyService = this
        batchRunner.mode = BatchRunnerMode.testMode
        batch_runner = batchRunner
        batchRunner.execute()
        batch_runner = null
    }

    /**
     * Runs `ServerCode` test in parallel thread.
     */
    void processTest() {
        dashboardFrame.echoButton.setEnabled(false)
        ServerCode sc = new ServerCode()
        try {
            String scriptName = curObj.name
            sc.dependencyList scriptName + '.dependencies'

            console_area.setText ""
            console "Script name: $scriptName"
            console '-'*80

            boolean testFailed = false
            String params = curParamsText()
            echo_mode = true
            test_mode = true
            script_name = scriptName
            try {
                sc.run(scriptName+'.js', params)

            } catch (ScriptException e) {
                sc.printScriptError(e)

            } catch (TestFailedException et) {
                testFailed = true
            }

            if (testFailed) {
                dashboardFrame.scriptNamePanel.setBackground(new java.awt.Color(255, 221, 229));
                dashboardFrame.scriptNameLabel.setForeground(new java.awt.Color(255, 0, 58));
                dashboardFrame.scriptNameLabel.setText("<html>${curScriptNameText()} - TEST FAILED</html>");
            } else {
                dashboardFrame.scriptNamePanel.setBackground(new java.awt.Color(204, 247, 167));
                dashboardFrame.scriptNameLabel.setForeground(new java.awt.Color(102, 153, 58));
                dashboardFrame.scriptNameLabel.setText("<html>${curScriptNameText()} - TEST SUCCEEDED</html>");
            }

        } catch (Exception e) {
            handleException(e, sc);
        } finally {
            dashboardFrame.echoButton.setEnabled(true)
        }
    }
    /**
     * Apply prettyprinting to contents of *Params* textarea.
     */
    String prettyPrintParams(String text) throws JsonException {
        String s = JsonOutput.toJson(apperyClient.jsonSlurper.parseText(text))
        return s.equals("{}")? "": JsonOutput.prettyPrint(s)
    }

    /**
     * Saves `.params` file for `curObj`.
     */
    void buttonSaveParams() {
        String scriptName = curObj.name
        ScriptParams params = new ScriptParams()
        params.query = dashboardFrame.paramsQueryArea.text.trim()
        params.body = dashboardFrame.paramsBodyArea.text.trim()
        try {
            params = saveScriptParamsFile(scriptName, params)
            dashboardFrame.paramsQueryArea.text = params.query
            dashboardFrame.paramsBodyArea.text = params.body
            console "File saved: ${scriptName}.params"

            reloadButtonStates()
            
        } catch(JsonException e) {
            console '[ERROR] Cannot parse JSON parameters. Error message:'
            console "`"*80
            console e.getMessage()
        }
    }

    /**
     * Save `.params` file and return formatted values.
     */
    ScriptParams saveScriptParamsFile(String scriptName, ScriptParams params) {
        if (params.query.length()==0 && params.body.length()==0) {
            new File(paramsFolder, scriptName + ".params").delete()
        } else {
            String text = prettyPrintParams("{}")
            if (params.query.length()>0) {
                params.query = prettyPrintParams(params.query)
                text = params.query
            } else {
                params.query = text
            }
            if (params.body.length()>0) {
                try {
                    params.body = prettyPrintParams(params.body)
                } catch(JsonException e1) {
                }
                text += '\n----\n' + params.body
            }
            new File(paramsFolder, scriptName + ".params").text = text
        }
        return params
    }
    
    class ScriptParams {
        String query;
        String body;
    }
    
    void buttonResetParams() {
        String scriptName = curObj.name
        reloadButtonStates()
        console "File reset: ${scriptName}.params"

        //dashboardFrame.saveParamsButton.setEnabled(false);
        //dashboardFrame.resetParamsButton.setEnabled(false);
    }
    
    /**
     * Checks ApperyDB if we are running the latest program version
     * and updates `scriptNameLabel` in `DashboardFrame` appropriately.
     */
    void checkApperyUnitVersion() {
        URIBuilder uriBuilder = new URIBuilder()
            .setScheme("https")
            .setHost("api.appery.io")
            .setPath("/rest/1/db/collections/Info/")
            .addParameter("where", '{"name":"version"}')
        HttpGet httpGet = new HttpGet(uriBuilder.build());
        httpGet.addHeader(new BasicHeader("X-Appery-Database-Id", "58ad4ff2e4b0e91ec571ce2d"));
        HttpResponse response = apperyClient.httpclient.execute(httpGet);
        String result = ""
        try {
            int status = response.getStatusLine().getStatusCode();
            assert status==200
            result = EntityUtils.toString(response.getEntity());
            def json = apperyClient.jsonSlurper.parseText(result)
            String curVersion = json[0].value
            if (curVersion!=apperyUnitVersion) {
                dashboardFrame.scriptNameLabel.setText(
                    '<html><b>ApperyUnit</b> Version ' + Utils.apperyUnitVersion +
                    ' - New version ' + curVersion + ' available, visit ' +
                    '<a href="http://apperyunit.app.appery.io/">apperyunit.app.appery.io</a> to download</html>');
                dashboardFrame.scriptNameLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                dashboardFrame.scriptNameLabel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        try {
                            java.awt.Desktop.getDesktop().browse(new URI('http://apperyunit.app.appery.io/'));
                        } catch (URISyntaxException | IOException ex) {
                        }
                    }
                });
            }
        } finally {
            response.close()
        }
    }

    /**
     * Fills `paramList` in `DashboardFrame` with
     * params info from `curObj`.
     */
    void buildParamList() {
        /*
        ListModel<String> listModel = new DefaultListModel<String>();
        dashboardFrame.paramList.setModel(listModel);
        if (curObj!=null && curObj.isScript) {
            listModel.addElement(curObj.name + '.params');
            dashboardFrame.paramList.setSelectedIndex(0);
            dashboardFrame.paramList.ensureIndexIsVisible(0);
        }*/
        ComboBoxModel<String> comboModel = new DefaultComboBoxModel<String>();
        dashboardFrame.paramListComboBox.setModel(comboModel);
        if (curObj!=null && curObj.isScript) {
            comboModel.addElement(curObj.name + '.params');
            dashboardFrame.paramListComboBox.setSelectedIndex(0);
        }
        //paramListComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        
    }


}