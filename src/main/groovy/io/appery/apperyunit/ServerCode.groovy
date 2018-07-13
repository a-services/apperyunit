package io.appery.apperyunit

import javax.script.*
import groovy.json.*
import javax.script.ScriptException
import static io.appery.apperyunit.Utils.*

/*
See:
|===
| Nashorn API: |  https://docs.oracle.com/javase/8/docs/jdk/api/nashorn/
| Nashorn Guide: | https://docs.oracle.com/javase/8/docs/technotes/guides/scripting/nashorn/toc.html
| Tutorial by Benjamin Winterberg: | http://winterbe.com/posts/2014/04/05/java8-nashorn-tutorial/
|===
 */
class ServerCode {

    def jsEngine;
    String jsSource = '';
    int linesInHeader;
    String jsFile;
    int testNum = 0;

    String sourceFile = 'source.js'
    ApperyClient apperyClient;

    /**
     * Run servercode file with given parameters
     */
    void run(String jsFile, String jsonParams) {
        this.jsFile = jsFile
        jsEngine = new ScriptEngineManager().getEngineByName("JavaScript")
        int nh1 = jsSource.count('\n')
        dependency(jsFile)

        if (jsonParams.trim().length()==0) {
            jsonParams = '{}'
        }

        String[] params = extractBody(jsonParams)
        /*
        println "...Printing params"
        for (p in params) {
            println p
            println '....'
        }
        */
        String Apperyio_requestParams = params[0].trim()
        console " $bold Params:$norm $Apperyio_requestParams"

        /*
        println "...Printing headers"
        String Apperyio_headers = '{}'
        if (params.size()>1 && params[1]!=null) {
        	Apperyio_headers = params[1]
            println " $bold Headers:$norm $Apperyio_headers"
        }
        */
        String Apperyio_body = '{}'
        if (params.size()>1 && params[1]!=null) {
        	Apperyio_body = params[1].trim().replace('\n','\\n')
            console " $bold Body:$norm $Apperyio_body"
        }
        console '.'*80

        // Template for JavaScript stub
        String jsHeader = """
            var Apperyio_requestParams = $Apperyio_requestParams;
            var Apperyio_body = '$Apperyio_body';

            var Apperyio_responseSuccess, Apperyio_responseError;

            var Apperyio = {

                request: {
                    keys: function () {
                        return Object.keys(Apperyio_requestParams);
                    },
                    get: function (k) {
                        return Apperyio_requestParams[k];
                    },
                    body: function() {
                        return Apperyio_body;
                    }
                    /*
                    headers: function() {
                        return Apperyio_headers;
                    }*/
                },

                response: {
                    success: function (result) {
                        ApperyCollection.consoleLog('=== Apperyio response SUCCESS ===');
                        Apperyio_responseSuccess = JSON.stringify(result);
                        Apperyio_responseError = null;
                    },
                    binary: function (result) {
                        ApperyCollection.consoleLog('=== Apperyio binary response SUCCESS ===');
                        Apperyio_responseSuccess = JSON.stringify(result);
                        Apperyio_responseError = null;
                    },
                    error: function (result, code) {
                        ApperyCollection.consoleLog('=== Apperyio response ERROR ' + code + ' ===');
                        var err = JSON.stringify(result);
                        ApperyCollection.consoleLog(err);
                        Apperyio_responseSuccess = null;
                        Apperyio_responseError = err;
                    }
                },

                PN: {
                    send: function(pushAPIKey, messageData) {
                        ApperyCollection.sendPush(pushAPIKey, JSON.stringify(messageData));
                        return {}
                    }
                }

            };

            var request = Apperyio.request, response = Apperyio.response;

            Apperyio.request.user = Apperyio_requestParams.user;
            Apperyio.request.headers = Apperyio_requestParams.headers;

            var Collection = {

                query: function(dbId, collectionName, jsParams, token) {
                    var par = jsParams? JSON.stringify(jsParams) : null;
                    var res = ApperyCollection.query(dbId, collectionName, par, token);
                    return JSON.parse(res);
                },

                distinct: function(dbId, collectionName, columnName, queryString, token) {
                    var par = queryString? JSON.stringify(queryString) : null;
                    var res = ApperyCollection.distinct(dbId, collectionName, columnName, par, token);
                    return JSON.parse(res);
                },

                createObject: function(dbId, collectionName, objectJSON, token) {
                    ApperyCollection.createObject(dbId, collectionName, JSON.stringify(objectJSON), token);\n\
                    return {}
                },

                updateObject: function(dbId, collectionName, objectId, objectJSON, token) {
                    ApperyCollection.updateObject(dbId, collectionName, objectId, JSON.stringify(objectJSON), token);
                    return {}
                },

                multiUpdateObject: function(dbApiKey, collectionName, queryString, updateJSON, operationsJSON, token) {
                    ApperyCollection.multiUpdateObject(dbApiKey, collectionName, queryString, JSON.stringify(updateJSON), JSON.stringify(operationsJSON), token);
                    return {}
                },

                retrieveObject: function(dbId, collectionName, objectId, include, token) {
                    /* var result = XHR.send('GET',
                      'https://api.appery.io/rest/1/code/e57dbc44-773f-4b3b-b0d7-006bd7df7f10/exec',
                      { 'parameters': { 'db':dbId, 'c':collectionName, 'id':objectId, 'inc':include, 't':token }});
                    return JSON.parse(result.body); */

                    // doc says there also should be `proj` in signature
                    var res = ApperyCollection.retrieveObject(dbId, collectionName, objectId, include, token);
                    return JSON.parse(res);
                },

                deleteObject: function(dbApiKey, collectionName, objectId, token) {
                    ApperyCollection.deleteObject(dbApiKey, collectionName, objectId, token);
                    return {}
                },

                multiDeleteObject: function(dbApiKey, collectionName, queryString, token) {
                    ApperyCollection.multiDeleteObject(dbApiKey, collectionName, JSON.stringify(queryString), token);
                    return {}
                },

                getCollectionList: function(dbApiKey) {
                    var result = XHR.send('GET',
                      'https://api.appery.io/rest/1/code/f9d25b89-406b-44c8-839b-85d8359c2fd9/exec',
                      { 'parameters': { 'db':dbApiKey }});
                    return JSON.parse(result.body);
                },
            };

            var XHR2 = {
                send: function(method, url, options) {
                    return XHR.send(method, url, options);
                }
            };

            var XHR = {
                send: function(method, url, options) {
                    var result = ApperyCollection.xhrCall(method, url, JSON.stringify(options));
                    return JSON.parse(result);
                }
            };

            var ScriptCall = {
                call: function(scriptID, params, body, bodyMimeType) {
                    ApperyCollection.scriptCall(scriptID, JSON.stringify(params), body, bodyMimeType);
                    return {}
                }
            };

            var DatabaseUser = {
                login: function(dbApiKey, username, password, masterApiKey) {
                    var res = ApperyCollection.login(dbApiKey, username, password, masterApiKey);
                    return JSON.parse(res);
                },
                retrieve: function(dbId, objectId, include, token) {
                    /* var result = XHR.send('GET',
                      'https://api.appery.io/rest/1/code/325a0706-3cf6-42e5-b2f4-9aa25769563c/exec',
                      { 'parameters': { 'db':dbId, 'id':objectId, 'inc':include, 't':token }});
                    return JSON.parse(result.body); */
                    var res = ApperyCollection.retrieveUser(dbId, objectId, include, token);
                    return JSON.parse(res);
                },
                query: function(dbId, params, token) {
                    var res = ApperyCollection.queryUser(dbId, JSON.stringify(params), token);
                    return JSON.parse(res);
                },
                update: function(dbId, objectId, userJSON, token) {
                    return ApperyCollection.updateUser(dbId, objectId, JSON.stringify(userJSON), token);
                },
                signUp: function(dbId, userJSON) {
                    var res = ApperyCollection.signUpUser(dbId, JSON.stringify(userJSON));
                    return JSON.parse(res);
                }
            };

        """
        jsSource = jsHeader + jsSource
        int nh2 = jsHeader.count('\n')
        linesInHeader = (nh1 + nh2 + 2)
        console ((linesInHeader + ' lines of header').padLeft(80))
        new File(fixturesFolder, sourceFile).text = jsSource

        jsEngine.put('console', new ApperyConsole())
        jsEngine.put('ApperyCollection', new ApperyCollection(jsFile))
        jsEngine.put('ApperyClient', new ApperyClient())
        //jsEngine.put('response', new ApperyResponse())
        long t = System.currentTimeMillis()
        jsEngine.eval(jsSource)
        saveScriptResult(jsFile)
        t = System.currentTimeMillis() - t
        console "%%% Script takes $t ms to execute in apperyunit %%%"
    }

