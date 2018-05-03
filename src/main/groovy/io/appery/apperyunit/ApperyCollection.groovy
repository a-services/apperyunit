package io.appery.apperyunit

import org.apache.http.impl.client.*
import org.apache.http.client.methods.*
import org.apache.http.*
import org.apache.http.util.*
import org.apache.http.client.*
import org.apache.http.client.utils.*
import org.apache.http.client.entity.*
import org.apache.http.message.*
import org.apache.http.entity.*

//import com.google.gson.*
import groovy.json.*
import static io.appery.apperyunit.Utils.*

import jdk.nashorn.api.scripting.ScriptUtils

class ApperyCollection {
    
    static CloseableHttpClient httpclient = HttpClients.createDefault();
    
    static def jsonSlurper = new JsonSlurper()
    //static Gson gson = new Gson();
    int queryCount = 0, updateCount = 0;
    String scriptName;
    String outFolder;
    
    boolean echoMode, testMode;
    
    ApperyCollection(String scriptName) {
        this.scriptName = scriptName[0..-4] // without '.js' extension
        outFolder = fixturesFolder + '/' + this.scriptName
        ensureFolder(outFolder)

        echoMode = echo_mode
        testMode = test_mode
    }
    
    /*
    static def deepMap(jsParams) {
        //println "deepMap class: "+jsParams.getClass().getName()
        if (jsParams instanceof jdk.nashorn.internal.runtime.Undefined) {
            return ''
        }
        if (jsParams instanceof Integer ||
            jsParams instanceof Long ||
            jsParams instanceof Double ||
            jsParams instanceof Boolean ||
            jsParams instanceof String) {
            return jsParams
        }
        if (jsParams instanceof jdk.nashorn.internal.objects.NativeNumber) {
            def n = jsParams.getDefaultValue(Number)
            //println "n = $n | class = ${n.getClass().getName()}" 
            //return removeNaN(n)
            return n
        }
        if (jsParams instanceof jdk.nashorn.internal.runtime.ConsString) {
            return jsParams as String
        }
        if (jsParams instanceof jdk.nashorn.internal.objects.NativeDate ||
            jsParams instanceof jdk.nashorn.internal.objects.NativeRegExp) {
            return jsParams.getDefaultValue()
        }
        if (jsParams instanceof jdk.nashorn.internal.objects.NativeArray) {
            def a = ScriptUtils.convert(jsParams, double[].class)
            //for (int i=0; i<a.length; i++) {
            //    a[i] = removeNaN(a[i])
            //}
            return a
        }
        Map result = [:]
        def keys = jsParams.getOwnKeys(true)
        for (String key in keys) {
            def value = jsParams.get(key)
            result.put(key, deepMap(value))
        }
        return result
    }
    
    static Double removeNaN(Double x) {
        return x==null || Double.isNaN(x)? null: x
    }*/
    
