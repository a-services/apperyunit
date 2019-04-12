package io.appery.apperyunit

import groovy.json.*
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
import org.apache.http.conn.ssl.*
import java.text.*

import javax.script.ScriptException

/**
 * Access to Appery.io REST API services.
 * Groovy part.
 */
public class ApperyClient extends ApperyRestClient {

    int metaCount = 0;

    /**
     * ApperyClient will save REST response into `outFolder`
     * if this property is not null.
     * It is based on static `scriptName` property.
     */
    String outFolder;
    static String scriptName;

    boolean echoMode = false;

    final String metaFileName = "meta-";

    /**
     * Create another instance of ApperyClient to use it from JavaScript.
     */
    static ApperyClient newInstance() {
        if (script_name==null) {
            // We expect global variable `script_name` defined in `Utils` to exist.
            throw new ApperyUnitException("Missing script name in ApperyClient");
        }

        ApperyClient ac = new ApperyClient();
        ac.outFolder = fixturesFolder + '/' + script_name
        ensureFolder(ac.outFolder)

        ac.echoMode = echo_mode
        return ac
	}

    // -------------- Login

    /**
     * Performs SAML login with default credentials.
     */
    boolean doLogin(String targetPath) {
        String auDebug = System.getenv("AU_DEBUG");
        if (auDebug==null) {
            return false;
        }
        String[] creds = auDebug.split(":");
        if (creds.length!=2) {
            return false;
        }
        return doLogin(creds[0], creds[1], targetPath)
    }

    /**
     * Performs SAML login to access Appery.io backend functions.
     */
    boolean doLogin(String username, String password) {
        return doLogin(username, password, "/bksrv/")
    }

    /**
     * Performs SAML login into Appery.io site.
     */
    boolean doLogin(String username, String password, String targetPath) {
        try {
			if (!echoMode) {
                new ApperySecurity(this).doLogin(username, password, targetPath);
		    } else {
				console "ECHO MODE: Login success"
			}
            return true;
        } catch(ApperyUnitException e) {
            console("[ERROR] " + e);
            return false;
        }
    }

    /**
     * Performs login to access standalone API Express.
     */
    boolean standaloneLogin(String username, String password) {
        try {
			if (!echoMode) {
                new ApperySecurity(this).standaloneLogin(username, password);
		    } else {
				console "ECHO MODE: Login success"
			}
            return true;
        } catch(ApperyUnitException e) {
            console("[ERROR] " + e);
            return false;
        }
    }

    // -------------- Appery projects

    /**
     * Get list of existing projects in Appery.io workspace.
     */
    String loadProjectList() {
        return traceGet('List of projects', '/app/rest/projects');
    }

    /**
     * Get information about project in Appery.io workspace.
     */
    String loadProjectInfo(String guid) {
        return traceGet('Project information', '/app/rest/html5/project', ['guid':guid]);
    }

    /**
     * Get available templates to create projects in Appery.io workspace.
     */
    String loadProjectTemplates() {
        return traceGet('Project templates', '/app/rest/html5/plugin/wizardProject');
    }

    /**
     * Create project in Appery.io workspace.
     */
    String createApperyProject(String projectName, int projectType) {
        String data = JsonOutput.toJson(["name":projectName,"templateId":projectType])
        return tracePost('Project creation result', '/app/rest/projects', data)
    }

    /**
     * Load list of assets for Appery.io project.
     */
    String loadProjectAssets(String projectGuid, List<String> assets) {
        String data = JsonOutput.toJson(["assets": assets.collect { ['id':it] }])
        return tracePost('List of assets', '/app/rest/html5/project/' + projectGuid + '/asset/data', data)
    }

    /**
     * Update list of assets in Appery.io project.
     */
    String updateProjectAssets(String projectGuid, String assetsData) {
        return tracePut('List of assets update result', '/app/rest/html5/project/' + projectGuid + '/asset/data', assetsData)
    }

    /**
     * Load ids of project source files.
     * @param projectGuidType  Project GUID concatenated with project type.
     *                         Example: projectGuid + '/IONIC/'
     */
    String loadSourceInfo(projectGuidType) {
        try {
            return traceGet('Source info', '/app/rest/html5/ide/source/read/' + projectGuidType);
        } catch (ApperyUnitException e) {
            return null;
        }
    }

    /**
     * Load source file by id.
     */
    String loadSource(srcId) {
        return makeGet('/app/rest/html5/ide/source/' + srcId + '/read/data');
    }

