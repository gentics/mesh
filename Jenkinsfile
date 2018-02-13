// The GIT repository for this pipeline lib is defined in the global Jenkins setting
@Library('jenkins-pipeline-library')
import com.gentics.*

// Make the helpers aware of this jobs environment
JobContext.set(this)

properties([
	parameters([
		booleanParam(name: 'runTests',            defaultValue: true,  description: "Whether to run the unit tests"),
		booleanParam(name: 'runPerformanceTests', defaultValue: false, description: "Whether to run performance tests."),
		booleanParam(name: 'runClusterTests',     defaultValue: false, description: "Whether to run cluster tests."),
		booleanParam(name: 'runDeploy',           defaultValue: false, description: "Whether to run the deploy steps."),
		booleanParam(name: 'runDocker',           defaultValue: false, description: "Whether to run the docker steps."),
		booleanParam(name: 'runMavenBuild',       defaultValue: false, description: "Whether to run the maven build steps."),
		booleanParam(name: 'runIntegrationTests', defaultValue: false, description: "Whether to run integration tests.")
	])
])

final def gitCommitTag         = '[Jenkins | ' + env.JOB_BASE_NAME + ']';

node("docker") {
	stage("Setup Build Environment") {
		checkout scm
		sh "cd .jenkins && docker build -t registry.gentics.com/jenkins-slave-mesh ."
		sh "cd .jenkins && docker push registry.gentics.com/jenkins-slave-mesh"

		podTemplate(containers: [
			containerTemplate(alwaysPullImage: true, args: '${computer.jnlpmac} ${computer.name}',
				command: '',
				image: 'registry.gentics.com/jenkins-slave-mesh',
				name: 'jnlp',
				privileged: false,
				ttyEnabled: true,
				resourceRequestCpu: '1000m',
				resourceRequestMemory: '2048Mi',
				workingDir: '/home/jenkins/workspace'),

			containerTemplate(alwaysPullImage: false, 
				command: '',
				image: 'docker:dind',
				name: 'dind',
				privileged: true,
				ttyEnabled: true,
				resourceRequestCpu: '250m',
				resourceRequestMemory: '1024Mi',
				workingDir: '/root')
				],

				inheritFrom: '',
				instanceCap: 10,
				label: 'mesh',
				name: 'jenkins-slave-mesh',
				namespace: 'default', 
				serviceAccount: 'jenkins',
				volumes: [
					emptyDirVolume(memory: false, mountPath: '/var/run'),
					hostPathVolume(hostPath: '/opt/jenkins-slave/maven-repo', mountPath: '/home/jenkins/.m2/repository'),
					persistentVolumeClaim(claimName: 'jenkins-credentials', mountPath: '/home/jenkins/credentials', readOnly: true)
				], 
				workspaceVolume: emptyDirWorkspaceVolume(false)) {
					node("mesh") {
						//env.DOCKER_HOST = "tcp://127.0.0.1:2375"

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
													sh "mvn -fae -Dmaven.javadoc.skip=true -Dskip.cluster.tests=true -Dmaven.test.failure.ignore=true -B -U -e -P inclusions -pl '!demo,!doc,!performance-tests' clean package"
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

						stage("Cluster Tests") {
							if (Boolean.valueOf(params.runClusterTests)) {
								try {
									sh "mvn -B -DskipTests clean install -pl '!demo,!doc'"
									sh "mvn -B test -pl distributed"
								} finally {
									step([$class: 'JUnitResultArchiver', testResults: 'distributed/target/surefire-reports/*.xml'])
								}
							} else {
								echo "Cluster tests skipped.."
							}
						}

						stage("Docker Build") {
							if (Boolean.valueOf(params.runDocker)) {
								// demo
								sh "rm demo/target/*sources.jar"
								sh "cd demo ; docker build -t gentics/mesh-demo:latest -t gentics/mesh-demo:" + version + " . "
								
								// server
								sh "rm server/target/*sources.jar"
								sh "cd server ; docker build -t gentics/mesh:latest -t gentics/mesh:" + version + " . "
							} else {
								echo "Docker build skipped.."
							}
						}

						stage("Performance Tests") {
							if (Boolean.valueOf(params.runPerformanceTests)) {
								try {
									sh "mvn -B -U clean package -pl '!doc,!demo,!distributed,!verticles,!server' -Dskip.unit.tests=true -Dskip.performance.tests=false -Dmaven.test.failure.ignore=true"
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
										sh 'docker push gentics/mesh-demo:latest'
										sh 'docker push gentics/mesh-demo:' + version
										sh 'docker push gentics/mesh:latest'
										sh 'docker push gentics/mesh:' + version
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