    void saveScriptResult(String jsFile) {
        String scriptName = jsFile[0..-4]
        testNum++
        def respSuccess = getResponseSuccess()
        def respError = getResponseError()
        if (respSuccess==null && respError==null) {
            respSuccess = []
        }
        if (respSuccess!=null) {
            String outName = scriptName+(testNum==1?"":"."+testNum) + '.success.json'
            String folderName = fixturesFolder + '/' + scriptName
            ensureFolder(folderName)
            String jsonStr = JsonOutput.prettyPrint(JsonOutput.toJson(respSuccess))

            if (test_mode) {
                ApperyCollection.assertEquals(jsonStr, outName)
            } else {
                console "Saving request to ${ital}`$outName`${norm}"
                new File(folderName, outName).text = jsonStr
            }
        }
        if (respError!=null) {
            console "\n${ital}Response error:${norm} $respError"
        }
        console ""
    }

    def getResponseSuccess() {
        def resp = jsEngine.get('Apperyio_responseSuccess')
        return (resp!=null)? ApperyCollection.jsonSlurper.parseText(resp) :null;
    }

    def getResponseError() {
        def resp = jsEngine.get('Apperyio_responseError')
        return (resp!=null)? ApperyCollection.jsonSlurper.parseText(resp) :null;
    }

