package io.appery.apperyunit;

import org.apache.http.HttpResponse;

/**
 * Wrapper object returned by HttpClient.
 */
public class HttpResultObj {
    public String   body = "";
    public Integer  status = 0;
    
    public HttpResultObj() {}
    
    public HttpResultObj(Integer status, String body) {
        this.status = status;
        this.body = body;
    }
    
    public HttpResultObj(HttpResponse resp){
        this.status = resp.getStatusLine().getStatusCode();
    }
}