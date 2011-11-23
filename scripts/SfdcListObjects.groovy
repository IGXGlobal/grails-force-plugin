/**
 * Gant Script to generate a single object from the Salesforce org. In order to generate
 * objects, the plugin's Salesforce configuration must be complete in the Config.groovy file.
 * 
 * @author Carlos.Munoz
 */
includeTargets << grailsScript("_GrailsSettings")
includeTargets << grailsScript("_GrailsBootstrap")
includeTargets << grailsScript("_GrailsArgParsing")
includeTargets << grailsScript("_GrailsEvents")

target(default:"Print a list of Salesforce objects available for generation to stdout") {
    depends(checkVersion, configureProxy, packageApp, classpath, loadApp, configureApp, parseArguments)
    
   
    
    // Code generation service
    salesforceGenService = appCtx.getBean("salesforceGenService")
    def objects = salesforceGenService.getObjectNames()
	println "------- Salesforce Object List -------"
	objects.each{
		println it
	}
	event('StatusFinal', [""])
}