    /**
     * Add dependency library to output source.
     */
    void dependency(String jsFile) {
        jsSource += '\n// ------ ' + jsFile + '\n' + new File(jsFile).text
    }

    void addDependencies(String scriptName) {
        DepTracker dt = new DepTracker()
        dt.loadJsonDependencies()
        dt.collectDeps(scriptName)
        dt.result.each { jsFile ->
            if (jsFile.length()>0) {
                jsSource += '\n// ------ ' + jsFile + '\n' + new File(jsFile + '.js').text
            }
        }
    }

    /**
     * Used to collect dependency records recursively,
     * storing them at `result`.
     */
    class DepTracker {

        Map jsonDeps = [:];
        List<String> result = new LinkedList()

        void loadJsonDependencies() {
            File f = new File('dependencies.json')
            if (f.exists()) {
                jsonDeps = new JsonSlurper().parseText(f.text)
            }
        }

        /**
         * Recursive support for `addDependencies()`
         */
        void collectDeps(String scriptName) {
            List scriptDeps = jsonDeps[scriptName]
            if (scriptDeps==null) {
                return
            }
            for (String dep in scriptDeps) {
                collectDeps(dep)
                result.add(dep)
            }
        }

    }

    /**
     * Support deprecated `.dependencies` format.
     * @deprecated
     */
    void dependencyList(String depFile) {
        File dep = new File(paramsFolder, depFile)
        if (!dep.exists()) {
            dep = new File(paramsFolder, 'default.dependencies')
            if (!dep.exists()) {
                return
            }
        }
        dep.eachLine { jsFile ->
            jsFile = jsFile.trim()
            if (jsFile.length()>0) {
                jsSource += '\n// ------ ' + jsFile + '\n' + new File(jsFile).text
            }
        }
    }

    /**
     * Body can also be specified in JSON file with parameters.
     * In that case there should be a line of 4 dashes: `----`
     * and all the text following it will be treated as request body.
     */
    static String[] extractBody(text) {
        //return text.split(/\r?\n----\r?\n/)
        def m = text =~ /\r?\n----\r?\n/
        if (!m) {
            return [ text, null ]
        } else {
            return [ text.substring(0,m.start()), text.substring(m.end()) ]
        }
    }

    void printScriptError(ScriptException e) {
        int lno = e.lineNumber
        int rno = lno-linesInHeader
        int cno = e.columnNumber
        String errMsg = e.message

        console '*'*80
        console "${ital}Error in `$jsFile` at line ${rno}:$norm"
        console errMsg
        console '.'*80

        String errLine = jsSource.split('\n')[lno-1]
        String prefix = rno+':'
        console ""
        console "${ital}${prefix}$norm $errLine"
        if (cno!=-1) {
            console ' '*(cno+prefix.length()+1)+'^'
        }
        console ""
    }

}
