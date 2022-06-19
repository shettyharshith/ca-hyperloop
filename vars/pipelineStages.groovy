/**
 * This file contains all the stages required by the pipeline
 * @author Harshith
 */
/**
 * This method performs triggering of jenkins smoke test jobs 
 */
def smokeStage(def caller) {

	switch(true) {
		case (!caller.includeSmokeExecution):
			println pipeLineConstants.SMOKE_SKIP_MESSAGE1
			return
		case (!caller.continueExecution):
			println pipeLineConstants.SMOKE_SKIP_MESSAGE2
			return
		case caller.smokeMap.isEmpty():
			println pipeLineConstants.SMOKE_SKIP_MESSAGE3
			return
	}
	searchUtility.updateSearchMap(caller.smokeMap,caller)
	caller.smokeMap.each{
		if(caller.currentBuild.result.toString().trim().equalsIgnoreCase(pipeLineConstants.SUCCESS) || caller.healthCheck.toString().trim().equalsIgnoreCase(pipeLineConstants.SKIP)) {
			def smokeJobName = it.key
			def jobBuildParam=it.value
			List<hudson.model.ParameterValue> paramValueList=pipeLineUtil.parseBuildInputParam(jobBuildParam)
			def build = build job:smokeJobName,propagate: false,parameters:paramValueList
			reportUtil.updateBuildResult(caller,build,smokeJobName)
			reportUtil.setCurrentBuildStatus(caller,build)
			reportDataAllRow=reportUtil.updateReportData(caller,build,smokeJobName)
		}
	}
}
/**
 * This method performs Validation and parsing of the input parameters
 */

def initStage(def caller,def params) {
	Properties properties = readProperties  file:pipeLineConstants.PROPERTY_FOLDER+pipeLineConstants.VALIDATION_RULE
	pipeLineUtil.validateMandatoryFields(properties)
	pipeLineUtil.parseInputParams(caller,params)
	pipeLineUtil.init(caller)
	pipeLineUtil.updateStageInclusion(caller.testType,caller)
}

/**
 * This method performs triggering of client test jobs
 *like Kamaji
 */
def clientStage(def caller) {
	searchUtility.updateSearchMap(caller.clientMap,caller)
	caller.clientMap.each{
		def clientJobName = it.key
		def jobBuildParam=it.value
		List<hudson.model.ParameterValue> paramValueList=pipeLineUtil.parseBuildInputParam(jobBuildParam)
		def build = build job:clientJobName,propagate: false,parameters:paramValueList
		pipeLineUtil.updateBuildResultAndReportData(caller,build,clientJobName)
	}
}
/**
 * This method performs triggering of jenkins smoke test jobs
 */
def perfStage(def caller) {
	searchUtility.updateSearchMap(caller.perfMap,caller)
	switch(true) {
		case (!caller.includePerformanceTests):
			println pipeLineConstants.PERFTEST_SKIP_MESSAGE1
			return
		case (!caller.continueExecution):
			println pipeLineConstants.PERFTEST_SKIP_MESSAGE2
			return
		case caller.perfMap.isEmpty():
			println pipeLineConstants.PERFTEST_SKIP_MESSAGE3
			return
	}
	caller.perfMap.each{
		def perfJobName = it.key
		def jobBuildParam=it.value
		List<hudson.model.ParameterValue> paramValueList=pipeLineUtil.parseBuildInputParam(jobBuildParam)
		def build = build job:perfJobName,propagate: false,parameters:paramValueList
		reportUtil.updateBuildResult(caller,build,perfJobName)
		reportUtil.setCurrentBuildStatus(caller,build)
		reportDataAllRow=reportUtil.updateReportData(caller,build,perfJobName)
	}
}

/**
 * This method performs triggering of jenkins healthcheck jobs
 */

def healthCheckStage(def caller,def params) {
	if(!caller.includeHealthCheckExecution||caller.healthCheck.toString().trim().equalsIgnoreCase(pipeLineConstants.SKIP)||caller.healthCheck.toString().trim().equalsIgnoreCase(pipeLineConstants.IGNORE)) {
		println pipeLineConstants.HEALTHCHECK_SKIP_MESSAGE
		return
	}
	def eib_namme =  caller.envs+pipeLineConstants.EIB
	def paramList = [string(name: pipeLineConstants.EIB_ENV, value: eib_namme), string(name: pipeLineConstants.SERVICE_NAME, value: caller.componentName.toString().toLowerCase())]
	def build = build job:searchUtility.searchJob(pipeLineConstants.HEALTH_CHECK_JOB,caller),propagate: false, parameters: paramList
	reportUtil.updateBuildResult(caller,build,pipeLineConstants.HEALTH_CHECK)
	reportUtil.setCurrentBuildStatus(caller,build)
	reportDataAllRow=reportUtil.updateReportData(caller,build,pipeLineConstants.HEALTH_CHECK)
}

