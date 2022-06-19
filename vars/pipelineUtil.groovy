import java.util.List
import org.apache.commons.lang3.text.StrSubstitutor

/**
 * Script created for generic pipeline utility methods
 *@author achatterjee
 * */

/**
 * Method to read from property file using path and key
 *
 * */
//@NonCPS
String readFromPropertyWithPath(String filepath, String key) {
    Properties properties
    if (filepath == null || filepath.isEmpty())
        properties = readProperties file: pipeLineConstants.TEMPLATE_FILE_PATH
    else
        properties = readProperties file: pipeLineConstants.TEMPLATE_FILE_PATH
    return properties[key].toString()

}

/**
 * Method to read from YML using key
 *
 * */
def readFromYMLWithKey(yml, String key) {
	def result =[:]
	try{
	     if(yml==null||yml.toString().isEmpty()||key==null||key.isEmpty())
                result = "invalid input"
	     else
		result = yml.(key.toString()) 
               }
	catch (Exception e){
	   throwError(e.getMessage())	
	}	
	return result
}

/**
 * Method to read from property using key
 * the default template.properties property folder will be used
 *
 * */
String readFromPropertyWithKey(String key) {
    return readFromPropertyWithPath(null, key)

}


/**
 * This method identifies if the yml file is designed using new schema or old schema
 */
boolean isOldSchema(def caller)
{
    String gitUrl = caller.parameterMap.get(pipeLineConstants.GIT_REPO)
    String projectDir = caller.parameterMap.get(pipeLineConstants.PROJECT_DIR)
    projectDir=(projectDir!=null && !projectDir.toString().matches(pipeLineConstants.PROJECT_DIR_REGEX)||projectDir==null)?"":projectDir
    pipeLineConstants.BRANCH=caller.parameterMap.containsKey(pipeLineConstants.GIT_BRANCH)?caller.parameterMap.get(pipeLineConstants.GIT_BRANCH):pipeLineConstants.BRANCH
    hyperloopGit.getGitFileThroughCurl(hyperloopGit.getGitOrgName(gitUrl), hyperloopGit.getGitRepoName(gitUrl), pipeLineConstants.BRANCH, projectDir, pipeLineConstants.YML_FILE_NAME)
    def userData = readFile file: pipeLineConstants.YML_FILE_NAME
    String formated_build_param = StrSubstitutor.replace(userData,caller.parameterMap,"[","]")
    userDataYML = readYaml text:  formated_build_param
    ymlOldSchema=formated_build_param.contains("Common:")?false:true
    return ymlOldSchema
}
/**
 * This method will check whether yml file exists in git repo.
 * @param parameterMap
 * @return
 */
public boolean doesFileExists(def parameterMap)
{
    try {
        String gitUrl = parameterMap.get(pipeLineConstants.GIT_REPO)
        String projectDir = parameterMap.get(pipeLineConstants.PROJECT_DIR)
        projectDir=(projectDir!=null && !projectDir.toString().matches(pipeLineConstants.PROJECT_DIR_REGEX)||projectDir==null)?"":projectDir
        pipeLineConstants.BRANCH=parameterMap.containsKey(pipeLineConstants.GIT_BRANCH)?parameterMap.get(pipeLineConstants.GIT_BRANCH):pipeLineConstants.BRANCH
        hyperloopGit.getGitFileThroughCurl(hyperloopGit.getGitOrgName(gitUrl), hyperloopGit.getGitRepoName(gitUrl), pipeLineConstants.BRANCH, projectDir, pipeLineConstants.YML_FILE_NAME)
        def fileData = readFile file: pipeLineConstants.YML_FILE_NAME
        fileExists=fileData.toString().contains(pipeLineConstants.FILE_NOT_FOUND.toString().trim())?false:true
        ymlOldSchema=fileData.toString().contains("Common:")?false:true
        return fileExists
    } catch(e) {
        println "Error occured while checking the file"+e

    }
}
/**
 * This method identifies if the yml file is designed using new schema or old schema
 * @return
 */
boolean isOldSchema(def parameterMap,def caller)
{
    println "Inside is Old Schema"
    println parameterMap.get(pipeLineConstants.PROJECT_DIR)
    def userData = readFile file: pipeLineConstants.YML_FILE_NAME
    String formated_build_param = StrSubstitutor.replace(userData,parameterMap,"[","]")
    caller.userDataYML = readYaml text:  formated_build_param
    println "User Data YML Is:"
    println caller.userDataYML
    ymlOldSchema=formated_build_param.contains("Common:")?false:true
    return ymlOldSchema
}
/**
 * This method will decide which stage should be included as a part of this execution
 */
