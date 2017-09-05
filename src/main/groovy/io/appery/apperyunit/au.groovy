/** 
 * Emulate Appery Server Code call.
 * 
 * Calling classes: 
 *   `au` -> `DashboardFrame` -> `ApperyClient` -> `PasswordDialog`
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

// --------- main ---------

println '-'*80
println new Date().toString().padLeft(80)

if (args.size()==0) {
    DashboardFrame.main([] as String[])
    return
}

ensureFolder(fixturesFolder)
ensureFolder(paramsFolder)

cl = new CommandLine(args)
colorMode = cl.extractColorMode()

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