    String makeGet(String urlPath, Map params, String dbId, token) {
        URIBuilder uriBuilder = new URIBuilder()
            .setScheme("https")
            .setHost("api.appery.io")
            .setPath(urlPath)
        Map params1 = collectQueryParams(params)    
        params1.each { name, value ->
            uriBuilder.addParameter(name, value.toString())
        }
        HttpGet httpGet = new HttpGet(uriBuilder.build());
        httpGet.addHeader("X-Appery-Database-Id", dbId)
        
        //print "token = $token, nashornEmptyValue(token) = " + nashornEmptyValue(token)
        if (!nashornEmptyValue(token)) {
            httpGet.addHeader("X-Appery-Master-Key", token)
        }
        
        ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

            @Override
            public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
                int status = response.getStatusLine().getStatusCode();
                console "--- response status: $status"
                
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;

                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            }

        };

        String responseBody = httpclient.execute(httpGet, responseHandler);
        String respJson = JsonOutput.prettyPrint(responseBody) //JsonOutput.prettyPrint(JsonOutput.toJson(responseBody))
        
        String fname = queryFileName()
        console "Saving result to $ital`$fname`$norm"
        new File(outFolder, fname).text = respJson
        
        sleep(500); // keep number of requests per second reasonable 
        return respJson
    }
    
    static void assertEquals(jsonStr, fname) {
        console "${ital}TEST:${norm} comparing with ${ital}`${fname}`${norm}"
        String outFolder = fixturesFolder + '/' + script_name
        String result = new File(outFolder, fname).text
        if (jsonStr!=result) {
            assert fname.endsWith('.json')
            String failedName = fname[0..-6] + '_failed.json'
            new File(outFolder, failedName).text = jsonStr
            console "     result saved as $red`$failedName`$norm"
            throw new TestFailedException(jsonStr, result)
        }
    }

    String nextUpdateParamFileName() {
        updateCount++
        return "update_r_${updateCount}.json"
    }

    String updateFileName() {
        return "update_${updateCount}.json"
    }

    String queryParamFileName() {
        return "query_r_${queryCount}.json"
    }

    String queryFileName() {
        return "query_${queryCount}.json"
    }

    boolean nashornEmptyValue(token) {
        return (token==null || 
               (token instanceof jdk.nashorn.internal.runtime.Undefined))
    }

    // filter undefined
    def fu(token) {
        return (token instanceof jdk.nashorn.internal.runtime.Undefined ? "undefined" : token)
    }
    
    String query(String dbId, 
                 String collectionName, 
                 String json /*jsParams*/, 
                 String token) {
        if (nashornEmptyValue(json)) {
            json = '{}'
        }     
        console "--> $bold`$collectionName`$norm query: "+JsonOutput.prettyPrint(json)
        /*
        Map params;
        if (json=='""') {
            params = [:]
        } else {
            params = gson.fromJson(json, LinkedHashMap.class)
        }
        */
        def params = jsonSlurper.parseText(json)
        Map traceJSON = [
                "operation": "query", 
                "collection": fu(collectionName),
                "params": fu(params)
        ]
        String traceJson = JsonOutput.prettyPrint(JsonOutput.toJson(traceJSON))
        //Map params = deepMap(jsParams) 
        queryCount++
        String fname = queryFileName()
        String pname = queryParamFileName()
        String result = '[]'
        if (echoMode) {
            if (testMode) {
                assertEquals(traceJson, pname)
            }
            console "ECHO MODE: reading from $fname" 
            result = new File(outFolder, fname).text
        } else {
            new File(outFolder, pname).text = traceJson
            console "Saving request to $ital`$pname`$norm"
            result = makeGet('/rest/1/db/collections/' + collectionName, params, dbId, token)
        }
        return result;
    }

    String distinct(String dbId, 
                    String collectionName, 
                    String columnName, 
                    String json /*queryString*/, 
                    String token) {
        if (nashornEmptyValue(json)) {
            json = '{}'
        }     
        console "--> $bold`$collectionName`$norm `$columnName` distinct: "+JsonOutput.prettyPrint(json)
        def params = jsonSlurper.parseText(json)
        Map traceJSON = [
                "operation": "distinct", 
                "collection": collectionName,
                "column": columnName,
                "params": params
        ]
        String traceJson = JsonOutput.prettyPrint(JsonOutput.toJson(traceJSON))
        queryCount++
        String fname = queryFileName()
        String pname = queryParamFileName()
        String result = '[]'
        if (echoMode) {
            if (testMode) {
                assertEquals(traceJson, pname)
            }
            console "ECHO MODE: reading from $fname" 
            result = new File(outFolder, fname).text
        } else {
            new File(outFolder, pname).text = traceJson
            console "Saving request to $ital`$pname`$norm"
            result = makeGet('/rest/1/db/collections/' + collectionName + '/distinct/' + columnName, 
                     params, dbId, token)
        }
        return result;
    }
    
    /* Creates a map of parameters to query Appery DB.
       Renames `criteria` parameter to `where`.
       Keeps track that `collName` in pointer queries goes before `_id`, like this:
         { "vendor": { "collName": "vendor", "_id": "580437d4e4b0dfb2708b0911" } }
       because other way gives 0 results:
         { "vendor": { "_id": "580437d4e4b0dfb2708b0911", "collName": "vendor" } }
         
       <overstrike>We are using Gson to parse script parameters because JsonSlurper sorts keys in objects alphabetically.</overstrike>
         https://github.com/google/gson
         https://github.com/google/gson/blob/master/UserGuide.md
     */
    Map collectQueryParams(Map params) {
        if (params['criteria']) {
            params.put('where', JsonOutput.toJson(params['criteria']))
            params.remove('criteria')
        }
        if (params['limit']) {
            params['limit'] = params['limit'] as Integer
        }
        return params
    }
    
    List query(dbId, collectionName, jsParams) {
        query(dbId, collectionName, jsParams, null)
    }
    
    void createObject(dbApiKey, collectionName, objectJSON) {
        createObject(dbApiKey, collectionName, objectJSON, null)
    }
    
    void createObject(String dbApiKey, 
                      String collectionName, 
                      String objectJSONstr, 
                      String token) {
        String fname = nextUpdateParamFileName()
        console "--> $bold`$collectionName`$norm create"
        Map traceJSON = [
                "operation": "createObject", 
                "collection": fu(collectionName),
                "objectJSON": jsonSlurper.parseText(objectJSONstr) 
        ]
        String traceJson = JsonOutput.prettyPrint(JsonOutput.toJson(traceJSON))
        if (testMode) {
            assertEquals(traceJson, fname)
        } else {
            console "Saving result to $ital`$fname`$norm"
            new File(outFolder, fname).text = traceJson
        }
    }
    
    void updateObject(dbApiKey, collectionName, objectId, objectJSON) {
        updateObject(dbApiKey, collectionName, objectId, objectJSON, null)
    }

    /* Callback function from JavaScript to update Appery DB collection.
       
       We are using Gson here to convert object to JSON string because
       `JsonOutput.toJson()` is throwing `java.lang.StackOverflowError`
       on big structures.
       
       P.S. Оказывается, что Gson возвращает при сериализации пустой объект.
       Вот что они пишут в документации:
       > Note that you can not serialize objects with circular references since that will result in infinite recursion.
       https://github.com/google/gson/blob/master/UserGuide.md#object-examples
     */
    void updateObject(String dbApiKey, 
                      String collectionName, 
                      String objectId, 
                      String objectJSONstr, 
                      String token) {
        String fname = nextUpdateParamFileName()
        console "--> $bold`$collectionName`$norm update"
        Map updateJSON = [
                "operation": "updateObject", 
                "collection": fu(collectionName),
                "objectId": fu(objectId),
                "objectJSON": jsonSlurper.parseText(objectJSONstr)  //deepMap(objectJSON)
        ]
        String traceJson = JsonOutput.prettyPrint(JsonOutput.toJson(updateJSON))
        if (testMode) {
            assertEquals(traceJson, fname)
        } else {
            console "Saving request to $ital`$fname`$norm"
            //String jsonStr = gson.toJson(objectJSON)
            new File(outFolder, fname).text = traceJson
        }
    }

    void multiUpdateObject(String dbApiKey, 
                           String collectionName, 
                           String queryString, 
                           String updateJSONstr, 
                           String operationsJSONstr, 
                           String token) {
        String fname = nextUpdateParamFileName()
        console "--> $bold`$collectionName`$norm multi-update"
        if (operationsJSONstr) {
            //String opsJson = JsonOutput.toJson(deepMap(operationsJSON)) 
            console "Operation: " + JsonOutput.prettyPrint(operationsJSONstr)
        }
        Map traceJSON = [
                "operation": "multiUpdateObject", 
                "collection": fu(collectionName),
                "queryString": fu(queryString),
                "updateJSON": jsonSlurper.parseText(updateJSONstr),  //deepMap(updateJSON),
                "operationsJSON": jsonSlurper.parseText(operationsJSONstr)  //deepMap(operationsJSON)
        ]
        String traceJson = JsonOutput.prettyPrint(JsonOutput.toJson(traceJSON))
        if (testMode) {
            assertEquals(traceJson, fname)
        } else {
            console "Saving request to $ital`$fname`$norm"
            new File(outFolder, fname).text = traceJson
        }
    }
    
    void updateUser(String dbApiKey, 
                    String objectId, 
                    String userJSONstr, 
                    String token) {
        String fname = nextUpdateParamFileName()
        console "--> Update User $objectId"
        Map traceJSON = [
                "operation": "updateUser", 
                "objectId": fu(objectId),
                "userJSON": jsonSlurper.parseText(userJSONstr)  //deepMap(userJSON)
        ]
        String traceJson = JsonOutput.prettyPrint(JsonOutput.toJson(traceJSON))
        if (testMode) {
            assertEquals(traceJson, fname)
        } else {
            console "Saving request to $ital`$fname`$norm"
            new File(outFolder, fname).text = traceJson
        }
    }
    
    String signUpUser(String dbApiKey, String userJSONstr) {
        String fname = nextUpdateParamFileName()
        console "--> Sign Up User $userJSONstr"
        def userJSON = jsonSlurper.parseText(userJSONstr)
        Map traceJSON = [
                "operation": "signUp", 
                "userJSON": userJSON
        ]
        String traceJson = JsonOutput.prettyPrint(JsonOutput.toJson(traceJSON))
        if (testMode) {
            assertEquals(traceJson, fname)
        } else {
            console "Saving request to $ital`$fname`$norm"
            new File(outFolder, fname).text = traceJson
        }
        return '{ "_id":"user_id_123", "sessionToken":"session_token_123", "username":"' + userJSON.username +'" }'
    }
    
    void deleteObject(String dbApiKey, 
                      String collectionName, 
                      String objectId, 
                      String token) {
        String fname = nextUpdateParamFileName()
        console "--> Delete $objectId from $bold`$collectionName`$norm"
        Map objectJSON = [
            "operation": "deleteObject", 
            "collectionName": fu(collectionName),
            "objectId": fu(objectId) 
        ]
        String traceJson = JsonOutput.prettyPrint(JsonOutput.toJson(objectJSON))
        if (testMode) {
            assertEquals(traceJson, fname)
        } else {
            console "Saving request to $ital`$fname`$norm"
            new File(outFolder, fname).text = traceJson
        }
    }
    
    void multiDeleteObject(String dbApiKey, 
                           String collectionName, 
                           String queryString, 
                           String token) {
        String fname = nextUpdateParamFileName()
        console "--> Multi-delete from $bold`$collectionName`$norm"
        Map objectJSON = [
            "operation": "multiDeleteObject", 
            "collectionName": fu(collectionName),
            "query": fu(queryString) 
        ]
        String traceJson = JsonOutput.prettyPrint(JsonOutput.toJson(objectJSON))
        if (testMode) {
            assertEquals(traceJson, fname)
        } else {
            console "Saving request to $ital`$fname`$norm"
            new File(outFolder, fname).text = traceJson
        }
    }
    
    String retrieveUser(dbId, objectId, include, token) {
        queryCount++
        String fname = queryFileName()
        String pname = queryParamFileName()
        console "--> Retrieve User $objectId"
        Map traceObj = [
            "operation": "retrieveUser", 
            "objectId": fu(objectId),
            "include": fu(include) 
        ]
        String traceJson = JsonOutput.prettyPrint(JsonOutput.toJson(traceObj))
        String result = '[]'
        if (echoMode) {
            if (testMode) {
                assertEquals(traceJson, pname)
            }            
            console "ECHO MODE: reading from $fname" 
            result = new File(outFolder, fname).text
        } else {
            new File(outFolder, pname).text = traceJson
            Map params = [ 'db':dbId, 'id':objectId, 'inc':include, 't':token ]
            result = makeGet('/rest/1/code/325a0706-3cf6-42e5-b2f4-9aa25769563c/exec', params, dbId, token)
            /*
            Map params = [:]
            if (include) {
                params.put('include', include)
            }
            result = makeGet('/rest/1/db/users/' + objectId, params, dbId, token)
            */
        }
        return result;
    }

    def login(String dbApiKey, 
              String username, 
              String password, 
              token) {
        queryCount++
        String fname = queryFileName()
        String pname = queryParamFileName()

        console "--> login"
        Map traceObj = [
            "operation": "login", 
            "dbApiKey": fu(dbApiKey),
            "username": fu(username),
            "password": fu(password) 
        ]
        String traceJson = JsonOutput.prettyPrint(JsonOutput.toJson(traceObj))
        String result = '[]'
        if (echoMode) {
            if (testMode) {
                assertEquals(traceJson, pname)
            }
            console "ECHO MODE: reading from $fname" 
            result = new File(outFolder, fname).text
        } else {
            console "Saving request to $ital`$pname`$norm"
            new File(outFolder, pname).text = traceJson
            
            Map params = [ 'db':dbApiKey, 'u':username, 'p':password, 't':token ]
            result = makeGet('/rest/1/code/41708113-d64b-4322-9b2e-08ed8b5414f2/exec', params, dbApiKey, token)
            /*
            result = makeGet('/rest/1/db/login', 
                    [ username:username, password:password ],
                    dbApiKey, token)
            */        
        }
        return result;            
    }

    def queryUser(String dbApiKey, 
                  String params, 
                  token) {
        queryCount++
        String fname = queryFileName()
        String pname = queryParamFileName()

        console "--> queryUser"
        Map traceObj = [
            "operation": "queryUser", 
            "dbApiKey": fu(dbApiKey),
            "params":   fu(params) 
        ]
        String traceJson = JsonOutput.prettyPrint(JsonOutput.toJson(traceObj))
        String result = '[]'
        if (echoMode) {
            if (testMode) {
                assertEquals(traceJson, pname)
            }
            console "ECHO MODE: reading from $fname" 
            result = new File(outFolder, fname).text
        } else {
            console "Saving request to $ital`$pname`$norm"
            new File(outFolder, pname).text = traceJson
            
            Map p = [ 'db':dbApiKey, 'p':params, 't':token ]
            result = makeGet('/rest/1/code/4d640b84-a2b2-44bf-b997-f3f10cd77310/exec', p, dbApiKey, token)
        }
        return result;            
    }

    void scriptCall(String scriptID, 
                    String paramStr, 
                    String body, 
                    String bodyMimeType) {
        String fname = nextUpdateParamFileName()
        console " $bold >>> ScriptCall >>>$norm scriptID=$scriptID, params=$paramStr, body=[$body], bodyMimeType=$bodyMimeType"
        Map traceObj = [
            "operation": "scriptCall", 
            "scriptID": fu(scriptID),
            "body": fu(body),
            "bodyMimeType": fu(bodyMimeType) 
        ]
        String traceJson = JsonOutput.prettyPrint(JsonOutput.toJson(traceObj))
        if (testMode) {
            assertEquals(traceJson, fname)
        } else {
            console "Saving request to $ital`$fname`$norm"
            new File(outFolder, fname).text = traceJson
        }
    }
    
    String xhrCall(String method, 
                String url, 
                String optionStr) {
        //Map p = ApperyCollection.deepMap(options)
        def p = jsonSlurper.parseText(optionStr)
        String o = JsonOutput.prettyPrint(JsonOutput.toJson(p))
        console " $bold >>> XHR >>>$norm method=$method, url=$url"
        console "XHR options: $o"
        method = method?.toUpperCase()

        Map traceObj = [
            "operation": "xhrCall", 
            "method": fu(method),
            "url": fu(url),
            "options": p 
        ]
        String traceJson = JsonOutput.prettyPrint(JsonOutput.toJson(traceObj))
        
        URL u = new URL(url)
        URIBuilder uriBuilder = new URIBuilder()
                .setScheme(u.protocol)
                .setHost(u.host)
                .setPath(u.path)
                
        int status = 500 
        String respBody = ''

        ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

            @Override
            public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
                status = response.getStatusLine().getStatusCode();
                console "--- response status: $status"
                
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;

                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            }

        };

        if (method=='GET') {
            queryCount++
            String fname = queryFileName()
            String pname = queryParamFileName()

            if (echoMode) {
                if (testMode) {
                    assertEquals(traceJson, pname)
                }                
                console "ECHO MODE: reading from $fname" 
                status = 200
                respBody = new File(outFolder, fname).text
            } else {
                new File(outFolder, pname).text = traceJson
                p.parameters.each { name, value ->
                    uriBuilder.addParameter(name, value.toString())
                }
                HttpGet httpGet = new HttpGet(uriBuilder.build());
                //httpGet.addHeader("Content-Type", ContentType.APPLICATION_JSON)

                String responseBody = httpclient.execute(httpGet, responseHandler);
                respBody = JsonOutput.prettyPrint(responseBody)
                //respBody = responseBody;
                
                console "Saving result to $ital`$fname`$norm"
                new File(outFolder, fname).text = respBody
            }

        } else
        if (method=='POST') {        
            String pname = nextUpdateParamFileName()
            String fname = updateFileName()
            if (testMode) {
                assertEquals(traceJson, pname)
            } else {
                console "Saving request to $ital`$pname`$norm"
                new File(outFolder, pname).text = traceJson
            }
            status = 200
            File respFile = new File(outFolder, fname)
            if (respFile.exists()) {
                respBody = respFile.text
            } else {
                respBody = '{ "success":true }'
            }
            /*
            HttpPost httpPost = new HttpPost(uriBuilder.build());
            httpPost.addHeader("Content-Type", "application/json")
            httpPost.setEntity(new StringEntity(p.body, ContentType.APPLICATION_JSON))

            String responseBody = httpclient.execute(httpPost, responseHandler);
            String jsonStr = JsonOutput.prettyPrint(responseBody)
            println "Saving request to $ital`$fname`$norm"
            new File(outFolder, fname).text = jsonStr
            */    
        }
        return JsonOutput.toJson([ status: status, body: respBody ])
    }
    
    String retrieveObject(dbId, collectionName, objectId, include, token) {
        console "--> Retrieve object $objectId from $bold`$collectionName`$norm"
        queryCount++
        String fname = queryFileName()
        String pname = queryParamFileName()
        Map traceObj = [
            "operation": "retrieveObject", 
            "collection": fu(collectionName),
            "objectId": fu(objectId),
            "include": fu(include)
            //"proj": proj 
        ]
        String traceJson = JsonOutput.prettyPrint(JsonOutput.toJson(traceObj))
        String result = '[]'
        if (echoMode) {
            if (testMode) {
                assertEquals(traceJson, pname)
            }
            console "ECHO MODE: reading from $fname" 
            result = new File(outFolder, fname).text
        } else {
            new File(outFolder, pname).text = traceJson
            console "Saving request to $ital`$pname`$norm"
            Map params = [ 'db':dbId, 'c':collectionName, 'id':objectId, 'inc':include, 't':token ]
            result = makeGet('/rest/1/code/e57dbc44-773f-4b3b-b0d7-006bd7df7f10/exec', params, dbId, token)
            /*
            String url = '/rest/1/db/collections/' + collectionName + '/' + objectId
            if (collectionName=='_users') {
                url = '/rest/1/db/users/' + objectId
            }
            result = makeGet(url, [:], dbId, token)
            */
        }
        return result;
    }
    
    void sendPush(pushAPIKey, messageData) {
        String fname = nextUpdateParamFileName()

        Map traceJSON = [
            "operation": "sendPush", 
            "pushAPIKey": fu(pushAPIKey),
            "data": jsonSlurper.parseText(messageData) 
        ]
        String traceJson = JsonOutput.prettyPrint(JsonOutput.toJson(traceJSON))
        console "--> Push notification sent: " + JsonOutput.prettyPrint(messageData)
        if (testMode) {
            assertEquals(traceJson, fname)
        } else {
            console "Saving request to $ital`$fname`$norm"
            new File(outFolder, fname).text = traceJson
        }
    }
    
    void consoleLog(String msg) {
        console msg
    }

}
