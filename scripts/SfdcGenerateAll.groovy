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

target(default:"Generates all Salesforce objects") {
    depends(checkVersion, configureProxy, packageApp, classpath, loadApp, configureApp, parseArguments)
    
    // Expected Parameters
    boolean genDomainClass = false

    // Read all other arguments
    if( argsMap.d ) {
        genDomainClass = true
    }
    
    // Plugin home dir
    def pluginHome = "${riptideSalesforcePluginDir}" + File.separator
    
    // Code generation service
    salesforceGenService = appCtx.getBean("salesforceGenService")
    salesforceGenService.generateAll( "${pluginHome}", "${basedir}", genDomainClass )
    event('StatusFinal', ["Done generating artefacts"])
}

