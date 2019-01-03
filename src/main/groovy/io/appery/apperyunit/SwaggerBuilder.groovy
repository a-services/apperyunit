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
            "swagger": "2.0",
            "info": [
              "title": "Appery.io Server Code",
              "description": "Server code functions",
              "version": "1.0.0"
            ],
            "host": "api.appery.io",
            "basePath": "/rest/1/code",
            "schemes": [
              "https"
            ]
        ];
        
        paths = new LinkedHashMap();
    }
    
    void saveResult() {
        swagger.put("paths", paths)
        swagger.put("securityDefinitions", [
            "ApperySecurity": [
                "type": "apiKey",
                "in": "header",
                "name": "X-Appery-Session-Token"                    
                ]    
            ]);
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
        String description = "https://appery.io/servercode/${script.guid}/edit <br>\n"
        if (script.description!=null) {
            description += script.description 
        }
        serviceInfo.put("description", description)
        
        // Add parent folders as tags 
        List<String> parentFolders = getParentFolders(script.folderId)
        if (parentFolders.size()>0) {
            serviceInfo.put("tags", [parentFolders.reverse().join(" / ")])
        }
        
        if (script.database!=null) {
            serviceInfo.put("security", [["ApperySecurity": []]])
        }
        
        def details = apperyClient.jsonSlurper.parseText(apperyClient.downloadScript(script.guid));
        if (details.testParams!=null) {
            
            method = details.testParams.requestMethod.toLowerCase();
            
            if (!jsonEmpty(details.testParams.urlParameters)) {
                serviceInfo.put("parameters", convertQueryParameters(details.testParams.urlParameters))
            }

            if (method.equals("post") && details.testParams.body!=null && details.testParams.body.length()>0) {
                serviceInfo.put("parameters", [
                    "name": "body",
                    "in": "body",
                    "description": "Request body",
                    "schema": [
                        "properties": convertBodyParameters(details.testParams.body)
                        ]
                    ]);
            }
        } 
        
        serviceInfo.put("responses", [
          "200": [
            "description": "Successful operation"
                ]
            ]);
    
        Map info = new LinkedHashMap();
        info.put(method, serviceInfo)
        paths.put("/${script.guid}/exec", info);
        console "Swagger: ${bold}${script.name}.js${norm}"
    }
    
    List convertQueryParameters(urlParameters) {
        return urlParameters.collect { key,value -> [
                "name": key,
                "value": value,    
                "in": "query",
                "type": "string"
            ]}
    }

    List convertBodyParameters(bodyStr) {
        try {
            def body = apperyClient.jsonSlurper.parseText(bodyStr)
            return body.collect { key,value -> [
                "name": key,
                "value": value,    
                "type": "string"
                ]}
        } catch (JsonException e) {
            return []
        }
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
