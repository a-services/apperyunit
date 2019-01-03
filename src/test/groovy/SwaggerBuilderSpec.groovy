import spock.lang.Specification
import spock.lang.Ignore
import io.appery.apperyunit.*;
import groovy.json.*

class SwaggerBuilderSpec extends Specification {
  
    ApperyClient ac;
    JsonSlurper jsonSlurper = new JsonSlurper() 
    
    String username;
    String password;    
    
    List scripts;
    List folders;
    
    @Ignore
    def "Generate swagger for single script"() {
        setup:
        loginAppery()
        scripts = jsonSlurper.parseText(ac.loadServerCodesList())
        folders = jsonSlurper.parseText(ac.loadServerCodesFolders())
        String outFile = '../swagger_ui/node_modules/swagger-ui-dist/swagger.json'
        SwaggerBuilder b = new SwaggerBuilder(outFile, ac, folders)
        
        String scriptName = 'account_calc_gaps'
        def sc = scripts.find { it.name==scriptName }
        b.load(sc as ScriptJson)
        b.saveResult()
    }

    @Ignore
    def "Generate swagger for the folder"() {
        setup:
        loginAppery()
        scripts = jsonSlurper.parseText(ac.loadServerCodesList())
        folders = jsonSlurper.parseText(ac.loadServerCodesFolders())
        String outFile = '../swagger_ui/node_modules/swagger-ui-dist/swagger.json'
        SwaggerBuilder b = new SwaggerBuilder(outFile, ac, folders)
        
        List subscripts = findAllScripts("5b71eebf2e22d734a2b35dc1")
        for (def script in subscripts) {
            b.load(script as ScriptJson)
        }
        b.saveResult()
    }
  
    @Ignore
    def "Generate swagger for all scripts"() {
        setup:
        loginAppery()
        scripts = jsonSlurper.parseText(ac.loadServerCodesList())
        folders = jsonSlurper.parseText(ac.loadServerCodesFolders())
        String outFile = '../swagger_ui/node_modules/swagger-ui-dist/swagger.json'
        SwaggerBuilder b = new SwaggerBuilder(outFile, ac, folders)
        
        for (int i=0; i<scripts.size(); i++) {
            if (scripts[i].executable) {
                println "script: ${scripts[i].name}"
                b.load(scripts[i] as ScriptJson)
            }
        }
        b.saveResult()
    }
    
    void loginAppery() {
        checkDebuggingCredentials()
        assert username!=null && password!=null

        ac = new ApperyClient()
        boolean ok = ac.doLogin(username, password, "/bksrv/")
        assert ok, "Login successful"
    }  
  
    void checkDebuggingCredentials() {
        String auDebug = System.getenv("AU_DEBUG");
        if (auDebug==null) {
            return;
        }
        String[] creds = auDebug.split(":");
        if (creds.length!=2) {
            return;
        }
        username = creds[0];
        password = creds[1];
    }
    
    List<String> findAllScripts(folderId) {
        def dt = new DepTracker()
        dt.trackFolderScripts(folderId)
        return new ArrayList(dt.result)
    }

    class DepTracker {

        List<String> result = new LinkedList()

        /**
         * Recursive support for `findAllScripts()`
         */
        void trackFolderScripts(String parentId) {
            List subfolders = folders.findAll { it.parentId==parentId }
            List subscripts = scripts.findAll { it.executable && it.folderId==parentId }
            result.addAll(subscripts)
            for (def folder in subfolders) {
                trackFolderScripts(folder._id);
            }
        }

    }
    
}