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
        assert script_name!=null
        
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
     * Performs SAML login to access Appery.io backend funnctions.
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

    // -------------- Appery projects

    /**
     * Get list of existing projects in Appery.io workspace.
     */
    String loadProjectList() {
        return traceGet('List of projects', '/app/rest/projects', null);
    }

    /**
     * Get information about project in Appery.io workspace.
     */
    String loadProjectInfo(String guid) {
        return traceGet('Project information', '/app/rest/projects', '/app/rest/html5/project', ['guid':guid]);
    }

    /**
     * Get available templates to create projects in Appery.io workspace.
     */
    String loadProjectTemplates() {
        return traceGet('Project templates', '/app/rest/html5/plugin/wizardProject', null);
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
    String updateProjectAssets(String projectGuid, assetsData) {
        String data = JsonOutput.toJson(assetsData)
        return tracePut('List of assets update result', '/app/rest/html5/project/' + projectGuid + '/asset/data', data)
    }

    // -------------- Server code scripts

    /**
     * Returns list of server code scripts and libraries
     * in Appery.io workspace. Metainformation about scripts included.
     */
    String loadServerCodesList() {
        return traceGet('List of server code scripts and libraries', '/bksrv/rest/1/code/admin/script/?light=true', null);
    }

    /**
     * Returns list of server code folders
     * in Appery.io workspace. Metainformation included.
     */
    String loadServerCodesFolders() {
        return traceGet('List of server code folders', '/bksrv/rest/1/code/admin/folders/', null);
    }

    /**
     * Download server code script.
     */
    String downloadScript(String scriptGuid) {
        return traceGet('Server code script', '/bksrv/rest/1/code/admin/script/' + scriptGuid, null);
    }

    // -------------- Utils

    String traceGet(String title, String url, Map<String, String> params) {
        return traceMeta(title, url, params, 'get');
    }

    String tracePost(String title, String url, Map<String, String> params) {
        return traceMeta(title, url, params, 'post');
    }

    String tracePut(String title, String url, Map<String, String> params) {
        return traceMeta(title, url, params, 'put');
    }

    String traceMeta(String title, String url, Map<String, String> params, String method) {
		metaCount++;
		String fname = metaFileName+metaCount+'.json'
		String result;
		if (echoMode) {
			console "ECHO MODE: reading from $fname"
			result = new File(outFolder, fname).text
		} else {
            switch (method) {
                case 'get':
                    result = makeGet(url, params);
                    break;
                case 'post':
                    result = makePost(url, params);
                    break;
                case 'put':
                    result = makePut(url, params);
                    break;
            }
            if (outFolder!=null) {
				new File(outFolder, fname).text = JsonOutput.prettyPrint(result);
				console "$title saved to $ital`$fname`$norm"
			}
		}
		return result
    }

    void saveJson(jsonData, String fname) {
        new File(fname).text = JsonOutput.prettyPrint(JsonOutput.toJson(jsonData))
        console "`$fname` saved"
    }

}