public void updateStageInclusion(def test,def caller)
{
    switch(test.toString().toLowerCase())
    {
        case pipeLineConstants.ALL:
            caller.includeHealthCheckExecution=true
            caller.includeSmokeExecution=true
            caller.includeServiceRegressionExecution=true
            caller.includeE2EExecution=true
            caller.includePerformanceTests=true
            caller.includeCustomTests=true
            break
        case pipeLineConstants.HEALTH_CHECK:
            caller.includeHealthCheckExecution=true
            break
        case pipeLineConstants.SMOKE:
            caller.includeHealthCheckExecution=true
            caller.includeSmokeExecution=true
            break
        case pipeLineConstants.E2E_SMOKE:
            caller.includeHealthCheckExecution=true
            caller.includeSmokeExecution=true
            caller.includeE2EExecution=true
            break
        case pipeLineConstants.REGRESSION:
            caller.includeHealthCheckExecution=true
            caller.includeSmokeExecution=true
            caller.includeServiceRegressionExecution=true
            break
        case pipeLineConstants.PERF_TEST:
            caller.includeHealthCheckExecution=true
            caller.includeSmokeExecution=true
            caller.includePerformanceTests=true
            break
        case pipeLineConstants.CUSTOM_TEST:
            caller.includeHealthCheckExecution=true
            caller.includeSmokeExecution=true
            caller.includeCustomTests=true
            break
        case pipeLineConstants.NONE:
            caller.includeNone=true
            sh "curl -X POST ${callback} -d pipeLineConstants.SUCCESS"
            break
    }
}
/**
 * This method is used for validating the mandatory fields.
 */
@NonCPS
boolean validateMandatoryFields(properties)
{
    def propertyNames= properties.propertyNames()
    if(InputParameters==null || InputParameters.toString().trim().isEmpty())
    {
        throwError("InputParameter"+pipeLineConstants.ERROR_MESSAGE)
    }
    while (propertyNames.hasMoreElements()) {
        String key = propertyNames.nextElement()
        def err=key+pipeLineConstants.ERROR_MESSAGE
        if(!InputParameters.toString().contains(key))
        {
            throwError(err)
        } else {
            String propertyName = InputParameters.substring(InputParameters.indexOf(key)+key.toString().length()+1)
            if(propertyName.contains("\n")) {
                propertyName = propertyName.substring(0,propertyName.indexOf("\n"))
            }else
            {
                propertyName = propertyName.substring(0)
            }
            if(propertyName==null || propertyName.toString().trim().isEmpty()|| propertyName.trim().contentEquals(";")) {
                throwError(err)
            }
        }
    }
}
/**
 * This method will throw an error to stop the execution of the pipeline in case of vaildation failure.
 */
public void throwError(errorMessage)
{
    ansiColor('xterm') {
        echo "\033[1;91m"+errorMessage+"\033[1;91m"
    }
    error "VALIDATION ${pipeLineConstants.FAILURE}"
}
/**
 * This method will add badges, create summary and split job names
 */
void init(def caller)
{
    caller.notification_type_List=caller.notification_type.split(",")
    def branch= pipeLineConstants.AF_GIT_BRANCH
    def gitUrl =  pipeLineConstants.AF_GIT_URL
    hyperloopGit.getGitFileThroughCurl(hyperloopGit.getGitOrgName(gitUrl), hyperloopGit.getGitRepoName(gitUrl), branch, pipeLineConstants.TEMPLATE_DIR, pipeLineConstants.REPORT_HTML)
    caller.reportPath=caller.env.WORKSPACE+"/html_report.html"
    caller.reportTemplate = readFile file: caller.reportPath
    def workspace =  env.WORKSPACE
    echo "The value of workspace is :"
    echo workspace
    //If user name not coming as inputparameter from remote caller then it extracts the user name
    if(caller.currentBuild.rawBuild.getCause(Cause.UserIdCause)!=null){
        caller.userName= caller.currentBuild.rawBuild.getCause(Cause.UserIdCause).getUserId()
        caller.dataMap.put(pipeLineConstants.USER_NAME, caller.userName)
        manager.addBadge(pipeLineConstants.USER_GIF,caller.userName)
    }else{
        manager.addBadge(pipeLineConstants.USER_GIF,caller.userName)
    }
	shortText = caller.componentName +"_"+ caller.envs
    manager.addShortText(shortText)
    def summary = manager.createSummary(pipeLineConstants.GEAR_ICON)
    summary.appendText("<h1>Job execution summary</h1></br>", false)
    caller.environmentArray= caller.envs.split(",")
    //initialize jenkins folder with all jobs
    searchUtility.initializeCTJobsFolder(caller)
}

/**
 * This method parses the inpiut paramers and returns as a List of parameters.
 * @param paramValueList
 * @param jobName
 * @return
 */
List<hudson.model.ParameterValue> parseBuildInputParameters(paramValueList,jobName,parameterMap)
{
    if(jobName.contains("@")) {
        String build_param = jobName.substring(jobName.indexOf("@")+1)
        jobName=jobName.substring(0,jobName.indexOf("@"))
        String formated_build_param = StrSubstitutor.replace(build_param,parameterMap,"{","}")
        String[] buildParamArray=formated_build_param.split(",")
        for(param in buildParamArray)
        {
            String[] paramArray = param.split("=")
            String value = (paramArray.length==1)?"":paramArray[1]
            paramValueList.add(new StringParameterValue(paramArray[0],value))
        }
    }
    return paramValueList
}

/**
 * This method will parse all input parameters
 * @author achatterjee
 * Moved to this Util and method update By ashrivastava1
 */
