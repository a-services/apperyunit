package io.appery.apperyunit

import groovy.json.*
import static io.appery.apperyunit.Utils.*

class SwaggerBuilder {
    
    String outFile;
    Map swagger;
    Map paths;
    ApperyClient apperyClient;
    List<FolderJson> folders;
    
    SwaggerBuilder(String outFile, ApperyClient apperyClient, List<FolderJson> folders) {
        this.outFile = outFile;
        this.apperyClient = apperyClient;
        this.folders = folders;

        swagger = [
            "openapi": "3.0.0",
            "info": [
              "title": "Appery.io Server Code",
              "description": "Server code functions",
              "version": "1.0.0"
            ],
            "servers": [
                [ "url": "https://api.appery.io/rest/1/code" ]
            ]
        ];
        
        paths = new LinkedHashMap();
    }
    
    void saveResult() {
        swagger.put("paths", paths)
        swagger.put("components", [
            "securitySchemes": [
                "ApperySecurity": [
                    "type": "apiKey",
                    "name": "X-Appery-Session-Token",                    
                    "in": "header"
                    ]    
                ]
            ]);
        /*
        swagger.put("securityDefinitions", [
            "ApperySecurity": [
                "type": "apiKey",
                "in": "header",
                "name": "X-Appery-Session-Token"                    
                ]    
            ]);
            */
        new File(outFile).text = JsonOutput.prettyPrint(JsonOutput.toJson(swagger))
        console "=== Swagger definitions saved: " + outFile
    }

    /**
     * Load Swagger definitions from Appery.
     */
    void load(ScriptJson script) {

        String method = "post"
        
        def serviceInfo = [
            "summary": script.name,
        ];
        
        // Add description with link to server-code in Appery
        String description = ""
        if (!isEmpty(script.description)) {
            description = script.description 
        }
        description += "\n> [Source](https://appery.io/servercode/${script.guid}/edit)    \n   \n   \n"
        serviceInfo.put("description", description)
        
        // Add parent folders as tags 
        List<String> parentFolders = getParentFolders(script.folderId)
        if (parentFolders.size()>0) {
            serviceInfo.put("tags", [parentFolders.reverse().join(" / ")])
        }
        
        if (!isEmpty(script.database)) {
            serviceInfo.put("security", [["ApperySecurity": []]])
        }
        
        def details = apperyClient.jsonSlurper.parseText(apperyClient.downloadScript(script.guid));
        if (details.testParams!=null) {
            
            method = details.testParams.requestMethod.toLowerCase();
            
            if (!jsonEmpty(details.testParams.urlParameters)) {
                serviceInfo.put("parameters", convertQueryParameters(details.testParams.urlParameters))
            }

            if (method.equals("post")) {
                serviceInfo.put("requestBody",[
                    "content": convertBodyParameters(details.testParams.body, details.testParams.bodyMimeType)
                ]);
            }
        } 
        
        serviceInfo.put("responses", [
          "200": [ "description": "Successful operation" ],
          "default": [ "description": "Unexpected error" ]    
          ]);
    
        Map info = new LinkedHashMap();
        info.put(method, serviceInfo)
        paths.put("/${script.guid}/exec", info);
        console "Swagger: ${bold}${script.name}.js${norm}"
    }
    
    List convertQueryParameters(urlParameters) {
        return urlParameters.collect { key,value -> [
                "name": key,
                "example": value,    
                "in": "query",
                "schema": [
                    "type": "string"
                ]
            ]}
    }

    Map convertBodyParameters(bodyStr, bodyMimeType) {
        /*
        Map result = new LinkedHashMap();
        try {
            if (isEmpty(bodyStr)) {
                result = [ : ]
            }
            if (bodyMimeType=='application/json') {
                def body = apperyClient.jsonSlurper.parseText(bodyStr)
                def properties = body.collect { key,value -> [
                    "name": key,
                    "value": value    
                    ]}
                result = [
                    "type": "object",
                    "properties": properties,
                    "type": "string"
                    ]
            } else {
                result = [ : ]
            }
        } catch (JsonException e) {
            result = [ : ]
        }
        */
        Map res = new LinkedHashMap();
        res.put(bodyMimeType, [ "schema": [ "example": bodyStr ] ])
        return res
    }
    
    boolean jsonEmpty(json) {
        if (json==null) {
            return true
        } 
        return JsonOutput.toJson(json).equals("{}")
    }
    
    List<String> getParentFolders(String folderId) {
        List<String> result = [];
        while (folderId!=null) {
            def folder = folders.find{ it._id==folderId }
            result.add(folder.name);
            folderId = folder.parentId;
        }
        return result;
    }
    
   
}
