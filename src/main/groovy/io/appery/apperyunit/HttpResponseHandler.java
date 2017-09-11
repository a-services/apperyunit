package io.appery.apperyunit;

import java.io.IOException;
import org.apache.http.impl.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.*;
import org.apache.http.util.*;
import org.apache.http.client.*;
import org.apache.http.client.utils.*;
import org.apache.http.client.entity.*;
import org.apache.http.message.*;

/**
 * ResponseHandler for HttpClient. 
 */
public class HttpResponseHandler implements ResponseHandler<HttpResultObj> {

    @Override
    public HttpResultObj handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
        HttpResultObj resp = new HttpResultObj(response);
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
            System.err.println("Handler Exception: " + ex.getMessage());
        }
        
        return resp;
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
                System.err.println("Dump Exception: " + ex.getLocalizedMessage());
            }
        }
        return body;
    }
}