def parseInputParams(def parent,params) {
    loadProperty(parent,params)
    parent.dataMap.put(pipeLineConstants.EMAIL_PREFERENCE, parent.emailPreference)
    parent.dataMap.put(pipeLineConstants.SLACK_PREFERENCE, parent.slackPreference)
    parent.componentName = parent.parameterMap.containsKey(pipeLineConstants.COMPONENT_NAME) ? parent.parameterMap.get(pipeLineConstants.COMPONENT_NAME) : parent.componentName
    parent.dataMap.put(pipeLineConstants.COMPONENT_NAME, parent.componentName)
    if (parent.parameterMap.containsKey(pipeLineConstants.SERVICE_SMOKE)) {
        overrideJob(parent.parameterMap.get(pipeLineConstants.SERVICE_SMOKE), parent.smokeMap)
        def smokeData = (parent.parameterMap.get(pipeLineConstants.SERVICE_SMOKE) == null ? "" : parent.parameterMap.get(pipeLineConstants.SERVICE_SMOKE))
        ignoreSmoke = (smokeData.toString().trim().equalsIgnoreCase(pipeLineConstants.IGNORE) || smokeData.toString().trim().equalsIgnoreCase(pipeLineConstants.SKIP)) ? true : false
    }
    if (parent.parameterMap.containsKey(pipeLineConstants.CLIENT_TEST_KEY)) {
        println "trying to read client test params"
        overrideJob(parent.parameterMap.get(pipeLineConstants.CLIENT_TEST_KEY), parent.customMap)
    }
    if (parent.parameterMap.containsKey(pipeLineConstants.SERVICE_REGRESSION)) {
        overrideJob(parent.parameterMap.get(pipeLineConstants.SERVICE_REGRESSION), parent.regressionMap)
    }
    if (parent.parameterMap.containsKey(pipeLineConstants.PERFORMANCE_TEST)) {
        overrideJob(parent.parameterMap.get(pipeLineConstants.PERFORMANCE_TEST), parent.perfMap)
    }
    if (parent.parameterMap.containsKey(pipeLineConstants.CUSTOM_TEST_KEY)) {
        println "trying to read custom params"
        overrideJob(parent.parameterMap.get(pipeLineConstants.CUSTOM_TEST_KEY), parent.customMap)
    }
    parent.envs = parent.parameterMap.containsKey(pipeLineConstants.ENVIRONMENT) ? parent.parameterMap.get(pipeLineConstants.ENVIRONMENT) : parent.envs
    parent.dataMap.put(pipeLineConstants.LINE_ENV, parent.envs)
    parent.releaseId = parent.parameterMap.containsKey(pipeLineConstants.VERSION) ? parent.parameterMap.get(pipeLineConstants.VERSION) : parent.releaseId
    parent.dataMap.put(pipeLineConstants.RELEASE, parent.releaseId)
    parent.notify_after = parent.parameterMap.containsKey(pipeLineConstants.pipeLineConstants.NOTIFY_AFTER) ? parent.parameterMap.get(pipeLineConstants.pipeLineConstants.NOTIFY_AFTER) : parent.notify_after
    parent.notification_type = parent.parameterMap.containsKey(pipeLineConstants.NOTIFICATION_TYPE) ? parent.parameterMap.get(pipeLineConstants.NOTIFICATION_TYPE) : parent.notification_type
    parent.slack_channel = parent.parameterMap.containsKey(pipeLineConstants.SLACK_CHANNEL) ? parent.parameterMap.get(pipeLineConstants.SLACK_CHANNEL) : parent.slack_channel
    parent.dataMap.put(pipeLineConstants.SLACK, parent.slack_channel)
    parent.userName = parent.parameterMap.containsKey(pipeLineConstants.USER_NAME) ? parent.parameterMap.get(pipeLineConstants.USER_NAME) : parent.userName
    parent.email = parent.parameterMap.containsKey(pipeLineConstants.EMAIL) ? parent.parameterMap.get(pipeLineConstants.EMAIL) : parent.email
    parent.dataMap.put(pipeLineConstants.EMAIL, parent.email)
    parent.release_version = parent.parameterMap.containsKey(pipeLineConstants.VERSION) ? parent.parameterMap.get(pipeLineConstants.VERSION) : parent.release_version
    parent.testType = parent.parameterMap.containsKey(pipeLineConstants.TEST) ? parent.parameterMap.get(pipeLineConstants.TEST) : parent.testType
    parent.gitRepo = parent.parameterMap.containsKey(pipeLineConstants.GIT_REPO) ? parent.parameterMap.get(pipeLineConstants.GIT_REPO) : parent.gitRepo
    parent.projectDir = parent.parameterMap.containsKey(pipeLineConstants.PROJECT_DIR) ? parent.parameterMap.get(pipeLineConstants.PROJECT_DIR) : parent.projectDir
    parent.healthCheck = parent.parameterMap.containsKey(pipeLineConstants.HEALTH_CHECK) ? parent.parameterMap.get(pipeLineConstants.HEALTH_CHECK) : parent.healthCheck
    parent.runRegressionInParallel = parent.parameterMap.containsKey(pipeLineConstants.RUN_REGRESSION_IN_PARALLEL) ? parent.parameterMap.get(pipeLineConstants.RUN_REGRESSION_IN_PARALLEL) : parent.runRegressionInParallel
    println "The value of Run Regression In Parallel Is:"
    println parent.runRegressionInParallel


}

/**
 * This method loads all the properties(Default) from the property file
 * @author achatterjee
 * Moved to Util and method Update By ashrivastava1
 */
