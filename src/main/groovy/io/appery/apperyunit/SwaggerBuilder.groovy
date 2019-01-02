package io.appery.apperyunit

import groovy.json.*
import static io.appery.apperyunit.Utils.*

class SwaggerBuilder {
    
    String outFile;
    Map swagger;
    Map paths;
    ApperyService apperyService;
    ApperyClient apperyClient;
    List<FolderJson> folders;
    
    SwaggerBuilder(ApperyService apperyService) {
        this.apperyService = apperyService;
        this.outFile = apperyService.swaggerOutputFile;
        this.apperyClient = apperyService.apperyClient;
        this.folders = apperyService.folders;
        /*
        swagger = new LinkedHashMap();
        swagger.put("swagger", "2.0")
        swagger.put("info", [
            "title": "Server Code",
            "description": "Server code functions",
            "version": "1.0.0"
        ])
        swagger.put("host", "api.appery.io")
        */
        swagger = [
            "swagger": "2.0",
            "info": [
              "title": "Server Code",
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
        console "Swagger definitions saved: " + outFile
    }

    /**
     * Load Swagger definitions from Appery.
     */
    void load(ScriptJson script) {
        def details = apperyClient.jsonSlurper.parseText(apperyClient.downloadScript(script.guid));
        String method = details.testParams.requestMethod.toLowerCase();
        def serviceInfo = [
            "summary": script.name,
        ];
        
        // Add description with link to server-code in Appery
        String description = ""
        if (script.description!=null) {
            description = script.description 
        }
        description += "\n\nhttps://appery.io/servercode/${script.guid}/edit"
        serviceInfo.put("description", description)
        
        // Add parent folders as tags 
        List<String> parentFolders = getParentFolders(script.parentId)
        if (parentFolders.size()>0) {
            serviceInfo.put("tags", parentFolders)
        }
        
        if (!jsonEmpty(details.testParams.urlParameters)) {
            serviceInfo.put("parameters": convertQueryParameters(details.testParams.urlParameters))
        }
        
        if (method.equals("post") && details.testParams.body!=null) {
            serviceInfo.put("parameters": [
                "name": "body",
                "in": "body",
                "description": "Request body",
                "schema": [
                    "properties": convertBodyParameters(details.testParams.body)
                ]
            ]);
        }
        
        serviceInfo.put("responses": [
          "200": [
            "description": "Successful operation"
          ]
        ]);
    
        paths.put("/${script.guid}/exec", [
            method: serviceInfo     
        ]);
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
    
    List<String> getParentFolders(String scriptParentId) {
        List<String> result = [];
        String parentId = scriptParentId;
        while (parentId!=null) {
            def folder = folders.find{ it.folderId==parentId }
            result.add(folder.name);
            parentId = folder.parentId;
        }
        return result;
    }
    
   
}
