properties([[$class: 'ParametersDefinitionProperty', parameterDefinitions: [
[$class: 'BooleanParameterDefinition', name: 'skipTests', defaultValue: false],
[$class: 'BooleanParameterDefinition', name: 'skipPerformanceTests', defaultValue: false],
[$class: 'BooleanParameterDefinition', name: 'skipDocker', defaultValue: false],
[$class: 'BooleanParameterDefinition', name: 'skipIntegrationTests', defaultValue: false],
[$class: 'BooleanParameterDefinition', name: 'skipReleaseBuild', defaultValue: false],
[$class: 'BooleanParameterDefinition', name: 'skipDeploy', defaultValue: false]
]]])

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



	stage 'Test'
	if (!Boolean.valueOf(skipTests)) {
		def splits = 25;
		sh "find -name \"*Test.java\" | grep -v Abstract | shuf | sed  's/.*java\\/\\(.*\\)/\\1/' > alltests"
		sh "split -a 2 -d -n l/${splits} alltests  includes-"
		stash includes: '*', name: 'project'
		def branches = [:]
		for (int i = 0; i < splits; i++) {
			def current = i
			branches["split${i}"] = {
				node('dockerSlave') {
					echo "Preparing slave environment for ${current}"
					//sh "ls -la"
					sh "rm -rf *"
					checkout scm
					//checkout([$class: 'GitSCM', branches: [[name: '*/' + env.BRANCH_NAME]],extensions: [[$class: 'CleanCheckout'],[$class: 'LocalBranch', localBranch: env.BRANCH_NAME]]])
					unstash 'project'
					sh "ls -la"
					def postfix = current;
					if (current <= 9) {
						postfix = "0" + current 
					}
					echo "Setting correct inclusions file ${postfix}"
					sh "mv includes-${postfix} inclusions.txt"
					sshagent(['601b6ce9-37f7-439a-ac0b-8e368947d98d']) {
						try {
							sh "${mvnHome}/bin/mvn -e -pl '!demo,!doc,!server,!performance-tests' -B clean test"
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
			sh "captain build"
		}
	} else {
		echo "Docker build skipped.."
	}


	stage 'Performance Tests'
	if (!Boolean.valueOf(skipPerformanceTests)) {
		node('cetus') {
			sh "rm -rf *"
			sh "rm -rf .git"
			checkout scm
			//checkout([$class: 'GitSCM', branches: [[name: '*/' + env.BRANCH_NAME]], extensions: [[$class: 'CleanCheckout'],[$class: 'LocalBranch', localBranch: env.BRANCH_NAME]]])
			try {
				sh "${mvnHome}/bin/mvn -B clean package -pl '!doc,!demo,!verticles,!server' -Dskip.unit.tests=true -Dskip.performance.tests=false"
			} finally {
				//step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/*.xml'])
				step([$class: 'JUnitResultArchiver', testResults: '**/target/*.performance.xml'])
			}
		}
	} else {
		echo "Performance tests skipped.."
	}


	stage 'Integration Tests'
	if (!Boolean.valueOf(skipIntegrationTests)) {
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
			sh "git push origin " + env.BRANCH_NAME
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