void loadProperty(def parent,params) {

    for (param in params) {
        String[] paramArray = param.split(":", 2)
        String key = paramArray[0]
        if (paramArray.length == 2) {
            String value = paramArray[1]
            println value
            parent.parameterMap.put(key.toString().trim(), value.toString().trim());
        } else if (paramArray.length == 1) {
            parent.parameterMap.put(key.toString().trim(), "");
        }
    }
    String env = parent.parameterMap.get(pipeLineConstants.ENVIRONMENT)
    line = env.substring(0, env.indexOf("-"))
    line = line.toUpperCase()
    String compName = parent.parameterMap.get(pipeLineConstants.COMPONENT_NAME)
    def isYMLExists=doesFileExists(parent.parameterMap)
    def isSchemaOld=false
    def doesLineExistsInYML=false
    if(!isYMLExists)
    {
        println "No YML present in service repo Hence redirecting to CARE default YML"
        loadCustomJobProperties(parent)
        parent.userDataYML=parent.customYMLTestConfig
        println "CARE default yml data is:"
        println parent.userDataYML

    }
    else {
        isSchemaOld = isOldSchema(parent.parameterMap, parent)
        doesLineExistsInYML = (parent.userDataYML.(line.toString())==null ||parent.userDataYML.(line.toString()).toString().isEmpty())?false:true
    }
    if (isSchemaOld ) {
        populateServiceJobMap(line, parent)
        parent.notification_type = parent.userDataYML.(line + pipeLineConstants.PROPERTY_NOTIFICATION_TYPE)
        parent.slack_channel = parent.userDataYML.(line + pipeLineConstants.PROPERTY_SLACK_CHANNEL)
        parent.email = parent.userDataYML.(line + pipeLineConstants.PROPERTY_EMAIL)
        parent.testType = parent.userDataYML.(line + pipeLineConstants.PROPERTY_TEST)
        parent.healthCheck = parent.userDataYML.(line + pipeLineConstants.PROPERTY_HEALTHCHECK)
        //If no data for below property is set in the line property section, then it will be picked from common properties section
        parent.notification_type = (parent.notification_type == null || parent.notification_type.toString().trim().isEmpty()) ? parent.userDataYML.(pipeLineConstants.COMMON + pipeLineConstants.PROPERTY_NOTIFICATION_TYPE) : parent.notification_type
        parent.slack_channel = (parent.slack_channel == null || parent.slack_channel.toString().trim().isEmpty()) ? parent.userDataYML.(pipeLineConstants.COMMON + pipeLineConstants.PROPERTY_SLACK_CHANNEL) : parent.slack_channel
        parent.email = (parent.email == null || parent.email.toString().trim().isEmpty()) ? parent.userDataYML.(pipeLineConstants.COMMON + pipeLineConstants.PROPERTY_EMAIL) : parent.email
        parent.testType = (parent.testType == null || parent.testType.toString().trim().isEmpty()) ? parent.userDataYML.(pipeLineConstants.COMMON + pipeLineConstants.PROPERTY_TEST) : parent.testType
        parent.healthCheck = (parent.healthCheck == null || parent.healthCheck.toString().trim().isEmpty()) ? parent.userDataYML.(pipeLineConstants.COMMON + pipeLineConstants.PROPERTY_HEALTHCHECK) : parent.healthCheck
        parent.healthCheck = (parent.healthCheck == null) ? "" : parent.healthCheck
        loadCAREDefaultYML(parent)
        loadCAREDefaultJobMap(line, pipeLineConstants.E2E_TEST_PROPERTY, parent.e2eMap,parent,pipeLineConstants.CARE_DEFAULT_YML_NAME,pipeLineConstants.E2E_PROPERTY_KEY)
        processStageDataMap(line,pipeLineConstants.E2E_TEST_PROPERTY,parent.e2eMap,parent)
        echo "loaded the properties successfully using old Schema"
    } else {
        println "Inside load property new Schema:"
        /**Adding the code update if no jobs are present in service repo yml*/
        if ((parent.clientMap.isEmpty()&&line.equalsIgnoreCase(pipeLineConstants.CUSTOM_JOB_ENV) && isYMLExists && doesLineExistsInYML)) {
            loadCustomJobProperties(parent)
        }
        else if(!doesLineExistsInYML){
            loadCustomJobProperties(parent)
            parent.userDataYML=parent.customYMLTestConfig
        }
        loadCAREDefaultYML(parent)
        loadCAREDefaultJobMap(line, pipeLineConstants.E2E_TEST_PROPERTY, parent.e2eMap,parent,pipeLineConstants.CARE_DEFAULT_YML_NAME,pipeLineConstants.E2E_PROPERTY_KEY)
        loadJobMaps(line, pipeLineConstants.SERVICE_SMOKE_PROPERTY, parent.smokeMap,parent)
        loadJobMaps(line, pipeLineConstants.SERVICE_REGRESSION_PROPERTY, parent.regressionMap,parent)
        loadJobMaps(line, pipeLineConstants.PERF_TEST_PROPERTY, parent.perfMap,parent)
        loadJobMaps(line, pipeLineConstants.CUSTOM_TEST_PROPERTY, parent.customMap,parent)
        loadJobMaps(line, pipeLineConstants.CLIENT_TEST_PROPERTY, parent.clientMap,parent)

        if (parent.e2eMap.isEmpty()&&!parent.skipE2e) {
            println "No data found in line section for E2E checking common section now"
            loadCAREDefaultJobMap(pipeLineConstants.COMMON, pipeLineConstants.E2E_TEST_PROPERTY, parent.e2eMap,parent,pipeLineConstants.CARE_DEFAULT_YML_NAME,pipeLineConstants.E2E_PROPERTY_KEY)
        }
        //additional check if the customer says skip in their YML and we say append
	checkAndUpdateStageData(line,pipeLineConstants.E2E_TEST_PROPERTY, parent.e2eMap,parent)
	if (!parent.e2eMap.isEmpty()&&parent.skipE2e) {
	    parent.skipE2e=false
	   }    
	
        if (parent.clientMap.isEmpty()) {
            println "trying to load client jobs"
            loadJobMaps(pipeLineConstants.COMMON, pipeLineConstants.CLIENT_TEST_PROPERTY, parent.customMap,parent)
        }
        if (parent.smokeMap.isEmpty() && !parent.ignoreSmoke) {
            println "SmokeMap is Empty"
            loadJobMaps(pipeLineConstants.COMMON, pipeLineConstants.SERVICE_SMOKE_PROPERTY, parent.smokeMap,parent)
        }
        if (parent.regressionMap.isEmpty() && !parent.skipRegression) {
            loadJobMaps(pipeLineConstants.COMMON, pipeLineConstants.SERVICE_REGRESSION_PROPERTY, parent.regressionMap,parent)
        }
        if (parent.perfMap.isEmpty() && !parent.skipPerformance) {
            loadJobMaps(pipeLineConstants.COMMON, pipeLineConstants.PERF_TEST_PROPERTY, parent.perfMap,parent)
        }
        if (parent.customMap.isEmpty() && !parent.skipCustom) {
            println "trying to load custom jobs"
            loadJobMaps(pipeLineConstants.COMMON, pipeLineConstants.CUSTOM_TEST_PROPERTY, parent.customMap,parent)
        }
        loadNotificationProperty(pipeLineConstants.EMAIL_PROPERTY, pipeLineConstants.EMAIL_GROUP,parent)
        loadNotificationProperty(pipeLineConstants.SLACK_PROPERTY, pipeLineConstants.SLACK_CHANNEL_PROPERTY,parent)
        if (line.equalsIgnoreCase(pipeLineConstants.CUSTOM_JOB_ENV)) {
            overrideNotificationProperty(pipeLineConstants.EMAIL_PROPERTY, pipeLineConstants.EMAIL_GROUP,parent)
            overrideNotificationProperty(pipeLineConstants.SLACK_PROPERTY, pipeLineConstants.SLACK_CHANNEL_PROPERTY,parent)

        }
        parent.notification_type = ((parent.email.toString().trim().isEmpty()) ? "" : "email,") + ((parent.slack_channel.toString().trim().isEmpty()) ? "" : "slack")
        parent.testType = parent.userDataYML.(line.toString()).(pipeLineConstants.TEST_PROPERTY.toString())
        parent.testType = (parent.testType == null || parent.testType.toString().trim().isEmpty()) ? parent.userDataYML.(pipeLineConstants.COMMON.toString()).(pipeLineConstants.TEST_PROPERTY.toString()) : parent.testType
        parent.testType = (parent.testType == null) ? "" : parent.testType
        parent.healthCheck = parent.userDataYML.(line.toString()).(pipeLineConstants.HEALTHCHECK_PROPERTY.toString())
        parent.healthCheck = (parent.healthCheck == null || parent.healthCheck.toString().trim().isEmpty()) ? parent.userDataYML.(pipeLineConstants.COMMON.toString()).(pipeLineConstants.HEALTHCHECK_PROPERTY.toString()) : parent.healthCheck
        parent.healthCheck = (parent.healthCheck == null) ? "" : parent.healthCheck
        parent.runRegressionInParallel = parent.userDataYML.(line.toString()).(pipeLineConstants.RUN_REGRESSION_IN_PARALLEL.toString())
        parent.runRegressionInParallel = (parent.runRegressionInParallel == null || parent.runRegressionInParallel.toString().trim().isEmpty()) ? parent.userDataYML.(pipeLineConstants.COMMON.toString()).(pipeLineConstants.RUN_REGRESSION_IN_PARALLEL.toString()) : parent.runRegressionInParallel
        println "The value of runRegression in Parallel is:"
        println parent.runRegressionInParallel
        echo "loaded the properties successfully using new Schema"
    }
}
/**
 * This method will load the custom properties from the custom yml.
 * @param parent
 */
