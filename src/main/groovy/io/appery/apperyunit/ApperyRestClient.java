package io.appery.apperyunit;

import java.io.IOException;
import java.util.List;
import org.apache.http.impl.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.*;
import org.apache.http.util.*;
import org.apache.http.client.*;
import org.apache.http.client.utils.*;
import org.apache.http.client.entity.*;
import org.apache.http.message.*;

/**
 * = Accessing Appery backend REST services.
 *
 * Appery REST services allow:
 * 
 * - to get the list of server-codes in some user's workspace.
 * - to download source of each server-code
 * - to get information about server-code dependencies, folders, parameters, etc.
 */
public class ApperyRestClient {

    CloseableHttpClient httpclient = HttpClients.createDefault();

    String host = "appery.io";
    String userName;

    List<ScriptJson> scripts;
    List<FolderJson> folders;

    /**
     * Performs HTTP GET.
     */
    String makeGet(String serviceUrl) throws IOException {
        HttpGet req = new HttpGet("https://" + host + serviceUrl);
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
    
    void setHost(String host) {
        this.host = host;
    }
    
}
