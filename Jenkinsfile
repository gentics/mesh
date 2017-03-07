properties([[$class: 'ParametersDefinitionProperty', parameterDefinitions: [
[$class: 'BooleanParameterDefinition', name: 'skipTests', defaultValue: false],
[$class: 'BooleanParameterDefinition', name: 'skipDocker', defaultValue: false],
[$class: 'BooleanParameterDefinition', name: 'skipReleaseBuild', defaultValue: false],
[$class: 'BooleanParameterDefinition', name: 'skipIntegrationTests', defaultValue: false],
[$class: 'BooleanParameterDefinition', name: 'skipPerformanceTests', defaultValue: false],
[$class: 'BooleanParameterDefinition', name: 'skipDeploy', defaultValue: false]
]]])

stage 'Test'
if (!Boolean.valueOf(skipTests)) {
	def splits = splitTests parallelism: [$class: 'CountDrivenParallelism', size: 5], generateInclusions: true
	def branches = [:]
	for (int i = 0; i < splits.size(); i++) {
		def split = splits[i]
		branches["split${i}"] = {
			node('mesh') {
				sh 'rm -rf *'
				checkout scm
				writeFile file: (split.includes ? 'inclusions.txt' : 'exclusions.txt'), text: split.list.join("\n")
				writeFile file: (split.includes ? 'exclusions.txt' : 'inclusions.txt'), text: ''
				def mvnHome = tool 'M3'
				sshagent(['601b6ce9-37f7-439a-ac0b-8e368947d98d']) {
					try {
						sh "${mvnHome}/bin/mvn -pl '!demo,!doc,!server' -B clean test"
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

node('dockerRoot') {

	stage 'Checkout'
	sh "rm -rf *"
	sh "rm -rf .git"
	checkout scm
	//checkout([$class: 'GitSCM', branches: [[name: '*/' + env.BRANCH_NAME]], extensions: [[$class: 'CleanCheckout'],[$class: 'LocalBranch', localBranch: env.BRANCH_NAME]]])
	def mvnHome = tool 'M3'


	stage 'Set Version'
	def originalV = version();
	def major = originalV[1];
	def minor = originalV[2];
	def patch  = Integer.parseInt(originalV[3]) + 1;
	def v = "${major}.${minor}.${patch}"
	if (v) {
		echo "Building version ${v}"
	}
	sh "${mvnHome}/bin/mvn -B versions:set -DgenerateBackupPoms=false -DnewVersion=${v}"
	//TODO only add pom.xml files
	sh 'git add .'
	sh "git commit -m 'Raise version'"
	sh "git tag ${v}"


	stage 'Release Build'
	if (!Boolean.valueOf(skipReleaseBuild)) {
		sshagent(['601b6ce9-37f7-439a-ac0b-8e368947d98d']) {
			sh "${mvnHome}/bin/mvn -B -DskipTests clean package"
		}
	} else {
		echo "Release build skipped.."
	}


	stage 'Docker Build'
	if (!Boolean.valueOf(skipDocker)) {
		withEnv(['DOCKER_HOST=tcp://gemini.office:2375']) {
			sh "rm demo/target/*sources.jar"
			sh "rm server/target/*sources.jar"
			sh "captain build"
		}
	} else {
		echo "Docker build skipped.."
	}


	stage 'Performance Tests'
	if (!Boolean.valueOf(skipPerformanceTests)) {
		echo "Not yet implemented"
	} else {
		echo "Performance tests skipped.."
	}


	stage 'Integration Tests'
	if (!Boolean.valueOf(skipPerformanceTests)) {
		withEnv(['DOCKER_HOST=tcp://gemini.office:2375', "MESH_VERSION=${v}"]) {
			sh "integration-tests/test.sh"
		}
	} else {
		echo "Performance tests skipped.."
	}


	stage 'Deploy/Push'
	if (!Boolean.valueOf(skipDeploy)) {

		withEnv(['DOCKER_HOST=tcp://gemini.office:2375']) {
			withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'dockerhub_login', passwordVariable: 'DOCKER_HUB_PASSWORD', usernameVariable: 'DOCKER_HUB_USERNAME']]) {
				sh 'docker login -u $DOCKER_HUB_USERNAME -p $DOCKER_HUB_PASSWORD -e entwicklung@genitcs.com'
				sh "captain push"
			}
		}
		sshagent(['601b6ce9-37f7-439a-ac0b-8e368947d98d']) {
			sh "${mvnHome}/bin/mvn -B -DskipTests clean deploy"
			def gitCommit = sh(returnStdout: true, script: 'git rev-parse --abbrev-ref HEAD').trim()
			sh "git push origin " + gitCommit
			sh "git push origin ${v}"
		}
	} else {
		echo "Deploy/Push skipped.."
	}

}

def version() {
	def matcher = readFile('pom.xml') =~ '<version>(\\d*)\\.(\\d*)\\.(\\d*)(-SNAPSHOT)*</version>'
	matcher ? matcher[0] : null
}
