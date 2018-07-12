package io.appery.apperyunit;

import java.io.IOException;
import java.util.*;
import org.apache.http.impl.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.*;
import org.apache.http.util.*;
import org.apache.http.client.*;
import org.apache.http.client.utils.*;
import org.apache.http.client.entity.*;
import org.apache.http.message.*;

import org.apache.http.client.utils.URIBuilder;
import java.net.URISyntaxException;
import org.apache.http.entity.StringEntity;

import groovy.json.JsonSlurper;

/**
 * Uses `HttpClient` to access Appery.io REST API services.
 */
public class ApperyRestClient {

    CloseableHttpClient httpclient = HttpClients.createDefault();

    String host = "appery.io";
    String userName;

    List<ScriptJson> scripts;
    List<FolderJson> folders;

    JsonSlurper jsonSlurper = new JsonSlurper();
        

    void setHost(String host) {
        this.host = host;
    }

    /**
     * Performs HTTP GET.
     */
    String makeGet(String serviceUrl, Map<String, String> params) throws IOException {
        HttpGet req;
        try {
            URIBuilder ub = new URIBuilder("https://" + host + serviceUrl);
            if (params!=null) {
                params.forEach((key,value) -> ub.setParameter(key, value));
            }
            req = new HttpGet(ub.build());
            
        } catch(URISyntaxException e) {
            throw new ApperyUnitException("[URISyntaxException] " + e.getMessage());
        }
        req.addHeader(new BasicHeader("Accept", "application/json"));
        req.addHeader(new BasicHeader("User-Agent", "AU-Test-Agent"));
        CloseableHttpResponse response = httpclient.execute(req);
        String result = "";
        try {
            int status = response.getStatusLine().getStatusCode();
            if (status != 200) {
                throw new ApperyUnitException("HTTP status expected: 200, received: " + status);
            }
            result = EntityUtils.toString(response.getEntity());
            //sessionTokenExpired = result.startsWith('<HTML>')
        } finally {
            response.close();
        }
        return result;
    }

    /**
     * Performs HTTP GET.
     */
    String makeGet(String serviceUrl) throws IOException {
        return makeGet(serviceUrl, null);
    }
    
    /**
     * Performs HTTP POST.
     */
    String makePost(String serviceUrl, String data) throws IOException {
        HttpPost req = new HttpPost("https://" + host + serviceUrl);
        req.addHeader(new BasicHeader("Content-Type", "application/json"));
        req.addHeader(new BasicHeader("Accept", "application/json"));
        req.addHeader(new BasicHeader("User-Agent", "AU-Test-Agent"));
        req.addHeader(new BasicHeader("isrestapicall", "true"));
        req.setEntity(new StringEntity(data));
        CloseableHttpResponse response = httpclient.execute(req);
        String result = "";
        try {
            int status = response.getStatusLine().getStatusCode();
            if (status != 200) {
                throw new ApperyUnitException("HTTP status expected: 200, received: " + status);
            }
            result = EntityUtils.toString(response.getEntity());
            //sessionTokenExpired = result.startsWith('<HTML>')
        } finally {
            response.close();
        }
        return result;
    }

    /**
     * Performs HTTP PUT.
     */
    String makePut(String serviceUrl, String data) throws IOException {
        HttpPut req = new HttpPut("https://" + host + serviceUrl);
        req.addHeader(new BasicHeader("Content-Type", "application/json"));
        req.addHeader(new BasicHeader("Accept", "application/json"));
        req.addHeader(new BasicHeader("User-Agent", "AU-Test-Agent"));
        req.addHeader(new BasicHeader("isrestapicall", "true"));
        req.setEntity(new StringEntity(data));
        CloseableHttpResponse response = httpclient.execute(req);
        String result = "";
        try {
            int status = response.getStatusLine().getStatusCode();
            if (status != 200) {
                throw new ApperyUnitException("HTTP status expected: 200, received: " + status);
            }
            result = EntityUtils.toString(response.getEntity());
            //sessionTokenExpired = result.startsWith('<HTML>')
        } finally {
            response.close();
        }
        return result;
    }
    
    class LogEntry {
        String time;
        String text;
    }
    
    /**
     * Load script logs from Appery.io.
     */
    List<LogEntry> loadScriptLogs(String scriptGuid) throws IOException {
        String body = makeGet("/bksrv/rest/1/code/admin/script/" + scriptGuid + "/trace");
        return (List<LogEntry>) jsonSlurper.parseText(body);
    }
}
