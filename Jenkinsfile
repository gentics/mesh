// The GIT repository for this pipeline lib is defined in the global Jenkins setting
@Library('jenkins-pipeline-library@nexus')
import com.gentics.*

// Make the helpers aware of this jobs environment
JobContext.set(this)

final def imagePrefix		   = "push.docker.gentics.com/docker-products/"

properties([
	parameters([
		booleanParam(name: 'runTests',            defaultValue: true,  description: "Whether to run the unit tests"),
		booleanParam(name: 'splitTests',          defaultValue: false, description: "Whether to split tests or not"),
		booleanParam(name: 'runSonar',            defaultValue: false, description: "Whether to run the sonarqube checks"),
		booleanParam(name: 'runPerformanceTests', defaultValue: false, description: "Whether to run performance tests."),
		booleanParam(name: 'runClusterTests',     defaultValue: false, description: "Whether to run cluster tests."),
		booleanParam(name: 'runDeploy',           defaultValue: false, description: "Whether to run the deploy steps."),
		booleanParam(name: 'runDocker',           defaultValue: false, description: "Whether to run the docker steps."),
		booleanParam(name: 'runMavenBuild',       defaultValue: false, description: "Whether to run the maven build steps."),
		booleanParam(name: 'runIntegrationTests', defaultValue: false, description: "Whether to run integration tests.")
	])
])

final def gitCommitTag         = '[Jenkins | ' + env.JOB_BASE_NAME + ']';
final def splits = 10;
final def runs = 10;

stage("Setup Build Environment") {
	node("mesh-worker") {
		githubBuildStarted()
		try {
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
					if (Boolean.valueOf(params.splitTests)) {
						sh ".jenkins/split-tests.sh ${splits}"
						stash includes: '**', name: 'project'
						def branches = [:]
						for (int i = 0; i < runs; i++) {
							def current = i
							branches["split${i}"] = {
								node('mesh-worker') {
									echo "Preparing slave environment for ${current}"
									sh "rm -rf split${i}"
									sh "mkdir split${i}"
									dir("split${i}") {
										unstash 'project'
										def postfix = current;
										if (current <= 9) {
											postfix = "0" + current
										}
										echo "Setting correct inclusions file ${postfix}"
										sh "mv includes-${postfix} inclusions.txt"
										sshagent(["git"]) {
											try {
												sh "mvn -fae -Dmaven.javadoc.skip=true -Dskip.cluster.tests=true -Dmaven.test.failure.ignore=true -B -U -e -P inclusions -pl '!doc,!performance-tests' clean install"
												stash name: "jacoco" + current, includes: "**/jacoco-partial.exec", allowEmpty: true
											} finally {
												step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/*.xml'])
											}
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
						sshagent(["git"]) {
							try {
								sh "mvn -fae -Dmaven.javadoc.skip=true -Dskip.cluster.tests=true -Dmaven.test.failure.ignore=true -B -U -e -pl '!doc,!performance-tests' clean install"
							} finally {
								step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/*.xml'])
							}
						}
					}
				} else {
					echo "Tests skipped.."
				}
			}

			stage("Maven Build") {
				if (Boolean.valueOf(params.runMavenBuild)) {
					sshagent(["git"]) {
						if (Boolean.valueOf(params.runDeploy)) {
							withCredentials([usernamePassword(credentialsId: 'docker.gentics.com', usernameVariable: 'repoUsername', passwordVariable: 'repoPassword')]) {
								sh "mvn -U -B -Dgpg.skip=false -DskipTests clean deploy"
							}
						} else {
							sh "mvn -B -Dgpg.skip=false -DskipTests clean verify"
						}
					}
				} else {
					echo "Maven build skipped.."
				}
			}

			stage ("Sonar") {
				if (Boolean.valueOf(params.runTests) && Boolean.valueOf(params.runSonar)) {
					if (Boolean.valueOf(params.splitTests)) {
						for (int i = 0; i < runs; i++) {
							echo "Invoking merge for " + i
							unstash "jacoco" + i
							sh "mvn jacoco:merge@merge-all-jacoco -Dskip.jacoco.merge=false"
							sh "find -name jacoco-partial.exec -exec rm {} \\;"
						}
					}

					// Run the maven build if the build did not run previously
					if (!Boolean.valueOf(params.runMavenBuild)) {
						sh "mvn -B -DskipTests package"
					}
					withCredentials([string(credentialsId: 'sonarcube.key', variable: 'TOKEN')]) {
						sh "mvn sonar:sonar -Dsonar.projectKey=mesh -Dsonar.host.url=https://sonarqube.apa.at -Dsonar.login=$TOKEN"
					}
				}
			}

			stage("Cluster Tests") {
				if (Boolean.valueOf(params.runClusterTests)) {
					node("mesh-cluster-worker") {
						try {
							sh "mvn -B -DskipTests clean install -pl '!doc'"
							sh "mvn -B test -pl distributed"
						} finally {
							step([$class: 'JUnitResultArchiver', testResults: 'distributed/target/surefire-reports/*.xml'])
						}
					}
				} else {
					echo "Cluster tests skipped.."
				}
			}

			stage("Docker Build") {

				if (Boolean.valueOf(params.runDocker)) {
					// server
					sh "rm server/target/*sources.jar || true"
					sh "rm server/target/*javadoc.jar || true"
					sh "cd server ; docker build --network=host -t " + imagePrefix + "gentics/mesh:latest -t " + imagePrefix + "gentics/mesh:" + version + " . "
				} else {
					echo "Docker build skipped.."
				}
			}

			stage("Performance Tests") {
				if (Boolean.valueOf(params.runPerformanceTests)) {
					node("mesh-performance-worker") {
						try {
							sh "mvn -B -U clean package -pl '!doc,!distributed,!verticles,!server' -Dskip.unit.tests=true -Dskip.performance.tests=false -Dmaven.test.failure.ignore=true"
						} finally {
							step([$class: 'JUnitResultArchiver', testResults: '**/target/*.performance.xml'])
						}
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
					withCredentials([usernamePassword(credentialsId: 'docker.gentics.com', usernameVariable: 'repoUsername', passwordVariable: 'repoPassword')]) {
						sh 'cd js'

						// Install dependencies
						sh 'npm ci --no-audit --no-fund'

						// Set the version for the packages
						sh "npm run nx -- release version ${version}"

						// Build all JS packages
						sh 'npm run nx -- run-many --targets build'

						// Publish the pacakges to artifactory
						sh 'npm run nx -- release publish'

						// Go back to the project root
						sh 'cd ..'
					}

					if (Boolean.valueOf(params.runDocker)) {
						withCredentials([usernamePassword(credentialsId: 'docker.gentics.com', usernameVariable: 'repoUsername', passwordVariable: 'repoPassword')]) {
							sh 'docker login -u $repoUsername -p $repoPassword docker.gentics.com'
							sh 'docker push ' + imagePrefix + 'gentics/mesh-demo:latest'
							sh 'docker push ' + imagePrefix + 'gentics/mesh-demo:' + version
							sh 'docker push ' + imagePrefix + 'gentics/mesh:latest'
							sh 'docker push ' + imagePrefix + 'gentics/mesh:' + version
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
		} catch (Exception e) {
			if (currentBuild.result == null || currentBuild.currentResult == "STABLE") {
				currentBuild.result = 'FAILURE'
				currentBuild.currentResult = 'FAILURE'
			}
			throw e
		} finally {
			githubBuildEnded()
			notifyMattermostUsers()
		}
	}
}
