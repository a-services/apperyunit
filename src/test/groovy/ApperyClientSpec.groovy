import spock.lang.Specification
import spock.lang.Ignore
import io.appery.apperyunit.*;
import groovy.json.*

class ApperyClientSpec extends Specification {

  String username;
  String password;

  final PROJECT_IONIC_3_BLANK = 799304

  ApperyClient ac;

  @Ignore
  def "Add button to Home page in Appery project"() {
  	setup:
    checkDebuggingCredentials()
    assert username!=null && password!=null

    ac = new ApperyClient()
    ac.host = 'beta.dev.appery.io'
    boolean ok = ac.doLogin(username, password, "/app/")
    assert ok, "Login successful"

    def projList = loadProjectList()
    def p = projList.find { it.name=="PWA Pizza 2"}
    assert p!=null
    
    def projInfo = loadProjectInfo(p.guid)
    def homePage = projInfo.assets.SCREEN.find { it.name=="home" }
    assert homePage!=null

    def homeAssets = loadProjectAssets(p.guid, [homePage.assetId])
    assert homeAssets.assets.size()==1
    def homeAsset = homeAssets.assets[0]
    
    def contentBeans = homeAsset.assetData.bean.children.bean
    assert contentBeans.size()==3 // header, body, footer
    def content = contentBeans[1].children.bean

    content.add(createNewButton("AU Button 7", "Button7"))
    updateProjectAssets(p.guid, homeAssets)
  }

  Map createNewButton(String buttonText, String componentName) {
  	return [
      "@id": UUID.randomUUID().toString().toUpperCase(),
      "@type": "Ionic3ButtonBean",
      "children": [
        "bean": []
      ],
      "property": [
        "text": buttonText,
        "width": "default",
        "size": "default",
        "btnType": "ion-button",
        "style": "default",
        "mode": "default",
        "round": "false",
        "strong": "false",
        "ion3Color": "",
        "icon": [
          "style": "none",
          "className": "",
          "color": "",
          "position": "default"
        ],
        "iconOnly": "false",
        "advancedProperties": [
          "(click)": ""
        ],
        "componentName": componentName,
        "className": ""
      ]
    ]
  }

  def updateProjectAssets(String projectGuid, assetsData) {
  	String data = JsonOutput.toJson(assetsData)
    String fname = "build/asset_update.json"
    new File(fname).text = data
    println "-- Project asset update request stored to `$fname`"
    String result = ac.makePut('/app/rest/html5/project/' + projectGuid + '/asset/data', data)
    println "-- Project asset update response: " + result
  }

  def loadProjectAssets(String projectGuid, List<String> assets) {
  	String data = JsonOutput.toJson(["assets": assets.collect { ['id':it] }])
    println "-- data: " + data
    String result = ac.makePost('/app/rest/html5/project/' + projectGuid + '/asset/data', data)
    String fname = "build/project_assets.json"
    new File(fname).text = result
    println "-- Project assets load response stored to `$fname`"
    return new JsonSlurper().parseText(result)
  }

  def loadProjectInfo(String guid) {
    String result = ac.makeGet('/app/rest/html5/project', ['guid':guid])
    String fname = "build/project_info.json"
    new File(fname).text = result
    println "-- Project info response stored to `$fname`"
    return new JsonSlurper().parseText(result)
  }

  def loadProjectList() {
    String result = ac.makeGet('/app/rest/projects')
    String fname = "build/project_list.json"
    new File(fname).text = result
    println "-- Project list response stored to `$fname`"
    return new JsonSlurper().parseText(result)
  }

  def loadProjectTemplates() {
    String result = ac.makeGet('/app/rest/html5/plugin/wizardProject')
    String fname = "build/project_types.json"
    new File(fname).text = result
    println "-- Project types response stored to `$fname`"
    return new JsonSlurper().parseText(result)
  }

  def createApperyProject(String projectName, int projectType) {
    String data = JsonOutput.toJson(["name":projectName,"templateId":projectType])
    println "-- data: " + data
    String result = ac.makePost('/app/rest/projects', data)
    String fname = "build/project_create.json"
    new File(fname).text = result
    println "-- Project creation response stored to `$fname`"
    return new JsonSlurper().parseText(result)
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

  @Ignore
  def "Create Ionic 3 project on Beta"() {
  	setup:
    checkDebuggingCredentials()
    assert username!=null && password!=null

    ac = new ApperyClient()
    ac.host = 'beta.dev.appery.io'
    boolean ok = ac.doLogin(username, password, "/app/")
    assert ok, "Login successful"
    
    def tlist = loadProjectTemplates()
    def t = tlist.templates.find { it.name=="Ionic3 Blank" }
    assert t.id==PROJECT_IONIC_3_BLANK && t.projectId==PROJECT_IONIC_3_BLANK

    createApperyProject("PWA Pizza 5", PROJECT_IONIC_3_BLANK)    
  }

}