public void loadCustomJobProperties(parent)
{
    def gitUrl = pipeLineConstants.AF_GIT_URL
    def config_branch = pipeLineConstants.CONFIG_BRANCH
    hyperloopGit.getGitFileThroughCurl(hyperloopGit.getGitOrgName(gitUrl), hyperloopGit.getGitRepoName(gitUrl), config_branch, pipeLineConstants.PROPERTY_FOLDER, pipeLineConstants.YML_FILE_NAME)
    parent.customTestJobsFilePath = parent.env.WORKSPACE + "/"+pipeLineConstants.YML_FILE_NAME
    parent.customYMLTestConfig = readYaml file: parent.customTestJobsFilePath
    loadJobMapForced(line, pipeLineConstants.CLIENT_TEST_PROPERTY, parent.clientMap, parent)
    println "Value of client map is"
    println parent.clientMap
}
/**
 * This method will populate the maps with all service smoke and service regression details.
 */
public void populateServiceJobMap(String line, def parent) {
    def smokeJobData = parent.userDataYML.(line + pipeLineConstants.PROPERTY_SERVICE_SMOKE)
    smokeJobData = (smokeJobData == null ? "" : smokeJobData)
    parent.ignoreSmoke = (smokeJobData.toString().trim().equalsIgnoreCase(pipeLineConstants.IGNORE) || smokeJobData.toString().trim().equalsIgnoreCase(pipeLineConstants.SKIP)) ? true : false
    populateMap(smokeJobData, parent.smokeMap)
    def regressionJobData = parent.userDataYML.(line + pipeLineConstants.PROPERTY_SERVICE_REGRESSION)
    populateMap(regressionJobData, parent.regressionMap)
}

