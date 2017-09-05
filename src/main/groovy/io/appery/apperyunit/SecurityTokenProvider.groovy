package io.appery.apperyunit;

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


/**
= Get Appery security token

Based on::
  /Users/eabramovich/Documents/RECENT/apperyunit/
  app-generator/apperyio-oauth-test/src/main/java/io/appery/utils/SecurityTokenProvider.java
*/  
class SecurityTokenProvider {
    
    String targetenvironment; 
    String user; 
    String password;
    String targetIdpUrl;
    
    SecurityTokenProvider(String host, String login, String password) {
        this.targetenvironment = host;
        this.user = login;
        this.password = password;   
    }
    
    String getToken(String moduleName) {
        String serviceURL = getServiceUrl(moduleName, "session-token/service");
        String token = "";
        
        CloseableHttpClient httpclient = HttpClients.createDefault();
        /*
        HttpClientBuilder builder = HttpClients.custom()
				.useSystemProperties()
                .setDefaultCookieStore(new BasicCookieStore())
				.setHostnameVerifier(new AllowAllHostnameVerifier());
        CloseableHttpClient httpclient = builder.build() 
        */
        
        try {
            /* Request to SP for service.
               Because of we are not authenticated, so we have to receive 200, but X-Appery-Status = 403
               Body content has HTTP Post Binding (Request) */
            Result response = stepOne_ServiceRequest(serviceURL, httpclient);
            if (response.status != 403) {
                throw new SecurityTokenException("Expected X-Appery-Status = 403 is not reached.");
            }
             
            /* Send HTTP Post Binding (Request) to IdP.
               Expected result: HTML with login form which should be submitted to
               <IdP_url>/j_security_check
               because IdP is protected with FORM authentication */
            response = stepTwo_IDPRequest(response.body, httpclient);

            /* Submit form with login and password to <IdP_url>/j_security_check
               Expected result if login and password correct - 302 redirect to  <IdP_url> */
            response = stepThree_SubmitLoginForm(user, password, httpclient);
            if (response.status != 302) {
                throw new SecurityTokenException("Expected 302 redirect is not reached. Check login password.");
            }
            
            /* Emulate redirect - sent GET to IdP
               Expected result - HTTP Post Binding Response (Response) - html document which should 
               be "auto-submitted" on next step */
            response = stepFour_receiveSAMLDoumentFromIDP(httpclient);
            
            /* Send HTTP Post Binding (Response) form content to SP
               Expected result: SP should redirect to original service url requested on 
               first step */
            response = stepFive_submitSAMLDoumentToSP(moduleName, response.body, httpclient);
            /*
            println "Status returned by step 5: "+ response.status
            println "+---- response.body: ----+"  
            println response.body
            println "+------------------------+"
            */
            if (response.body.equals(serviceURL)){
                response = stepOne_ServiceRequest(response.body, httpclient);
                if (response.status == 200 ){
                    /* We expect something like this in response body:
                       ```
                       <html><body><p>RMvzSmbPBnf7Sdv10sCy7Nje5qyHCs9J</p></body></html>
                       ```
                     */
                    token = response.body.substring(response.body.indexOf("p>") + 2, response.body.indexOf("</p>"));
                    
                } else { 
                    println "Status returned by step 5+1: "+ response.status + ", expected: 200"
                    println "Service URL: " + serviceURL
                    println "+---- response.body: ----+"  
                    println response.body
                    println "+------------------------+"
                    println "See:"
                    println "  https://en.wikipedia.org/wiki/Security_Assertion_Markup_Language"
                    println "  https://www.samltool.com/"
                    throw new SecurityTokenException("Service end point replied with "+ response.status +" Status code");
                }
                
            } else {
                
                println("Redirect from SP does not match original service URL");
                println("Expected: "+ serviceURL);
                println("Actual  : "+ response.body);
                
                throw new SecurityTokenException("SP reply with "+response.body);
            }
                
        } finally {
            httpclient.close()
        }
        
        return token
    }
    
    /**
     * At this step we request the target resource.
     *
     * @param url - service URL
     * @param client - HttpClient. Should keep cookies for the session.
     *
     * @return  -  if user is not logged in  auto-submit form.
     * @return  -  if user logged in  value of token.
     */
    protected Result stepOne_ServiceRequest(String url, CloseableHttpClient client) throws IOException {
        HttpGet attemptToGetToken = new HttpGet(url);
        Result result = client.execute(attemptToGetToken, new LocalResponseHandler());
        return result;
    }    

    protected Result stepTwo_IDPRequest(String htmlpage_text, CloseableHttpClient client) throws IOException {
        
        UrlEncodedFormEntity samlPost = new UrlEncodedFormEntity(
                    Arrays.asList(new BasicNameValuePair("SAMLRequest", 
                                  getSAMLDocumentFromPage(htmlpage_text))));
        targetIdpUrl = getActionEndpointURL(htmlpage_text)
        HttpPost postSamplToIdp = new HttpPost(this.targetIdpUrl);
        postSamplToIdp.setEntity(samlPost);
        postSamplToIdp.addHeader(new BasicHeader("Content-Type", "application/x-www-form-urlencoded"));
        Result result = client.execute(postSamplToIdp, new LocalResponseHandler());
        return result;
    }
    
