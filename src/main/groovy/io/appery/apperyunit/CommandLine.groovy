package io.appery.apperyunit;

import groovy.json.*
import static io.appery.apperyunit.Utils.*

class CommandLine extends CommandLineBase {

    
    CommandLine(args) {
        this.args = args;
        initColors() 
    }
    
    String extractScriptName() {
        String scriptNameExt = args[pos];
        assert scriptNameExt.endsWith('.js');
        pos++;
        script_name = scriptNameExt[0..-4];
        return script_name;
    }
	
    /**
     * Returns the name of `.params` file specified in command-line
     * or taken from the script name.
     */
    String extractParams() throws ParamsException {
        String result = null
        if (pos<args.size()) {
            if (args[pos].endsWith('.params')) {
                result = args[pos]
                pos++
                verifyParamsFile(result)
            }
        } 
        if (result==null) {
            File params = new File(paramsFolder, script_name+'.params')
            //println "Name: ${params.path}, exists: ${params.exists()}"
            if (params.exists()) {
                result = script_name+'.params'
                verifyParamsFile(result)
            }
        }
        return result
    }
    
    void verifyParamsFile(String paramsName) throws ParamsException {
        File f = new File(paramsFolder, paramsName)
        if (!f.exists()) {
            throw new ParamsException("File not found ${paramsFolder}/${paramsName}")
        }
        String result = f.text
        String json = ServerCode.extractBody(result)[0].trim()
        if (json.length()==0) {
            throw new ParamsException("Empty file: ${script_name}.params")
        }
        if (JsonOutput.toJson(json)==null) {
            throw new ParamsException("Invalid JSON: ${script_name}.params")
        }        
    }
    
    /**
     * [[paramlist]]
     * === `.paramlist` file
     * 
     * Each new test scenario requires its own parameters, so we need a set 
     * of `.params` files associated with the server code script. 
     * This set can be specified with the `.paramlist` file.
     * 
     * Each line in `.paramlist` is a name of `.params` file,
     * or it can be empty or commented out with `#` character.
     */
    List<String> extractParamList() throws ParamsException {
        List<String> result = null
        if (pos<args.size()) {
            if (args[pos].endsWith('.paramlist')) {
                String paramListName = args[pos]
                result = new File(paramsFolder, paramListName).findAll { 
                    String s = it.trim()
                    boolean skip = s.length()==0 || s[0]=='#'
                    if (!skip) {
                        if (!s.endsWith('.params')) {
                            throw new ParamsException("Invalid file name in `$paramListName`: `$s`. Should end with `.params`")
                        }
                        verifyParamsFile(s)
                    }
                    return !skip
                }
                pos++
            }
        } 
        return result
    }
    
    

    
}