public void populateMap(jobData, jobMap) {
    if (jobData != null && !(jobData.toString().trim().equalsIgnoreCase(pipeLineConstants.IGNORE)) && !(jobData.toString().trim().equalsIgnoreCase(pipeLineConstants.SKIP))) {
        jobData.each {
            def Map<String, String> paramMap = new HashMap<String, String>();
            def jobName = it.key
            def value = it.value
            if (!ymlOldSchema) {
                value = value.JobParameter
            }
            if (value != "none") {
                value.each {
                    paramMap.put(it.key, it.value)
                }
            }
            jobMap.put(jobName, paramMap)
        }
    }

    echo "Completed job data population in map"
    println jobMap
}

private void loadJobMaps(indent, testType, testMap,def parent) {
    def jobData = parent.userDataYML.(indent.toString()).(testType.toString().trim())
    jobData = (jobData == null ? "" : jobData)
    if (testType.toString().trim().equalsIgnoreCase(pipeLineConstants.SERVICE_SMOKE_PROPERTY)) {
        parent.ignoreSmoke = (jobData.toString().trim().equalsIgnoreCase(pipeLineConstants.IGNORE) || jobData.toString().trim().equalsIgnoreCase(pipeLineConstants.SKIP)) ? true : false
    } else if (testType.toString().trim().equalsIgnoreCase(pipeLineConstants.SERVICE_REGRESSION_PROPERTY)) {
        parent.skipRegression = (jobData.toString().trim().equalsIgnoreCase(pipeLineConstants.IGNORE) || jobData.toString().trim().equalsIgnoreCase(pipeLineConstants.SKIP)) ? true : false
    }
    else if (testType.toString().trim().equalsIgnoreCase(pipeLineConstants.PERF_TEST_PROPERTY)) {
        parent.skipPerformance = (jobData.toString().trim().equalsIgnoreCase(pipeLineConstants.IGNORE) || jobData.toString().trim().equalsIgnoreCase(pipeLineConstants.SKIP)) ? true : false
    }
    else if (testType.toString().trim().equalsIgnoreCase(pipeLineConstants.CUSTOM_TEST_PROPERTY)) {
        parent.skipCustom = (jobData.toString().trim().equalsIgnoreCase(pipeLineConstants.IGNORE) || jobData.toString().trim().equalsIgnoreCase(pipeLineConstants.SKIP)) ? true : false
    }
    else if (testType.toString().trim().equalsIgnoreCase(pipeLineConstants.E2E_TEST_PROPERTY)) {
        parent.skipE2e = isJobDataSkipOrIgnore(jobData)
    }
        populateMap(jobData, testMap)
}

private void loadNotificationProperty(notification_Type,NotificationGroup,def parent)
{
    //Checking in line property
    def notificationType=parent.userDataYML.(line.toString()).(pipeLineConstants.NOTIFICATION_TYPE_PROPERTY.toString())

    if(notificationType.toString().trim().equalsIgnoreCase(pipeLineConstants.SKIP))
    {
        return
    }
    def notification=(notificationType!=null)?notificationType.(notification_Type.toString()):""
    if(notification.toString().trim().toLowerCase().equals(pipeLineConstants.SKIP))
    {
        return
    }
    notification=(notification==null)?"":notification

    if(NotificationGroup.toString().trim().equals(pipeLineConstants.EMAIL_GROUP) && !notification.toString().isEmpty())
    {
        parent.email=(!notification.toString().trim().isEmpty())?notification.(NotificationGroup.toString()):""
        parent.emailPreference=(notification.EmailOn==null?"":notification.EmailOn)
    }else if(NotificationGroup.toString().trim().equals(pipeLineConstants.SLACK_CHANNEL_PROPERTY) && !notification.toString().isEmpty()){
        parent.slack_channel=(!notification.toString().trim().isEmpty())?notification.(NotificationGroup.toString()):""
        parent.slackPreference=(notification.SlackOn==null?"":notification.SlackOn)
    }
    //Checking in Common property if no line property is specified
    if(notificationType==null || notification.toString().isEmpty())
    {
        notificationType=parent.userDataYML.(pipeLineConstants.COMMON.toString()).(pipeLineConstants.NOTIFICATION_TYPE_PROPERTY.toString());
        if(notificationType.toString().trim().equalsIgnoreCase(pipeLineConstants.SKIP))
        {
            return
        }
        notification=(notificationType!=null)?notificationType.(notification_Type.toString()):""
        notification=(notification==null||notification.toString().trim().equalsIgnoreCase(pipeLineConstants.SKIP))?"":notification
        if(NotificationGroup.toString().trim().equals(pipeLineConstants.EMAIL_GROUP) && !notification.toString().isEmpty())
        {
            parent.email=(!notification.toString().trim().isEmpty())?notification.(NotificationGroup.toString()):""
            parent.emailPreference=(notification.EmailOn==null?"":notification.EmailOn)
        }
        else if(NotificationGroup.toString().trim().equals(pipeLineConstants.SLACK_CHANNEL_PROPERTY) && !notification.toString().isEmpty()){
            println "Inside Common Slack Block"
            parent.slack_channel=notification.(NotificationGroup.toString())
            parent.slack_channel=(parent.slack_channel==null?"":parent.slack_channel)
            parent.slackPreference=(notification.SlackOn==null?"":notification.SlackOn)
        }
    }
}