    protected Result stepThree_SubmitLoginForm(String username, String password, CloseableHttpClient client) throws IOException {
        HttpPost postLoginFromToIdp = new HttpPost(targetIdpUrl + "j_security_check");
        HttpEntity loginForm = new UrlEncodedFormEntity(
                                    Arrays.asList(  new BasicNameValuePair("j_username", username), 
                                                    new BasicNameValuePair("j_password", password)));
        postLoginFromToIdp.addHeader(new BasicHeader("Content-Type", "application/x-www-form-urlencoded"));
        postLoginFromToIdp.addHeader(new BasicHeader("Referer", targetIdpUrl));
        postLoginFromToIdp.addHeader(new BasicHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"));
        postLoginFromToIdp.setEntity(loginForm);
        Result result = client.execute(postLoginFromToIdp, new LocalResponseHandler());
        return result;
    }
    
    protected Result stepFour_receiveSAMLDoumentFromIDP(CloseableHttpClient client) throws IOException {
        HttpGet idpRedirect = new HttpGet(targetIdpUrl);
        Result idpSamlRedirect = client.execute(idpRedirect, new LocalResponseHandler());
        return idpSamlRedirect;
    }
    
    /**
     * @return  expect redirect from SP to original request on step one.
     */
    protected Result stepFive_submitSAMLDoumentToSP(String moduleName, String html_text, CloseableHttpClient client) throws IOException {
        UrlEncodedFormEntity app_samlPost = new UrlEncodedFormEntity(
            Arrays.asList(new BasicNameValuePair("SAMLResponse", getSAMLDocumentFromPage(html_text))));
        String spUrl = getActionEndpointURL(html_text);
        HttpPost postSamplToSP = new HttpPost(spUrl);
        postSamplToSP.setEntity(app_samlPost);
        postSamplToSP.addHeader(new BasicHeader("Content-Type", "application/x-www-form-urlencoded"));
        Result appRedirect = client.execute(postSamplToSP, new LocalResponseHandler());
        return appRedirect;
    }
    
    /**
     *  Extract SAML document from form
     */
    protected String getSAMLDocumentFromPage(String htmlpage_text) {
        Pattern samlPattern = Pattern.compile("VALUE=\"([^\"]+)");
        Matcher m = samlPattern.matcher(htmlpage_text);
        String saml = "";
        while (m.find()) {
            saml = m.group();
            break;
        }
        return saml.substring("VAlUE=\"".length());
    }

    protected String getActionEndpointURL(String htmlpage_text) {
        Pattern actionPattern = Pattern.compile("ACTION=\"([^\"]+)");
        Matcher m = actionPattern.matcher(htmlpage_text);
        String action = "";
        while (m.find()) {
            action = m.group();
            break;
        }
        return action.substring("ACTION=\"".length());
    }

    protected String getServiceUrl(String moduleName, String servicePath) {
        return this.getSPUrl(moduleName) + servicePath;
    }

    protected String getSPUrl(String moduleName) {
        return "https://" + this.targetenvironment + "/" + moduleName + "/";
    }
    


    class Result {
        public String   body = "";
        public Integer  status = 0;
        
        public Result() {}
        
        public Result(Integer status, String body) {
            this.status = status;
            this.body = body;
        }
        
        public Result(HttpResponse resp){
            this.status = resp.getStatusLine().getStatusCode();
        }
    }
        
    class LocalResponseHandler implements ResponseHandler<Result> {
    
        @Override
        public Result handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
            Result resp = new Result(response);
            try {
                resp.body = dump(response);
                if (response.getStatusLine().getStatusCode() == 302){
                    //resp.body = dump(response);
                    resp.body = response.getLastHeader("Location").getValue();
                } else {
                    Header xApperyStatusHdr = response.getLastHeader("X-Appery-Status"); 
                    String xApperyStatus = (xApperyStatusHdr != null)? 
                                            xApperyStatusHdr.getValue() : null;        
                    if ( "403".equals(xApperyStatus) ){
                        resp.status = 403;
                    }
                    //resp.body = dump(response);
                }
            } catch (Exception ex){
                println "Handler Exception: " + ex.getMessage();
            }
            
            return resp;
        }   
        
        void dumpHeader(HttpResponse response) throws Exception {
            println(response.getStatusLine().getStatusCode());
            println(response.getStatusLine().getReasonPhrase());
            for (Header h : response.getAllHeaders()) {
                println(h.getName() + " = " + h.getValue());
            }
            /*
            for (Cookie c : getCookies().getCookies()) {
                log.debug("Cookie= " + c.toString());
            }; 
            */
        }
        
        String dump(HttpResponse response) throws Exception {
            //println("-----------");
            //dumpHeader(response);
            String body = "";
            if (response.getStatusLine().getStatusCode() < 300) {
                try {
                    BasicResponseHandler brh = new BasicResponseHandler();
                    body = brh.handleResponse(response);
                    //println(body);
                } catch (Exception ex) {
                    println("Dump Exception: " + ex.getLocalizedMessage());
                }
            }
            return body;
        }
        
    }
    
    
}
