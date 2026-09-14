@Library('jenkins-pipeline-library') import com.gentics.*
JobContext.set(this)

javaPipelineWithImage imageName: "java-worker:17.2", sonarProjectKey: "mesh-commercial-plugins", mavenOpts: "-Xmx1g -XX:MaxMetaspaceSize=256m" {
    scanRootFilesystem path: "$WORKSPACE", exitCode: 1, offline: true
}