private void overrideJob(jobMetaData,jobMap)
{
    jobMap.clear()
    if(jobMetaData==null || jobMetaData.toString().trim().isEmpty()||jobMetaData.toString().trim().equalsIgnoreCase(pipeLineConstants.IGNORE)||jobMetaData.toString().trim().equalsIgnoreCase(pipeLineConstants.SKIP))
    {
        return
    }
    def jobData = readYaml text:  jobMetaData
    populateMap(jobData, jobMap)
}
/**
 * This method parses the input parameters and returns as a List of parameters.
 * @param paramValueList
 * @param jobName
 * @return
 */
List<hudson.model.ParameterValue> parseBuildInputParam(paramMap)
{
    List<hudson.model.ParameterValue> paramValueList = new ArrayList<hudson.model.ParameterValue>()
    paramMap.each{
        paramValueList.add(new StringParameterValue(it.key.toString(),it.value.toString()))
    }
    return paramValueList
}
/**
 * This method forcefully overrides the customMap
 * this is a workaround until service teams include it as a part of YML
 */
def loadJobMapForced(indent, testType, testMap, def parent) {
    def jobData = parent.customYMLTestConfig.(indent.toString()).(testType.toString().trim())
    println "printing jobData"
    println jobData
    jobData = (jobData == null ? "" : jobData)
    populateMap(jobData, testMap)
}

/**
 * This method updates build result and report data
 * this is a workaround until service teams include it as a part of YML
 */
def updateBuildResultAndReportData(def caller, def build , def jobName ) {
        reportUtil.updateBuildResult(caller,build,jobName)
		reportDataAllRow=reportUtil.updateReportData(caller,build,jobName)
}

/**
 * This method will load the custom notificationKeyValuePair destination both slack and email
 * these notificationKeyValuePair are defined in custom YML provided and maintained by CARE team
 */
private void overrideNotificationProperty(notificationTypeBlockGetter,NotificationDestinationKey,def parent)
{
    //Checking in line property
    def notificationType=parent.customYMLTestConfig.(line.toString()).(pipeLineConstants.NOTIFICATION_TYPE_PROPERTY.toString())
    if(notificationType.toString().trim().equalsIgnoreCase(pipeLineConstants.SKIP))
    {
        println "Notification block is either skipped or null"
        return
    }
    def notificationKeyValuePair=(notificationType!=null)?notificationType.(notificationTypeBlockGetter.toString()):""
    notificationKeyValuePair=(notificationKeyValuePair==null)?"":notificationKeyValuePair

    if(notificationKeyValuePair.toString().trim().toLowerCase().equals(pipeLineConstants.SKIP))
    {
        return
    }
    if(NotificationDestinationKey.equals(pipeLineConstants.EMAIL_GROUP) && !notificationKeyValuePair.toString().isEmpty())
    {
        if(parent.email.isEmpty()){
            parent.email=(!notificationKeyValuePair.toString().trim().isEmpty())?notificationKeyValuePair.(NotificationDestinationKey.toString()):""
            parent.emailPreference=(notificationKeyValuePair.EmailOn==null?"":notificationKeyValuePair.EmailOn)
        }
        else{
            parent.email+=(!notificationKeyValuePair.toString().trim().isEmpty())?pipeLineConstants.SEPARATOR+notificationKeyValuePair.(NotificationDestinationKey.toString()):""

        }
    }else if(NotificationDestinationKey.toString().equals(pipeLineConstants.SLACK_CHANNEL_PROPERTY) && !notificationKeyValuePair.toString().isEmpty()){
        if(parent.slack_channel.isEmpty()){
            parent.slack_channel=(!notificationKeyValuePair.toString().trim().isEmpty())?notificationKeyValuePair.(NotificationDestinationKey.toString()):""
            parent.slackPreference=(notificationKeyValuePair.SlackOn==null?"":notificationKeyValuePair.SlackOn)
        }
        else{
            parent.slack_channel+=(!notificationKeyValuePair.toString().trim().isEmpty())?pipeLineConstants.SEPARATOR+notificationKeyValuePair.(NotificationDestinationKey.toString()):""

        }
    }
    //Checking in Common property if no line property is specified
    if(notificationType==null || notificationKeyValuePair.toString().isEmpty())
    {
        notificationType=parent.customYMLTestConfig.(pipeLineConstants.COMMON.toString()).(pipeLineConstants.NOTIFICATION_TYPE_PROPERTY.toString());
        if(notificationType.toString().trim().equalsIgnoreCase(pipeLineConstants.SKIP))
        {
            return
        }
        notificationKeyValuePair=(notificationType!=null)?notificationType.(notificationTypeBlockGetter.toString()):""
        notificationKeyValuePair=(notificationKeyValuePair==null||notificationKeyValuePair.toString().trim().equalsIgnoreCase(pipeLineConstants.SKIP))?"":notificationKeyValuePair
        if(NotificationDestinationKey.toString().equals(pipeLineConstants.EMAIL_GROUP) && !notificationKeyValuePair.toString().isEmpty())
        {
            if(parent.email.isEmpty()||parent.email.equals(notificationKeyValuePair.(NotificationDestinationKey.toString()))){
                parent.email=(!notificationKeyValuePair.toString().trim().isEmpty())?notificationKeyValuePair.(NotificationDestinationKey.toString()):""
            }
            else{
                parent.email+=(!notificationKeyValuePair.toString().trim().isEmpty())?pipeLineConstants.SEPARATOR+notificationKeyValuePair.(NotificationDestinationKey.toString()):""

            }
            parent.emailPreference=(notificationKeyValuePair.EmailOn==null?"":notificationKeyValuePair.EmailOn)
        }
        else if(NotificationDestinationKey.toString().equals(pipeLineConstants.SLACK_CHANNEL_PROPERTY) && !notificationKeyValuePair.toString().isEmpty()){
            if(parent.slack_channel.isEmpty()||parent.slack_channel.equals(notificationKeyValuePair.(NotificationDestinationKey.toString()))){
                parent.slack_channel=(!notificationKeyValuePair.toString().trim().isEmpty())?notificationKeyValuePair.(NotificationDestinationKey.toString()):""
            }
            else{
                parent.slack_channel+=(!notificationKeyValuePair.toString().trim().isEmpty())?pipeLineConstants.SEPARATOR+notificationKeyValuePair.(NotificationDestinationKey.toString()):""

            }
            parent.slackPreference=(notificationKeyValuePair.SlackOn==null?"":notificationKeyValuePair.SlackOn)
        }
    }
}

