import spock.lang.Specification
import spock.lang.Ignore
import io.appery.apperyunit.*;
import groovy.json.*

class ApperyClientSpec extends Specification {

  String username;
  String password;

  @Ignore
  def "Create Ionic 3 project on Beta"() {
  	setup:
    checkDebuggingCredentials()
    assert username!=null && password!=null

    ApperyClient ac = new ApperyClient()
    ac.host = 'beta.dev.appery.io'
    boolean ok = ac.doLogin(username, password, "/app/")
    assert ok, "Login successful"

    // Get project types
    //String projTypes = ac.makeGet('/app/rest/html5/plugin/wizardProject')
    //println "-- projTypes " + projTypes
    
    // Create Ionic 3 project
    String data = JsonOutput.toJson(["name":"PWA Pizza 3","templateId":"799304"])
    println "-- data: " + data
    String res = ac.makePost('/app/rest/projects', data)
    println "-- res: " + res
    // FIXME: Error 500 currently returned
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

}
