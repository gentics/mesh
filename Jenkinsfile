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
		booleanParam(name: 'runReleaseBuild',     defaultValue: false, description: "Whether to run the release steps."),
		booleanParam(name: 'runIntegrationTests', defaultValue: false, description: "Whether to run integration tests.")
	])
])

final def dockerHost           = "tcp://gemini.office:2375"
final def gitCommitTag         = '[Jenkins | ' + env.JOB_BASE_NAME + ']';

node("testpod") {
	stage("Checkout") {
		checkout scm
	}
	def branchName = GitHelper.fetchCurrentBranchName()

	stage("Set Version") {
		if (Boolean.valueOf(params.runReleaseBuild)) {
			version = MavenHelper.getVersion()
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
			try {
				sh "mvn -fae -Dmaven.test.failure.ignore=true -B -U -e -pl '!demo,!doc,!server,!performance-tests' clean test"
			} finally {
				step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/*.xml'])
			}
		} else {
			echo "Tests skipped.."
		}
	}

	stage("Release Build") {
		if (Boolean.valueOf(params.runReleaseBuild)) {
			sshagent(["git"]) {
				sh "mvn -B -DskipTests clean package"
			}
		} else {
			echo "Release build skipped.."
		}
	}

	stage("Docker Build") {
		if (Boolean.valueOf(params.runDocker)) {
			withEnv(["DOCKER_HOST=" + dockerHost ]) {
				sh "rm demo/target/*sources.jar"
				sh "rm server/target/*sources.jar"
				sh "captain build"
			}
		} else {
			echo "Docker build skipped.."
		}
	}

	stage("Performance Tests") {
		if (Boolean.valueOf(params.runPerformanceTests)) {
			container('jnlp') {
				checkout scm
				try {
					sh "mvn -B -U clean package -pl '!doc,!demo,!verticles,!server' -Dskip.unit.tests=true -Dskip.performance.tests=false -Dmaven.test.failure.ignore=true"
				} finally {
					//step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/*.xml'])
					step([$class: 'JUnitResultArchiver', testResults: '**/target/*.performance.xml'])
				}
			}
		} else {
			echo "Performance tests skipped.."
		}
	}

	stage("Integration Tests") {
		if (Boolean.valueOf(params.runIntegrationTests)) {
			withEnv(["DOCKER_HOST=" + dockerHost, "MESH_VERSION=" + version]) {
				sh "integration-tests/test.sh"
			}
		} else {
			echo "Performance tests skipped.."
		}
	}

	stage("Deploy") {
		if (Boolean.valueOf(params.runDeploy)) {
			if (Boolean.valueOf(params.runDocker)) {
				withEnv(["DOCKER_HOST=" + dockerHost]) {
					withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'dockerhub_login', passwordVariable: 'DOCKER_HUB_PASSWORD', usernameVariable: 'DOCKER_HUB_USERNAME']]) {
						sh 'docker login -u $DOCKER_HUB_USERNAME -p $DOCKER_HUB_PASSWORD -e entwicklung@genitcs.com'
						sh "captain push"
					}
				}
			}
			sshagent(["git"]) {
				sh "mvn -U -B -DskipTests clean deploy"
			}
		} else {
			echo "Deploy skipped.."
		}
	}

	stage("Git push") {
		if (Boolean.valueOf(params.runReleaseBuild)) {
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

