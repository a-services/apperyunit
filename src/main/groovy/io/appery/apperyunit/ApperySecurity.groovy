package io.appery.apperyunit;

import java.text.*;
import java.util.regex.*;

import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.entity.*;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.*;
import org.apache.http.impl.client.*;
import org.apache.http.message.*;
import org.apache.http.util.*;

/**
 * Performs SAML login into Appery.io site.
 */
public class ApperySecurity {

    ApperyClient apperyClient;
    CloseableHttpClient httpclient;
    String host;
    String protocol;

    ApperySecurity(ApperyClient apperyClient) {
        this.apperyClient = apperyClient;
    	this.httpclient = apperyClient.httpclient;
    	this.host = apperyClient.host;
    	this.protocol = apperyClient.protocol;
    }

    /**
     * Main method.
     */
    void doLogin(String username, String password, String targetPath) 
            throws ApperyUnitException {
        String target = protocol + host + targetPath; 
        String loginUrl = protocol + "idp." + host + "/idp/doLogin";
        
        Map<String, String> params = new HashMap<String, String>();
        params.put("cn", username);
        params.put("pwd", password);
        params.put("target", target);
        loginUrl = apperyClient.addGetParams(loginUrl, params);
        
        HttpGet request = new HttpGet(loginUrl);
        def response = httpclient.execute(request, new HttpResponseHandler());
        
        String htmlText = response.body;
        String samlKey = getSAMLDocumentFromPage(htmlText);
        if (samlKey==null) {
        	throw new ApperyUnitException("Login error: SAML key not found");
        }
        String targetIdpUrl = getActionEndpointURL(htmlText);
        if (targetIdpUrl==null) {
        	throw new ApperyUnitException("Login error: target IDP URL not found");
        }
        HttpPost samlRequest = new HttpPost(targetIdpUrl);
        samlRequest.addHeader(new BasicHeader("Content-Type", "application/x-www-form-urlencoded"));
        ArrayList postParameters = new ArrayList<NameValuePair>();
        postParameters.add(new BasicNameValuePair("SAMLResponse", samlKey));
        samlRequest.setEntity(new UrlEncodedFormEntity(postParameters, "UTF-8"));
        response = httpclient.execute(samlRequest, new HttpResponseHandler());
    }

    /**
     * Get SAML document during login.
     */
    private String getSAMLDocumentFromPage(String htmlpage_text) {
        Pattern samlPattern = Pattern.compile("VALUE=\"([^\"]+)");
        Matcher m = samlPattern.matcher(htmlpage_text);
        if (m.find()) {
            String saml = m.group();
            return saml.substring("VAlUE=\"".length());
        }
        return null;
    }

    /**
     * Get action endpoint during login.
     */
    private String getActionEndpointURL(String htmlpage_text) {
        Pattern actionPattern = Pattern.compile("ACTION=\"([^\"]+)");
        Matcher m = actionPattern.matcher(htmlpage_text);
        if (m.find()) {
            String action = m.group();
            return action.substring("ACTION=\"".length());
        } 
        return null;
    }

    /**
     * Main method.
     */
    void standaloneLogin(String username, String password) 
            throws ApperyUnitException {
        String loginUrl = protocol + host + "/apiexpress/rest/auth/login";
        
        HttpPost samlRequest = new HttpPost(loginUrl);
        samlRequest.addHeader(new BasicHeader("Content-Type", "application/x-www-form-urlencoded"));
        ArrayList postParameters = new ArrayList<NameValuePair>();
        postParameters.add(new BasicNameValuePair("j_username", username));
        postParameters.add(new BasicNameValuePair("j_password", password));
        samlRequest.setEntity(new UrlEncodedFormEntity(postParameters, "UTF-8"));
        def response = httpclient.execute(samlRequest, new HttpResponseHandler());
    }

}