/**
 * Script for searching logic used in pipeline
 * @author ashrivastave1
 * */

def initializeCTJobsFolder(def caller) {
//initialize jenkins folder with all jobs
    def jenkinsFolder = Jenkins.instance.allItems
    jenkinsFolder.each { job ->
        caller.folders.add(job.fullName)
    }
}

/**
 * This method search for a job if not fond it will throw an error
 */
def searchJob(def testJobName,def caller) {
	if(testJobName.toString().contains("/"))
	return testJobName.toString()
    def jobList = caller.folders.findAll {
        it = !(it.toString().lastIndexOf("/") == -1) ? it.toString().trim().substring(it.toString().lastIndexOf("/") + 1) : it
        it.toString().equalsIgnoreCase(testJobName)
    }
	println "TestJobNameList Is:"
	println jobList
    if (jobList.size() > 1) {
        return updateJobSearchResult(pipeLineConstants.SEARCH_MESSAGE_PART1 + testJobName + pipeLineConstants.SEARCH_MESSAGE_PART2,caller)

    }
    def job = (!jobList.isEmpty() ? jobList.first() : "")
    job = Jenkins.instance.getItemByFullName(job.toString())
	println "TestJob full Name Is:"
	println job
    return (job != null ? job.fullName : updateJobSearchResult(testJobName + pipeLineConstants.JOB_UNAVAILABLE_MESSAGE,caller))
}

/**
 * This method will add the job to not found array and return proper error message
 */
String updateJobSearchResult(testJobName,def caller) {

    caller.jobSearchResultArray.add(testJobName)
    return pipeLineConstants.DATA_UNAVAILABLE_MESSAGE
}
/**
 * this job will update the map passed to it with search results
 * */
def updateSearchMap(def map,def caller) {
    map.each {
        def jobName = it.key
        def jobBuildParam = it.value
        def jobFullName = searchJob(jobName,caller)
        if (!jobFullName.equalsIgnoreCase(pipeLineConstants.DATA_UNAVAILABLE_MESSAGE)) {
            map.remove(jobName)
            map.put(jobFullName.toString(), jobBuildParam)
        } else {
            map.remove(jobName)
        }

    }
}
