package com.riptide.grails.salesforce;

import com.sforce.soap.partner.DescribeSObjectResult;

import grails.util.Environment;

import com.sforce.soap.partner.DescribeGlobalResponse;
import com.sforce.soap.partner.DescribeGlobalResult;
import com.sforce.soap.partner.DescribeGlobalSObjectResult;
import com.sforce.soap.partner.DescribeSObjectResult;

import groovy.text.SimpleTemplateEngine;

import java.util.Map;

import org.codehaus.groovy.grails.commons.ConfigurationHolder;

/**
 * this service is to be only used internally by the plugin. It takes care of source code 
 * generation for the plugin.
 * @author Carlos.Munoz
 *
 */
public class SalesforceGenService extends SalesforceService {
    
    /* Default package to use if none is configured */
    private static final String DEFAULT_PKG = "com.salesforce.sobject";
	
	private static final int DESCRIBE_LIMIT = 100
    
    
    /**
     * Applies a template given a context and an output file location.
     */
    private void applyTemplate(Map binding, String templateLoc, String outputLoc) {

        // Generation Engine
        def engine = new SimpleTemplateEngine()
    
        // Generate a class file for the object
    
        // Get the template
        def templateFile = new File("${templateLoc}")
        def template = engine.createTemplate(templateFile).make(binding)
    
        // Build the file path
        log.info "Generating: " + outputLoc
        // print the file
        def classFile = new File(outputLoc)
        try {
            // create the new file if it does not exist
            if( !classFile.exists() ) {
                classFile.getParentFile().mkdirs()
                classFile.createNewFile()
            }
            classFile.withPrintWriter{ pwriter ->
                pwriter.println template.toString()
            }
        }
        catch( Exception ex ) {
            log.error "Error generating file: " + ex.getMessage()
        }

    }
    
    
    /**
     * Generates a single class from the Salesforce org given a Type Description
     */
    private void generateObject( DescribeSObjectResult typeDesc, String pluginBasedir, String appDir, boolean generateDomainClass ) {
        
        // Get the necessary configuration parameters
        String pkg = DEFAULT_PKG
        if( ConfigurationHolder.config.riptide."${Environment.current.name}".salesforce.codegen.pkg ) {
            pkg = ConfigurationHolder.config.riptide."${Environment.current.name}".salesforce.codegen.pkg
        }
        log.info "package used: " + pkg
        
        // Description was found
        if( typeDesc ) {
            String type = typeDesc.getName()
            
            // Create a new file based on the template and the binding below
            def context = [TYPE_NAME: type,
                           PACKAGE: pkg,
                           TYPE_DESC: typeDesc,
                           IS_DOMAIN_CLASS: generateDomainClass]

            // Get the class template
            def templateFileName = "${pluginBasedir}${File.separator}grails-app${File.separator}templates" +
                File.separator + "SforceObject.tmpl"

            def pkgToDir = pkg.replace('.' as char, File.separatorChar)

            // Remove the '__c' from the class' file name if needed
            String gClassName = type
            if( gClassName.endsWith("__c") ) {
                gClassName = gClassName.substring(0, gClassName.length()-3)
            }

            // Build the class file path
            String classFileName =
                appDir + File.separator + "src" + File.separator + "groovy" + File.separator +
                pkgToDir + File.separator + gClassName + ".groovy"
                
            // If it is a domain class, generate in the apps domain class dir
            if(generateDomainClass) {
                classFileName =
                    appDir + File.separator + "grails-app" + File.separator + "domain" + File.separator +
                    pkgToDir + File.separator + gClassName + ".groovy"
            }


            // print the file
            try {
                this.applyTemplate(context, templateFileName, classFileName)
            }
            catch( Exception ex ) {
                log.error "Error generating file: " + ex.getMessage()
            }
        }
        else {
            log.error "Object with name ${typeDesc.getName()} not found in Salesforce org."
        }
    }
    
    
    def generateAll(String pluginBasedir, String appDir, boolean generateDomainClass) {
        
		// Get a list of names of SObjects in the org.
		List<String> objectNames = this.getObjectNames()
		
		// Get the SOBject descriptions from Salesforce
		List<DescribeSObjectResult> typeDesc = []
		
		// We have to segment the list we're sending because the max # of
		// SObjects we can describe at one time is 100 so we'll send the object
		// names in groups of 100 until we've got them all.
		int rangeStart = 0
		int numObjects = objectNames.size()
		println "${numObjects} objects will be generated"
		while( rangeStart < numObjects ){
			def rangeEnd = rangeStart + DESCRIBE_LIMIT - 1
			
			// if the end of the range is >= to the num objects,
			// this could result in ArrayIndexOutOfBounds exceptions
			// we'll make sure that doesn't happen by making sure
			// we never go > numObjects - 1
			if(rangeEnd >= numObjects){
				rangeEnd = numObjects - 1
			}
			typeDesc.addAll(this.describeSObjects(objectNames[(rangeStart..rangeEnd)]))
			rangeStart = rangeEnd + 1
		}
		
		println "finished getting object descriptions"
		
		typeDesc.eachWithIndex{ desc, idx ->
			if(idx%20 == 0){
				println "${numObjects - idx} objects left to generate"
			}
			this.generateObject(desc, pluginBasedir, appDir, generateDomainClass)	
		}
		
    }
    
    
    def generateSingle( String sObjName, String pluginBasedir, String appDir, boolean generateDomainClass ) {
        
        // Get the object to generate's description
        DescribeSObjectResult typeDesc = this.describeSObject(sObjName);
		
//		typeDesc.getFields().each{ f ->
//			println f.getName() + " is " + f.getType()	
//		}
        
		
        // Generate
        this.generateObject(typeDesc, pluginBasedir, appDir, generateDomainClass);
    }
	
	def getObjectNames(){
		// Get a list of names of SObjects in the org.
		DescribeGlobalResult result = this.describeGlobal()
		List<DescribeGlobalSObjectResult> objectResults = result.getSobjects()
		List<String> objectNames = []
		objectResults.each{ res ->
			objectNames.add(res.getName())
		}
		return objectNames
	}

}
