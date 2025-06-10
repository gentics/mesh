@Library('jenkins-pipeline-library') import com.gentics.*
JobContext.set(this)

javaPipelineWithImage imageName: "java-worker:17.2", sonarProjectKey: "mesh-commercial-plugins", {
    scanFilesystem path: "$PWD", exitCode: 1, params: '--scanners vuln'
}