/**
 * This method will load the custom properties from the custom yml.
 * @param parent
 */
  void loadCAREDefaultYML(parent){
    println "Loading the default YML"
    hyperloopGit.getGitFileThroughCurl(hyperloopGit.getGitOrgName(pipeLineConstants.AF_GIT_URL), hyperloopGit.getGitRepoName(pipeLineConstants.AF_GIT_URL), pipeLineConstants.CONFIG_BRANCH, pipeLineConstants.PROPERTY_FOLDER, pipeLineConstants.YML_FILE_NAME)
    parent.customTestJobsFilePath = parent.env.WORKSPACE + "/"+pipeLineConstants.YML_FILE_NAME
    def careData= readFile file: parent.customTestJobsFilePath
    String formated_build_param = StrSubstitutor.replace(careData,parent.parameterMap,"[","]")
    parent.customYMLTestConfig =  readYaml text:  formated_build_param	  

}

/**
 * This method will load the custom properties from the custom yml.
 * @param parent
 */
private void loadCAREDefaultJobMap(indent, testType, testMap,parent,ymlname,propertykey) {
    println "loading the default map for : " + testType
    def jobData =readFromYMLWithKey(readFromYMLWithKey(parent.(ymlname.toString()),indent),testType)
    jobData = (jobData == null ? "" : jobData)
    parent.(propertykey.toString()) = isJobDataSkipOrIgnore(jobData)
    updateOverrideField(jobData,parent)	
    populateMap(jobData, testMap)
}

/**
 * This method will load modify dataflag property from yml section
 * and update in deployment validator
 * @param jobData,parent
 */
private void updateOverrideField(jobData,parent){
    parent.modifyData = isJobDataSkipOrIgnore(jobData)?jobData:readFromYMLWithKey(jobData,pipeLineConstants.MODIFY_DATA)
    if(!isJobDataSkipOrIgnore(jobData)){ 	
        jobData.each{
            if(it.key.toString().equalsIgnoreCase(pipeLineConstants.MODIFY_DATA)){
                jobData.remove(it.key)
            }
          }
	}
     }

/**
 * This method will check the stage data for skipping/overriding/Appending
 * @param line,TEST_PROPERTY,testMap,parent
 */
private void processStageDataMap(line,testProperty,testMap,parent) {
    println "Checking the Value for modify flag"
    switch(parent.modifyData.toString().toLowerCase())
    {
        case pipeLineConstants.OVERRIDE:
            println "The value for modification was Override"
            break
        case pipeLineConstants.APPEND:
            println "The value for modification was Append"
            loadJobMaps(line,testProperty,testMap,parent)
            break
        case pipeLineConstants.SKIP:
            println "The value for modification was Skip"
	    parent.skipE2e=true
            testMap.clear()
            break
        default:
            println "No value specified in YML"
            break
    }
    println testMap.toString()

}

/**
*Method to check if the job is either skipped or ignored
*return false if value is not specified
*/
private boolean isJobDataSkipOrIgnore(jobData) {
boolean result=false
result = (jobData.toString().trim().equalsIgnoreCase(pipeLineConstants.IGNORE) || jobData.toString().trim().equalsIgnoreCase(pipeLineConstants.SKIP)) ? true : false
return result	
}

/**
*Method to checkand update stage data from serviceYML
*/
private void checkAndUpdateStageData(indent,testType,testMap,parent){
 def result =parent.userDataYML.(indent.toString()).(testType.toString().trim())
	if(result!=null){
	       processStageDataMap(line,testType,testMap,parent)
	    }
	    else{
	        processStageDataMap(pipeLineConstants.COMMON,testType,testMap,parent)
	    }	
}
