import groovy.sql.Sql
import groovy.transform.Field
import java.util.ArrayList
import groovy.json.JsonSlurper
import com.google.gson.Gson

/**
 * Read jenkins.test.yml and retrieve attributes required for
 * reporting
 * @param properties_url
 * @return
 */
public LinkedHashMap getTestYmlProperties(yml_url) {

    if (yml_url == null) {
        print 'Input YML URL is blank'
        return
    }

    print "Test YML Link: " + yml_url
    def yml_entries_map = [:]
    def stages = ''
    try {
        yml_response = gitUtil.getGitResponse(yml_url)
        // This is a check for YML with unparseable text.
        yml_response = yml_response.replace("]-eib", "]")
        
        def yml = readYaml text: yml_response
        yml_common_map = [:]
        def common = yml.Common
        if (common != null) {
            yml_common_map.Test = (common.Test != null) ? common.Test.value : ""
            yml_common_map.HealthCheck =  common.HealthCheck
            yml_common_map.ServiceSmoke =  common.ServiceSmoke
            yml_common_map.ServiceRegression = common.ServiceRegression
            yml_common_map.ServicePerformance = common.ServicePerformance
            print "Map for common attributes: " + yml_common_map.toString()
        }
        pipeLineConstants.VALID_LINES.each {
            x ->
                yml_line_map = [:]
                yml_line_map = pipeLineConstants.DEFAULT_YML_ATTRIBUTES.clone()
                yml.collect {
                    u ->
                        if (u.toString().contains(x.toString())) {
                            def eval_str = u.value.ServiceSmoke
                            yml_line_map.ServiceSmoke = u.value.ServiceSmoke
                            yml_line_map.ServiceRegression = u.value.ServiceRegression
                            yml_line_map.ServicePerformance = u.value.ServicePerformance
                            if(u.value.HealthCheck!=null){
                                yml_line_map.HealthCheck = u.value.HealthCheck
                            }
                            yml_line_map.EnableAjex = u.value.EnableAjex
                            if ( yml_line_map.EnableAjex ) { 
                                if ( u.value.ServiceTests != null ) {
                                    def service_tests = u.value.ServiceTests.toString().split(",")
                                    service_tests.each { s -> 
                                        if (s.toString().trim().contains("Stages")){
                                            stages  = s.toString().split(":")[1]
                                        }
                                    }
                                    if(stages!=null){
                                         yml_line_map.Stages = stages
                                    }
                                 }
                            }
                            
                        }
                }
                yml_entries_map.put(x, yml_line_map)
        }
        yml_entries_map.put("common", yml_common_map )
    } catch(Exception e) {
        println "An error occurred while getting test yml properties: " + e
    }
    return yml_entries_map
}


/**
 * Checks the map and identify if a certain test type has been chosen
 * to skip or ignore or empty
 * * @param yml
 * @return
 */
public LinkedHashMap evaluateTestYmlOptions(yml_map) {

    def yml_usage_map = [:]
    try {
        pipeLineConstants.VALID_LINES.each {
            x ->
                yml_status_map = [:]
                yml_status_map = pipeLineConstants.DEFAULT_YML_STATUS.clone()
                yml_map.collect {
                    u ->
                        if (u.toString().contains(x.toString())) {
                            yml_status_map.smoke = checkSkipped(u.value.ServiceSmoke) ? false : true
                            yml_status_map.regression = checkSkipped(u.value.ServiceRegression) ? false : true
                            yml_status_map.perf = checkSkipped(u.value.ServicePerformance) ? false : true
                            yml_status_map.enableAjex = checkIfAjexIsNotEnabled(u.value.EnableAjex) ? false : true
                            yml_status_map.stages = checkSkipped(u.value.Stages) ? "none" : u.value.Stages
                        }
                        if (x.toString().contains("common")) {
                            /**
                             * This logic follows the hierarchy based on which the tests will eventually be triggered
                             */
                            def test_check = (u.common != null && u.common.Test != null) ? u.common.Test.value : ""
                            def regression_check = test_check.equalsIgnoreCase("regression") ? true : false
                            def performance_check = test_check.equalsIgnoreCase("performance") ? true : false
                            def e2e_check = (test_check.equalsIgnoreCase("all")) ||
                                    test_check.equalsIgnoreCase("e2e_smoke") ? true : false
                            def smoke_check = regression_check || performance_check || e2e_check ||
                                    test_check.equalsIgnoreCase("smoke") ? true : false
                            def health_check = regression_check ||  smoke_check ||  e2e_check || performance_check ||
                                    test_check.equalsIgnoreCase("health_check")? true : false
                            yml_status_map.health = health_check && checkSkipped(u.common.HealthCheck) ? false : true
                            yml_status_map.smoke =  smoke_check && checkSkipped(u.common.ServiceSmoke) ? false : yml_status_map.smoke
                            yml_status_map.regression = regression  && checkSkipped(u.common.ServiceRegression) ? false : yml_status_map.regression
                            yml_status_map.perf = performance_check && checkSkipped(u.common.ServicePerformance) ? false : yml_status_map.perf
                            yml_status_map.e2e =  e2e_check
                        }
                }
                yml_usage_map.put(x, yml_status_map)
        }
    } catch (Exception e) {
        println "An error occurred while evaluating the test yml entries: " + e
    }
    return yml_usage_map
}


/*
 Check status if any of the options were ignores or skipped
 */

public boolean checkSkipped(eval_str) {
    return (eval_str == null ||
            eval_str.isEmpty() ||
            (eval_str.toString().equalsIgnoreCase("ignore") ||
                    eval_str.toString().equalsIgnoreCase("skip")) || eval_str.toString().equalsIgnoreCase("false"))

}


/*
 * Check status if AJEX is enabled
 */

public boolean checkIfAjexIsNotEnabled(eval_bool) {
    return (eval_bool == null || eval_bool == false)
}

/**
 * Read jenkins.deploy.properties and retrieve keys for
 * SnapshotTestEnvironment and ReleaseTestEnvironment
 *
 * @param properties_url
 * @return
 */
public LinkedHashMap getDeployProperties(properties_url) {
    def validEntries = [:]
    try {
        properties_response = gitUtil.getGitResponse(properties_url)
        properties_response = properties_response.toString()
        validEntries = properties_response.split('\n').collect { it }
        validEntries.removeAll {
            u ->
                !(u.contains(pipeLineConstants.JENKINS_DEPLOY_KEYS[0])
                        || u.contains(pipeLineConstants.JENKINS_DEPLOY_KEYS[1]))
        }
        print validEntries

    } catch (Exception e) {
        println "An error occurred while getting deploy properties: " + e
    }
    return validEntries
}

