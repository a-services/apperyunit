/** 
= Emulate Appery Server Code call 

[cols="1,5", options="header"]
|===
| Date        | Path
|  1 Mar 2017 | /Users/eabramovich/Documents/RECENT/apperyunit/apperyunit/
| 11 Jan 2017 | /Users/eabramovich/Documents/2015/15-11/dyjet/KnowYE_ServerCode/apperyunit/src/main/groovy/apperyunit.groovy
| 15 Dec 2016 | /Users/eabramovich/Documents/16-04/Tak/ServerCode_Tak/apperyunit/src/main/groovy/apperyunit.groovy
|  6 Dec 2016 | /Users/eabramovich/Documents/RECENT/Jobster/Jobster_Server/apperyunit/src/main/groovy/apperyunit.groovy
| 25 Nov 2016 | C:\doc\recent\Jobster\Jobster_Server\apperyunit.groovy
| 24 Nov 2016 | /home/egor/prog/appery/Jobster/Jobster_Server/apperyunit/src/main/groovy/apperyunit.groovy
|===
*/

package io.appery.apperyunit

import groovy.json.*
import static io.appery.apperyunit.Utils.*

import javax.script.ScriptException

def asArray(resp) {
	List result = []
	int n = resp.size()
	for (i=0; i<n; i++) {
	    def obj = resp[i as String]
	    assert obj!=null
	    result.add(obj)
	}
	return result
}

/*
void downloadScriptsConsole() {
    String configFileName = 'apperyunit.json'
    File f = new File()
    if (!f.exists(configFileName)) {
        println "Configuration file not found: " + configFileName
        return
    }
    def jsonSlurper = new JsonSlurper()
    def config = jsonSlurper.parseText(f.text)

    assert config.username!=null
    assert config.password!=null
    assert config.scripts!=null
    assert config.scripts.size()>0

    new ApperyClient().downloadScriptsConsole(config.username, config.password, config.scripts)
}
*/

// --------- main ---------

println '-'*80
println new Date().toString().padLeft(80)

//debug_mode = System.getenv("AU_DEBUG")=='1'
if (args.size()==0) {
    // println "Parameters: (color)? (download)? script_name.js (param_list.paramlist)? (params_name.params)? (echo)? (test)? "
    // au -> DashboardFrame -> ApperyClient -> PasswordDialog
    DashboardFrame.main([] as String[])
    return
}

ensureFolder(fixturesFolder)
ensureFolder(paramsFolder)

cl = new CommandLine(args)
colorMode = cl.extractColorMode()

/*
downloadMode = cl.extractDownloadMode()
if (downloadMode) {
    downloadScriptsConsole()
    return
}
*/

scriptName = cl.extractScriptName()

sc = new ServerCode()
//sc.dependencyList scriptName + '.dependencies'
sc.addDependencies(scriptName)
    
try {
    paramList = cl.extractParamList()
    paramName = cl.extractParams()
} catch (ParamsException e) {
    println e.reason
    return
}

if (paramName!=null && paramList!=null) {
    println "No need to specify `.params` file when `.paramlist` is specified"
    return
}
/*
if (paramName==null && paramList==null) {
    println "You should specify `.params` or `.paramlist` file"
    return
}
*/
if (paramList==null) {
    paramList = [paramName] 
}

echoMode = cl.extractEchoMode()
testMode = cl.extractTestMode()
testFailed = false

println "Script name: $scriptName"
println "Echo mode: ${echoMode}"
println "Test mode: ${testMode}"
println '-'*80

try {
    for (String paramName in paramList) {
        String paramStr = '{}'
        if (paramName) {
            File pfile = new File(paramsFolder, paramName)
            paramStr = pfile.text
        }
        if (!new File(scriptName+'.js').exists()) {
            println "${red}[ERROR]$norm File not found: ${scriptName}.js"
            return
        }
        sc.run(scriptName+'.js', paramStr)
    }
    
} catch (ScriptException e) {
    sc.printScriptError(e)
    
} catch (TestFailedException et) {
    testFailed = true    
    
} catch (RuntimeException ef) {
    // FileNotFoundException for echo queries goes here
    testFailed = true  
    println "${red}[ERROR]$norm " + ef.message
    
} finally {
    ApperyCollection.httpclient.close()
}

if (testMode) {
    if (testFailed) {
        println red
        println "*"*80
        println "***${'TEST FAILED'.center(74)}***"
        println "*"*80
        println norm
        
    } else {
        println bold 
        println "*"*80
        println "***${'TEST SUCCEEDED'.center(74)}***"
        println "*"*80
        println norm
    }
}
