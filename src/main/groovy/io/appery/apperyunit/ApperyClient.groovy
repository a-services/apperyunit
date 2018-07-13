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

    /**
     * Download servercode script.
     */
    def downloadScript(String scriptGuid) {
        try {
            String body = makeGet('/bksrv/rest/1/code/admin/script/' + scriptGuid)
            return jsonSlurper.parseText(body)
        } catch(Exception e) {
            console("[ERROR] " + e);
            return null;
        }
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
            new ApperySecurity(this).doLogin(username, password, targetPath);
            return true;
        } catch(ApperyUnitException e) {
            console("[ERROR] " + e);
            return false;
        }
    }

    /**
     * Returnns list of server code scripts and libraries 
     * in Appery.io workspace. Metainformation about scripts included.
     */
    def loadServerCodesList() {
        try {
            String body = makeGet('/bksrv/rest/1/code/admin/script/?light=true')
            return jsonSlurper.parseText(body)
        } catch(ApperyUnitException e) {
            console("[ERROR] " + e);
            return null;
        }
    }	

    /**
     * Returnns list of server code folders 
     * in Appery.io workspace. Metainformation included.
     */
    def loadServerCodesFolders() {
        try {
            String body = makeGet('/bksrv/rest/1/code/admin/folders/')
            return jsonSlurper.parseText(body)
        } catch(ApperyUnitException e) {
            console("[ERROR] " + e);
            return null;
        }
    }	

    
    /**
     * Get available templates to create projects in Appery.io workspace. 
     */
    def loadProjectTemplates() {
        String result = makeGet('/app/rest/html5/plugin/wizardProject')
        return jsonSlurper.parseText(result)
    }
    
    /**
     * Create project in Appery.io workspace.
     */
    def createApperyProject(String projectName, int projectType) {
        String data = JsonOutput.toJson(["name":projectName,"templateId":projectType])
        String result = makePost('/app/rest/projects', data)
        return jsonSlurper.parseText(result)
    }
    
    /**
     * Get list of existing projects in Appery.io workspace.
     */
    def loadProjectList() {
        String result = makeGet('/app/rest/projects')
        return jsonSlurper.parseText(result)
    }
    
    /**
     * Get information about project in Appery.io workspace.
     */
    def loadProjectInfo(String guid) {
        String result = makeGet('/app/rest/html5/project', ['guid':guid])
        return jsonSlurper.parseText(result)
    }
    
    /**
     * Load list of assets for Appery.io project.
     */
    def loadProjectAssets(String projectGuid, List<String> assets) {
        String data = JsonOutput.toJson(["assets": assets.collect { ['id':it] }])
        String result = makePost('/app/rest/html5/project/' + projectGuid + '/asset/data', data)
        return jsonSlurper.parseText(result)
    }
    
    /**
     * Update list of assets in Appery.io project.
     */
    String updateProjectAssets(String projectGuid, assetsData) {
        String data = JsonOutput.toJson(assetsData)
        String result = makePut('/app/rest/html5/project/' + projectGuid + '/asset/data', data)
        return result
    }
            
}