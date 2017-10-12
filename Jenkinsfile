// The GIT repository for this pipeline lib is defined in the global Jenkins setting
@Library('jenkins-pipeline-library')
import com.gentics.*

// Make the helpers aware of this jobs environment
JobContext.set(this)

properties([
	parameters([
		booleanParam(name: 'runTests',            defaultValue: true,  description: "Whether to run the unit tests"),
		booleanParam(name: 'runPerformanceTests', defaultValue: false, description: "Whether to run performance tests."),
		booleanParam(name: 'runDeploy',           defaultValue: false, description: "Whether to run the deploy steps."),
		booleanParam(name: 'runDocker',           defaultValue: false, description: "Whether to run the docker steps."),
		booleanParam(name: 'runMavenBuild',       defaultValue: false, description: "Whether to run the maven build steps."),
		booleanParam(name: 'runIntegrationTests', defaultValue: false, description: "Whether to run integration tests.")
	])
])

final def dockerHost           = "tcp://gemini.office:2375"
final def gitCommitTag         = '[Jenkins | ' + env.JOB_BASE_NAME + ']';

node("docker") {
	stage("Setup Build Environment") {

		stage("Checkout") {
			checkout scm
		}

		sh "cd .jenkins && docker build -t registry.gentics.com/jenkins-slave-mesh ."
		sh "cd .jenkins && docker push registry.gentics.com/jenkins-slave-mesh"

		podTemplate(containers: [containerTemplate(alwaysPullImage: true, args: '${computer.jnlpmac} ${computer.name}', 
				command: '', 
				envVars: [], 
				image: 'registry.gentics.com/jenkins-slave-mesh', 
				livenessProbe: containerLivenessProbe(execArgs: '', failureThreshold: 0, initialDelaySeconds: 0, periodSeconds: 0, successThreshold: 0, timeoutSeconds: 0), 
				name: 'jnlp', ports: [], 
				privileged: false, 
				resourceLimitCpu: '', 
				resourceLimitMemory: '', 
				resourceRequestCpu: '', 
				resourceRequestMemory: '', 
				ttyEnabled: true, 
				workingDir: '/home/jenkins/workspace')], 
				inheritFrom: '', 
				instanceCap: 0, 
				label: 'mesh', 
				name: 'jenkins-slave-mesh', 
				namespace: 'default', 
				nodeSelector: '', 
				serviceAccount: 'jenkins', 
				volumes: [
					hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock'), 
					persistentVolumeClaim(claimName: 'jenkins-maven-repository', mountPath: '/home/jenkins/.m2/repository', readOnly: false),
					persistentVolumeClaim(claimName: 'jenkins-credentials', mountPath: '/home/jenkins/credentials', readOnly: true)
				], 
				workspaceVolume: emptyDirWorkspaceVolume(false)) {
					node("mesh") {
						stage("Checkout") {
							checkout scm
						}

						def branchName = GitHelper.fetchCurrentBranchName()
						def version = MavenHelper.getVersion()

						stage("Set Version") {
							if (Boolean.valueOf(params.runDeploy)) {
								if (version) {
									echo "Building version " + version
									version = MavenHelper.transformSnapshotToReleaseVersion(version)
									MavenHelper.setVersion(version)
								}
								//TODO only add pom.xml files
								sh 'git add .'
								sh "git commit -m 'Raise version'"
								GitHelper.addTag(version, 'Release of version ' + version)
							}
						}

						stage("Test") {
							if (Boolean.valueOf(params.runTests)) {
							def splits = 20;
								sh "ls -la"
								sh "find -name \"*Test.java\" | grep -v Abstract | shuf | sed  's/.*java\\/\\(.*\\)/\\1/' > alltests"
								sh "cat alltests | wc -l"
								sh "split -a 2 -d -n l/${splits} alltests  includes-"
								stash includes: '**', name: 'project'
								def branches = [:]
								for (int i = 0; i < splits; i++) {
									def current = i
									branches["split${i}"] = {
										node('mesh') {
											echo "Preparing slave environment for ${current}"
											unstash 'project'
											def postfix = current;
											if (current <= 9) {
												postfix = "0" + current
											}
											echo "Setting correct inclusions file ${postfix}"
											sh "mv includes-${postfix} inclusions.txt"
											sshagent(["git"]) {
												try {
													sh "mvn -fae -Dmaven.javadoc.skip=true -Dmaven.test.failure.ignore=true -B -U -e -P inclusions -pl '!demo,!doc,!performance-tests' clean package"
												} finally {
													step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/*.xml'])
												}
											}
										}
									}
								}
								try {
									parallel branches
								} catch (err) {
									echo "Failed " + err.getMessage()
									error err.getMessage()
								}
							} else {
								echo "Tests skipped.."
							}
						}

						stage("Maven Build") {
							if (Boolean.valueOf(params.runMavenBuild)) {
								sshagent(["git"]) {
									if (Boolean.valueOf(params.runDeploy)) {
										sh "mvn -U -B -DskipTests clean deploy"
									} else {
										sh "mvn -B -DskipTests clean package"
									}
								}
							} else {
								echo "Maven build skipped.."
							}
						}

						stage("Docker Build") {
							if (Boolean.valueOf(params.runDocker)) {
								sh "rm demo/target/*sources.jar"
								sh "rm server/target/*sources.jar"
								sh "captain build"
							} else {
								echo "Docker build skipped.."
							}
						}

						stage("Performance Tests") {
							if (Boolean.valueOf(params.runPerformanceTests)) {
								try {
									sh "mvn -B -U clean package -pl '!doc,!demo,!verticles,!server' -Dskip.unit.tests=true -Dskip.performance.tests=false -Dmaven.test.failure.ignore=true"
								} finally {
									step([$class: 'JUnitResultArchiver', testResults: '**/target/*.performance.xml'])
								}
							} else {
								echo "Performance tests skipped.."
							}
						}

						stage("Integration Tests") {
							if (Boolean.valueOf(params.runIntegrationTests)) {
								withEnv(["MESH_VERSION=" + version]) {
									sh "integration-tests/test.sh"
								}
							} else {
								echo "Performance tests skipped.."
							}
						}

						stage("Deploy") {
							if (Boolean.valueOf(params.runDeploy)) {
								if (Boolean.valueOf(params.runDocker)) {
									withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'dockerhub_login', passwordVariable: 'DOCKER_HUB_PASSWORD', usernameVariable: 'DOCKER_HUB_USERNAME']]) {
										sh 'docker login -u $DOCKER_HUB_USERNAME -p $DOCKER_HUB_PASSWORD'
										sh "captain push"
									}
								}
							} else {
								echo "Deploy skipped.."
							}
						}

						stage("Git push") {
							if (Boolean.valueOf(params.runDeploy)) {
								sshagent(["git"]) {
									def snapshotVersion = MavenHelper.getNextSnapShotVersion(version)
									MavenHelper.setVersion(snapshotVersion)
									GitHelper.addCommit('.', gitCommitTag + ' Prepare for the next development iteration (' + snapshotVersion + ')')
									GitHelper.pushBranch(branchName)
									GitHelper.pushTag(version)
								}
							} else {
								echo "Push skipped.."
							}
						}
					}
		}
	}
}