    /**
     * Get list of certificates in Appery.io workspace.
     */
    String loadCertificateList() {
        return traceGet('List of certificates', '/app/rest/certificates');
    }

    /**
     * Get information about certificate in Appery.io workspace.
     */
    String loadCertificateInfo(String uuid) {
        return traceGet('List of certificates', '/app/rest/certificates/' + uuid);
    }

    // -------------- Server code scripts

    /**
     * Returns list of server code scripts and libraries
     * in Appery.io workspace. Metainformation about scripts included.
     */
    String loadServerCodesList() {
        return traceGet('List of server code scripts and libraries', '/bksrv/rest/1/code/admin/script/?light=true');
    }

    /**
     * Returns list of server code folders
     * in Appery.io workspace. Metainformation included.
     */
    String loadServerCodesFolders() {
        return traceGet('List of server code folders', '/bksrv/rest/1/code/admin/folders/');
    }

    /**
     * Download server code script.
     */
    String downloadScript(String scriptGuid) {
        return traceGet('Server code script', '/bksrv/rest/1/code/admin/script/' + scriptGuid);
    }
    
    /**
     * Update server code script.
     */
    String updateScript(String scriptGuid, String scriptData) {
        return tracePut('Server code update result', '/bksrv/rest/1/code/admin/script/' + scriptGuid, scriptData)
    }

    // -------------- Database

    /**
     * Get list of existing databases in Appery.io workspace.
     */
    String loadDatabaseList() {
        return traceGet('List of databases', '/bksrv/rest/1/admin/databases');
    }

    /**
     * Get list of collections in Appery.io database.
     */
    String loadCollectionList(String dbid) {
        return traceGet('List of collections', '/bksrv/rest/1/admin/collections', null, ['X-Appery-Database-Id':dbid]);
    }

    // -------------- API Express

    /**
     * Get list of AEX projects in Appery.io workspace.
     */
    String loadAexProjectList() {
        return traceGet('List of AEX projects', '/apiexpress/rest/projects');
    }

    /**
     * Returns list of AEX folders in Appery.io workspace
     */
    String loadAexFolders(String projectRootId) {
        return traceGet('List of AEX folders', '/apiexpress/rest/folders/' + projectRootId + '/children');
    }

    /**
     * Returns list of AEX services in project folder
     */
    String loadAexServices(String folderId) {
        return traceGet('List of AEX services', '/apiexpress/rest/service/custom/' + folderId + '/children');
    }

    // -------------- Utils

    String traceGet(String title, String url) {
        return traceMeta(title, url, null, null, null, 'get');
    }

    String traceGet(String title, String url, Map<String, String> params) {
        return traceMeta(title, url, params, null, null, 'get');
    }

    String traceGet(String title, String url, Map<String, String> params, Map<String, String> headers) {
        return traceMeta(title, url, params, headers, null, 'get');
    }

    String tracePost(String title, String url, String body) {
        return traceMeta(title, url, null, null, body, 'post');
    }

    String tracePut(String title, String url, String body) {
        return traceMeta(title, url, null, null, body, 'put');
    }

    String traceMeta(String title, String url,
                     Map<String, String> params,
                     Map<String, String> headers,
                     String body,
                     String method) {
		metaCount++;
		String fname = metaFileName+metaCount+'.json'
		String result;
		if (echoMode) {
			console "ECHO MODE: reading from $fname"
			result = new File(outFolder, fname).text
		} else {
            switch (method) {
                case 'get':
                    result = makeGet(url, params, headers);
                    break;
                case 'post':
                    result = makePost(url, body);
                    break;
                case 'put':
                    result = makePut(url, body);
                    break;
            }
            if (outFolder!=null) {
				new File(outFolder, fname).text = JsonOutput.prettyPrint(result);
				console "$title saved to $ital`$fname`$norm"
			}
		}
		return result
    }

    void saveJson(String jsonData, String fname) {
        new File(fname).text = JsonOutput.prettyPrint(jsonData); //JsonOutput.toJson(jsonData))
        console "$ital`$fname`$norm saved"
    }

    void saveFile(text, String fname) {
        new File(fname).text = text
        console "$ital`$fname`$norm saved"
    }

    String readFile(String fname) {
        return new File(fname).text
    }

    void delay(int ms) {
        sleep(ms);
    }

}