/**
 * This method performs triggering of jenkins regression test jobs
 */

def regressionStage(def caller,def params) {
	switch(true) {
		case (!caller.includeServiceRegressionExecution):
			println pipeLineConstants.REGRESSION_SKIP_MESSAGE1
			return
		case (!caller.continueExecution):
			println pipeLineConstants.REGRESSION_SKIP_MESSAGE2
			return
		case (caller.regressionMap.isEmpty()):
			println pipeLineConstants.REGRESSION_SKIP_MESSAGE3
			return
	}
	searchUtility.updateSearchMap(caller.regressionMap,caller)
	caller.runRegressionInParallel=(caller.runRegressionInParallel==null?false:caller.runRegressionInParallel.toBoolean())
	if(!caller.runRegressionInParallel) {
		println pipeLineConstants.RUN_IN_SEQUENCE
		caller.regressionMap.each{
			def regressionJobName = it.key
			def jobBuildParam=it.value
			List<hudson.model.ParameterValue> paramValueList=pipeLineUtil.parseBuildInputParam(jobBuildParam)
			println paramValueList
			def build = build job:regressionJobName,propagate: false,parameters:paramValueList
			reportUtil.updateBuildResult(caller,build,regressionJobName)
			reportUtil.setCurrentBuildStatus(caller,build)
			reportDataAllRow=reportUtil.updateReportData(caller,build,regressionJobName)
			echo caller.currentBuild.result
		}
	}
	else {
		println pipeLineConstants.RUN_IN_PARALLEL
		caller.regressionMap.each{
			def regressionJobName = it.key
			def jobBuildParam=it.value
			List<hudson.model.ParameterValue> paramValueList=pipeLineUtil.parseBuildInputParam(jobBuildParam)
			println paramValueList
			caller.dynamicCodeMap=caller.dynamicCodeMap+[(regressionJobName):{
				def build = build job:regressionJobName,propagate: false,parameters:paramValueList
				reportUtil.updateBuildResult(caller,build,regressionJobName)
				reportUtil.setCurrentBuildStatus(caller,build)
				reportDataAllRow=reportUtil.updateReportData(caller,build,regressionJobName)
			}]
		}
		parallel caller.dynamicCodeMap
	}
}

/**
 * This method performs triggering of custom test jobs
 *like Kamaji
 */
def customStage(def caller) {
	searchUtility.updateSearchMap(caller.customMap,caller)
	switch(true) {
		case (!caller.includeCustomTests):
			println pipeLineConstants.CUSTOMTEST_SKIP_MESSAGE1
			return
		case (!caller.continueExecution):
			println pipeLineConstants.CUSTOMTEST_SKIP_MESSAGE2
			return
		case caller.customMap.isEmpty():
			println pipeLineConstants.CUSTOMTEST_SKIP_MESSAGE3
			return
	}
	caller.customMap.each{
		def customJobName = it.key
		def jobBuildParam=it.value
		List<hudson.model.ParameterValue> paramValueList=pipeLineUtil.parseBuildInputParam(jobBuildParam)
		def build = build job:customJobName,propagate: false,parameters:paramValueList
		reportUtil.updateBuildResult(caller,build,customJobName)
		reportUtil.setCurrentBuildStatus(caller,build)
		reportDataAllRow=reportUtil.updateReportData(caller,build,customJobName)
	}
}
/**
 * This method performs triggering of jenkins e2e test jobs
 */
def e2eStage(def caller) {
	script{
		switch(true) {
			case (!caller.includeE2EExecution):
				println pipeLineConstants.E2E_SKIP_MESSAGE1
				return
			case (!caller.continueExecution):
				println pipeLineConstants.E2E_SKIP_MESSAGE2
				return
			case caller.e2eMap.isEmpty():
			        println pipeLineConstants.E2E_SKIP_MESSAGE3
			        return
		}
		caller.e2eMap.each{
			def e2eJobName = it.key
			def jobBuildParam=it.value
			List<hudson.model.ParameterValue> paramValueList=pipeLineUtil.parseBuildInputParam(jobBuildParam)
			def build = build job:e2eJobName,propagate: false,parameters:paramValueList
			reportUtil.updateBuildResult(caller,build,e2eJobName)
			reportUtil.setCurrentBuildStatus(caller,build)
			reportDataAllRow=reportUtil.updateReportData(caller,build,e2eJobName)
		}
	}
}


