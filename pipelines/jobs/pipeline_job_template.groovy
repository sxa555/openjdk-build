import groovy.json.JsonOutput

if(!binding.hasVariable('triggerSchedule')) {
    triggerSchedule = "@daily"
}

gitRefSpec = ""
propagateFailures = false
runTests = true
runInstaller = true
runSigner = true

// if true means this is running in the pr builder pipeline
if (binding.hasVariable('PR_BUILDER')) {
    triggerSchedule = "0 0 31 2 0"
    gitRefSpec = "+refs/pull/*:refs/remotes/origin/pr/* +refs/heads/master:refs/remotes/origin/master +refs/heads/*:refs/remotes/origin/*"
    propagateFailures = true
    runTests = false
    runInstaller = false
    runSigner = false
}

if (!binding.hasVariable('disableJob')) {
    disableJob = false
}

folder("${BUILD_FOLDER}")
folder("${BUILD_FOLDER}/jobs")

pipelineJob("${BUILD_FOLDER}/${JOB_NAME}") {
    description('<h1>THIS IS AN AUTOMATICALLY GENERATED JOB DO NOT MODIFY, IT WILL BE OVERWRITTEN.</h1><p>This job is defined in pipeline_job_template.groovy in the openjdk-build repo, if you wish to change it modify that.</p>')
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url("${GIT_URL}")
                        refspec(gitRefSpec)
                    }
                    branch("${BRANCH}")
                }
            }
            scriptPath(SCRIPT)
        }
    }
    disabled(disableJob)
    triggers {
        cron(triggerSchedule)
    }
    logRotator {
        numToKeep(60)
        artifactNumToKeep(2)
    }

    properties {
        copyArtifactPermission {
            projectNames('*')
        }
    }

    parameters {
        textParam('targetConfigurations', JsonOutput.prettyPrint(JsonOutput.toJson(targetConfigurations)))
        stringParam('activeNodeTimeout', "0", 'Number of minutes we will wait for a label-matching node to become active.')
        stringParam('dockerExcludes', "", 'Map of targetConfigurations to exclude from docker building. If a targetConfiguration (i.e. { "x64LinuxXL": [ "openj9" ], "aarch64Linux": [ "hotspot", "openj9" ] }) has been entered into this field, jenkins will build the jdk without using docker. This param overrides the dockerImage and dockerFile downstream job parameters.')
        choiceParam('releaseType', ['Nightly', 'Nightly Without Publish', 'Release'], 'Nightly - release a standard nightly build.<br/>Nightly Without Publish - run a nightly but do not publish.<br/>Release - this is a release, this will need to be manually promoted.')
        stringParam('overridePublishName', "", '<strong>REQUIRED for OpenJ9</strong>: Name that determines the publish name (and is used by the meta-data file), defaults to scmReference(minus _adopt if present).<br/>Nightly builds: Leave blank (defaults to a date_time stamp).<br/>OpenJ9 Release build Java 8 example <code>jdk8u192-b12_openj9-0.12.1</code> and for OpenJ9 Java 11 example <code>jdk-11.0.2+9_openj9-0.12.1</code>.')
        stringParam('scmReference', "", 'Tag name or Branch name from which to build. Nightly builds: Defaults to, Hotspot=dev, OpenJ9=openj9, others=master.</br>Release builds: For hotspot JDK8 this would be the OpenJDK tag, for hotspot JDK11+ this would be the Adopt merge tag for the desired OpenJDK tag eg.jdk-11.0.4+10_adopt, and for OpenJ9 this will be the release branch, eg.openj9-0.14.0.')
        booleanParam('enableTests', runTests, 'If set to true the test pipeline will be executed')
        booleanParam('enableInstallers', runInstaller, 'If set to true the installer pipeline will be executed')
        booleanParam('enableSigner', runSigner, 'If set to true the signer pipeline will be executed')
        stringParam('additionalConfigureArgs', "", "Additional arguments that will be ultimately passed to OpenJDK's <code>./configure</code>")
        stringParam('additionalBuildArgs', "", "Additional arguments to be passed to <code>makejdk-any-platform.sh</code>")
        stringParam('overrideFileNameVersion', "", "When forming the filename, ignore the part of the filename derived from the publishName or timestamp and override it.<br/>For instance if you set this to 'FOO' the final file name will be of the form: <code>OpenJDK8U-jre_ppc64le_linux_openj9_FOO.tar.gz</code>")
        booleanParam('cleanWorkspaceBeforeBuild', false, "Clean out the workspace before the build")
        booleanParam('propagateFailures', propagateFailures, "If true, a failure of <b>ANY</b> downstream build (but <b>NOT</b> test) will cause the whole build to fail")
        booleanParam('keepTestReportDir', false, 'If true, test report dir (including core files where generated) will be kept even when the testcase passes, failed testcases always keep the report dir. Does not apply to JUnit jobs which are always kept, eg.openjdk.')
        stringParam('adoptBuildNumber', "", "Empty by default. If you ever need to re-release then bump this number. Currently this is only added to the build metadata file.")
    }
}
