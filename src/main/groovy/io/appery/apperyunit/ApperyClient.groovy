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

import javax.script.ScriptException

/**
 * Provides methods to execute when buttons on `DashboardFrame` are called.
 */
public class ApperyClient extends ApperyRestClient {

    JsonSlurper jsonSlurper = new JsonSlurper()
    
    /**
     * Maps script name to the list of dependencies.
     */
    Map<String,List<String>> jsonDeps = [:];
    
    ScriptNode curObj;
    DashboardFrame dashboardFrame;

    /**
     * Initialize ApperyClient in GUI mode.
     */
    void init(DashboardFrame dashboardFrame) {
        this.dashboardFrame = dashboardFrame
        console_area = dashboardFrame.consoleArea
        openPasswordDialog();
    }
    
    /**
     * We need password to set cookies in HttpClient.
     */
    void openPasswordDialog() {
        PasswordDialog dialog = new PasswordDialog(dashboardFrame, true);
        dialog.setApperyClient(this);
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
            String body = makeGet('/bksrv/rest/1/code/admin/script/' + script.guid)
            def details = jsonSlurper.parseText(body)
            File f =  new File(scriptName + '.js') 
            /*
            if (!overwrite && f.exists()) {
                console "${red}[WARN]${norm} Already exists: ${red}${scriptName}.js${norm}"    
                return
            }
            */
            f.text = details.sourceCode;
            console "Script saved: ${bold}${scriptName}.js${norm}"
            //saveDependencies(script)
            updateJsonDependencies(script)
        }
    }

    String addGetParams(String url, Map parameters) {
        URL u = new URL(url)
        URIBuilder uriBuilder = new URIBuilder()
                .setScheme(u.protocol)
                .setHost(u.host)
                .setPath(u.path)
        parameters.each { name, value ->
            uriBuilder.addParameter(name, value)
        }
        return uriBuilder.build()
    }

    boolean doLogin(String username, String password) {
        String target = "https://" + host + "/bksrv/"; // "/app/"
        String loginUrl = "https://idp." + host + "/idp/doLogin";
        loginUrl = addGetParams(loginUrl, [ "cn":username, "pwd":password, "target":target ])
        HttpGet request = new HttpGet(loginUrl);
        def response = httpclient.execute(request, new HttpResponseHandler());
        
        String htmlText = response.body
        String samlKey = getSAMLDocumentFromPage(htmlText)
        if (samlKey==null) {
            return false
        }
        String targetIdpUrl = getActionEndpointURL(htmlText)
        if (targetIdpUrl==null) {
            return false
        }
        HttpPost samlRequest = new HttpPost(targetIdpUrl);
        samlRequest.addHeader(new BasicHeader("Content-Type", "application/x-www-form-urlencoded"));
        ArrayList postParameters = new ArrayList<NameValuePair>();
        postParameters.add(new BasicNameValuePair("SAMLResponse", samlKey));
        samlRequest.setEntity(new UrlEncodedFormEntity(postParameters, "UTF-8"));
        response = httpclient.execute(samlRequest, new HttpResponseHandler());
        
        return true
    }

    String getSAMLDocumentFromPage(String htmlpage_text) {
        Pattern samlPattern = Pattern.compile("VALUE=\"([^\"]+)");
        Matcher m = samlPattern.matcher(htmlpage_text);
        if (m.find()) {
            String saml = m.group();
            return saml.substring("VAlUE=\"".length());
        }
        return null;
    }

    String getActionEndpointURL(String htmlpage_text) {
        Pattern actionPattern = Pattern.compile("ACTION=\"([^\"]+)");
        Matcher m = actionPattern.matcher(htmlpage_text);
        if (m.find()) {
            String action = m.group();
            return action.substring("ACTION=\"".length());
        } 
        return null;
    }
    

    
    void loadScriptList() {
        String body = makeGet('/bksrv/rest/1/code/admin/script/?light=true')
        //if (!sessionTokenExpired) {
            scripts = jsonSlurper.parseText(body)
        //}
    }	

    void loadFolders() {
        String body = makeGet('/bksrv/rest/1/code/admin/folders/')
        //if (!sessionTokenExpired) {
            folders = jsonSlurper.parseText(body)
        //}
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
        libs.each {
            it.checkbox.model.val(deps.contains(it.guid))
        }
        //dashboardFrame.dependenciesPanel.repaint()
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
            downloadScript(dep as ScriptJson)
        }
        if (depNames.size()>0) {
            jsonDeps.put(script.name, depNames)
        } else {
            jsonDeps.remove(script.name);
        }
        //}
    }
    
    /**
     * Rewrite `dependencies.json` file.
     */
    void saveJsonDependencies() {
        new File('dependencies.json').text = JsonOutput.prettyPrint(JsonOutput.toJson(jsonDeps))
    }
    
    void loadJsonDependencies() {
        File f = new File('dependencies.json')
        if (f.exists()) {
            jsonDeps = new JsonSlurper().parseText(f.text)
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
        String scriptName = curObj.name
        
        setCurrentDependencies()
        
        if (curObj.isScript) {
            File paramsFile = new File(paramsFolder, scriptName+'.params')
            dashboardFrame.paramsArea.setText(paramsFile.exists()? paramsFile.text : "")
            dashboardFrame.paramsArea.setEditable(true)
        } else {
            dashboardFrame.paramsArea.setText("")
            dashboardFrame.paramsArea.setEditable(false)
        }
        
        dashboardFrame.scriptNameLabel.setText("<html>${curScriptNameText()}</html>");
        //if (statusBk!=null) {
        dashboardFrame.scriptNamePanel.setBackground(UIManager.getColor("Panel.background"));
        dashboardFrame.scriptNameLabel.setForeground(UIManager.getColor("Panel.foreground"));
        
        
        dashboardFrame.downloadButton.setEnabled(true)
        dashboardFrame.saveButton.setEnabled(false)
        dashboardFrame.runButton.setEnabled(new File(scriptName+'.js').exists())

        File successFile = new File(fixturesFolder + '/' + scriptName + '/' + scriptName+'.success.json')
        dashboardFrame.echoButton.setEnabled(successFile.exists())
        dashboardFrame.testButton.setEnabled(successFile.exists())
        
        buildParamList();
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
     * Instead of running `downloadProcess()` directly on click, 
     * we delegate it to `BatchRunner`  that implements `SwingWorker`.
     * @see #downloadProcess()
     */
    void buttonDownload() {
        BatchRunner batchRunner = new BatchRunner()
        batchRunner.apperyClient = this
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
    void downloadProcess() {
        dashboardFrame.downloadButton.setEnabled(false)
        try {
            ensureFolder(paramsFolder)
            markAsNotDownloaded(scripts)
            
            if (curObj.isScript) {
                downloadScript(curObj.data as ScriptJson)
                dashboardFrame.runButton.setEnabled(true)
            } else 
            if (curObj.isRoot) {
                console "--- Downloading all scripts ---"
                for (int i=0; i<scripts.size(); i++) {
                    downloadScript(scripts[i] as ScriptJson)
                    console "Script saved: ${bold}${scripts[i].name}.js${norm}"
                }
            } else {
                // is folder
                List subscripts = findAllScripts(curObj.data._id)
                console "--- Downloading ${subscripts.size()} scripts from `${curObj.name}` folder ---" 
                for (def script in subscripts) {
                    downloadScript(script as ScriptJson)
                }
                console "--- Done ---"
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
    
    void ensureFixturesFolder() {
        ensureFolder(fixturesFolder)
    }
    
    void buttonRun() {
        BatchRunner batchRunner = new BatchRunner()
        batchRunner.apperyClient = this
        batchRunner.mode = BatchRunnerMode.runMode
        batch_runner = batchRunner
        batchRunner.execute()
        batch_runner = null        
    }
    
    void runProcess() {
        dashboardFrame.runButton.setEnabled(false)
        ServerCode sc = new ServerCode()
        try {
            String scriptName = curObj.name
            //sc.dependencyList scriptName + '.dependencies'
            sc.addDependencies(scriptName)

            console ""
            console "Script name: $scriptName"
            console '-'*80

            String params = curParamsText()
            echo_mode = false
            test_mode = false
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
    
    void buttonEcho() {
        BatchRunner batchRunner = new BatchRunner()
        batchRunner.apperyClient = this
        batchRunner.mode = BatchRunnerMode.echoMode
        batch_runner = batchRunner
        batchRunner.execute()
        batch_runner = null        
    }
    
    void echoProcess() {
        dashboardFrame.echoButton.setEnabled(false)
        ServerCode sc = new ServerCode()
        try {
            String scriptName = curObj.name
            sc.dependencyList scriptName + '.dependencies'

            console ""
            console "Script name: $scriptName"
            console '-'*80

            String params = curParamsText()
            echo_mode = true
            test_mode = false
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
    
    void buttonTest() {
        BatchRunner batchRunner = new BatchRunner()
        batchRunner.apperyClient = this
        batchRunner.mode = BatchRunnerMode.testMode
        batch_runner = batchRunner
        batchRunner.execute()
        batch_runner = null        
    }

    void testProcess() {
        dashboardFrame.echoButton.setEnabled(false)
        ServerCode sc = new ServerCode()
        try {
            String scriptName = curObj.name
            sc.dependencyList scriptName + '.dependencies'

            console ""
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
    
    void buttonSave() {
        boolean validParams = true
        String[] params = ServerCode.extractBody(dashboardFrame.paramsArea.text)
        try {
            jsonSlurper.parseText(params[0])
        } catch (JsonException e) {
            validParams = false
            console '[ERROR] Cannot parse JSON parameters. Error message:'
            console "`"*80
            console e.getMessage()
        }
        if (validParams) {
            String scriptName = curObj.name
            new File(scriptName + ".params").text = dashboardFrame.paramsArea.text
            console "File saved: ${scriptName}.params"
            
            dashboardFrame.saveButton.setEnabled(false);
            dashboardFrame.runButton.setEnabled(true);
            dashboardFrame.echoButton.setEnabled(true);
            dashboardFrame.testButton.setEnabled(true);
        }
    }
    
    void checkApperyUnitVersion() {
        URIBuilder uriBuilder = new URIBuilder()
            .setScheme("https")
            .setHost("api.appery.io")
            .setPath("/rest/1/db/collections/Info/")
            .addParameter("where", '{"name":"version"}')
        HttpGet httpGet = new HttpGet(uriBuilder.build());
        httpGet.addHeader(new BasicHeader("X-Appery-Database-Id", "58ad4ff2e4b0e91ec571ce2d"));
        HttpResponse response = httpclient.execute(httpGet);
        String result = ""
        try {
            int status = response.getStatusLine().getStatusCode();
            assert status==200
            result = EntityUtils.toString(response.getEntity());  
            def json = jsonSlurper.parseText(result)
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
    
    void buildParamList() {
        ListModel<String> listModel = new DefaultListModel<String>();
        dashboardFrame.paramList.setModel(listModel);
        if (curObj!=null && curObj.isScript) {
            listModel.addElement(curObj.name + '.params');
            dashboardFrame.paramList.setSelectedIndex(0);
            dashboardFrame.paramList.ensureIndexIsVisible(0);
        }
    }
}