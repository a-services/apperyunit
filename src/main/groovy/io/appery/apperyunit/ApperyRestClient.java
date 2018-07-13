package io.appery.apperyunit;

import static io.appery.apperyunit.Utils.console;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import groovy.json.JsonSlurper;

/**
 * Access to Appery.io REST API services. 
 * Java part.
 */
public class ApperyRestClient {

    CloseableHttpClient httpclient = HttpClients.createDefault();

    String host; 

    JsonSlurper jsonSlurper = new JsonSlurper();
        
    ApperyRestClient() {
        host = "appery.io";
        String envHost = System.getenv("AU_BACKEND");
        if (envHost!=null) {
            host = envHost;
        } 
    }
    
    void setHost(String host) {
        this.host = host;
    }

    /**
     * Performs HTTP GET.
     */
    String makeGet(String serviceUrl, Map<String, String> params) 
           throws ApperyUnitException, IOException {
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
                throw new ApperyUnitException(status);
            }
            result = EntityUtils.toString(response.getEntity());
            if (result.startsWith("<HTML>")) {
                throw new ApperyUnitException("Authentication error");
            }
        } finally {
            response.close();
        }
        return result;
    }

    /**
     * Performs HTTP GET.
     */
    String makeGet(String serviceUrl) 
           throws ApperyUnitException, IOException {
        return makeGet(serviceUrl, null);
    }

    /**
     * Add parameters to GET URL.
     */
    static String addGetParams(String url, Map<String, String> parameters) 
                  throws MalformedURLException, URISyntaxException {
        URL u = new URL(url);
        URIBuilder uriBuilder = new URIBuilder()
                .setScheme(u.getProtocol())
                .setHost(u.getHost())
                .setPath(u.getPath());
                
        parameters.entrySet().stream().forEach(e -> 
            uriBuilder.addParameter(e.getKey(), e.getValue())
        );
        /* parameters.each { name, value ->
            uriBuilder.addParameter(name, value);
        } */
        
        return uriBuilder.build().toString();
    }
    
    /**
     * Performs HTTP POST.
     */
    String makePost(String serviceUrl, String data) 
           throws ApperyUnitException, IOException {
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
                throw new ApperyUnitException(status);
            }
            result = EntityUtils.toString(response.getEntity());
            if (result.startsWith("<HTML>")) {
                throw new ApperyUnitException("Authentication error");
            }
        } finally {
            response.close();
        }
        return result;
    }

    /**
     * Performs HTTP PUT.
     */
    String makePut(String serviceUrl, String data) 
           throws ApperyUnitException, IOException {
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
                throw new ApperyUnitException(status);
            }
            result = EntityUtils.toString(response.getEntity());
            if (result.startsWith("<HTML>")) {
                throw new ApperyUnitException("Authentication error");
            }
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
    List<LogEntry> loadScriptLogs(String scriptGuid) {
        try {
            String body = makeGet("/bksrv/rest/1/code/admin/script/" + scriptGuid + "/trace");
            return (List<LogEntry>) jsonSlurper.parseText(body);
        } catch(Exception e) {
            console("[ERROR] " + e);
            return null;
        }
    }
}
