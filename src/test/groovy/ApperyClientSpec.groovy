import spock.lang.Specification
import spock.lang.Ignore
import io.appery.apperyunit.*;
import groovy.json.*

class ApperyClientSpec extends Specification {

  String username;
  String password;

  final PROJECT_IONIC_3_BLANK = 799304

  ApperyClient ac;

  void loginApperyBeta() {
    checkDebuggingCredentials()
    assert username!=null && password!=null

    ac = new ApperyClient()
    ac.host = 'beta.dev.appery.io'
    boolean ok = ac.doLogin(username, password, "/app/")
    assert ok, "Login successful"
  }

  @Ignore
  def "Export `home` page assets from `PWA Pizza` project"() {
    setup:
    loginApperyBeta()
    def proj = loadProject("PWA Pizza")
    assert proj!=null

    def homeAssets = loadPageAssets("home")
    assert homeAssets!=null

    saveJson(homeAssets, 'build/home_assets.json')
  }

  String projectGuid;

  def loadProject(String projectName) {
    def projList = ac.loadProjectList()
    def proj = projList.find { it.name==projectName }
    projectGuid = proj.guid
    return proj
  } 

  def loadPageAssets(String pageName) {
    assert projectGuid!=null
    def projInfo = ac.loadProjectInfo( projectGuid )
    def homePage = projInfo.assets.SCREEN.find { it.name==pageName }
    assert homePage!=null
    return ac.loadProjectAssets( projectGuid, [homePage.assetId] )
  }

  @Ignore
  def "Add button to Home page in Appery project"() {
  	setup:
    loginApperyBeta()
    def proj = loadProject("PWA Pizza 2")
    assert proj!=null
    
    def homeAssets = loadPageAssets("home")
    assert homeAssets.assets.size()==1
    def homeAsset = homeAssets.assets[0]
    
    def contentBeans = homeAsset.assetData.bean.children.bean
    assert contentBeans.size()==3 // header, body, footer
    def content = contentBeans[1].children.bean

    content.add(createNewButton("AU Button 3", "Button3"))
    ac.updateProjectAssets(proj.guid, homeAssets)
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

  void saveJson(jsonData, String fname) {
      new File(fname).text = JsonOutput.prettyPrint(JsonOutput.toJson(jsonData))
      println "-- `$fname` saved"
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
    loginApperyBeta()

    def tlist = ac.loadProjectTemplates()
    def t = tlist.templates.find { it.name=="Ionic3 Blank" }
    assert t.id==PROJECT_IONIC_3_BLANK && t.projectId==PROJECT_IONIC_3_BLANK

    def res = ac.createApperyProject("PWA Pizza 5", PROJECT_IONIC_3_BLANK)
    saveJson(res, "build/project_create.json")    
  }

}
