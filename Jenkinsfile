properties([[$class: 'ParametersDefinitionProperty', parameterDefinitions: [
[$class: 'BooleanParameterDefinition', name: 'skipTests', defaultValue: false],
[$class: 'BooleanParameterDefinition', name: 'skipDocker', defaultValue: false]
]]])

stage 'Test'
if (Boolean.valueOf(skipTests)) {
	echo "Skipped"
} else {
	def splits = splitTests parallelism: [$class: 'CountDrivenParallelism', size: 5], generateInclusions: true
	def branches = [:]
	for (int i = 0; i < splits.size(); i++) {
		def split = splits[i]
		branches["split${i}"] = {
			node('dockerSlave') {
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
}

node('dockerSlave') {
	stage 'Checkout'
	sh "rm -rf *"
	sh "rm -rf .git"
	checkout scm
	checkout([$class: 'GitSCM', branches: [[name: '*/' + env.BRANCH_NAME]],
		extensions: [[$class: 'CleanCheckout'],[$class: 'LocalBranch', localBranch: env.BRANCH_NAME]]])
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
	sshagent(['601b6ce9-37f7-439a-ac0b-8e368947d98d']) {
		sh "${mvnHome}/bin/mvn -B -DskipTests -Ddocker.skip=false -Ddocker.tag=latest clean deploy"
		sh "git push origin " + env.BRANCH_NAME
		sh "git push origin ${v}"
	}
	stage 'Docker Build'
	if (Boolean.valueOf(skipDocker)) {
		 echo "Skipped"
	} else {
		/*
		withEnv(['DOCKER_HOST=tcp://gemini.office:2375']) {
			sh "captain build"
			sh "captain push"
		}
		*/
	}
	
}

def version() {
	def matcher = readFile('pom.xml') =~ '<version>(\\d*)\\.(\\d*)\\.(\\d*)(-SNAPSHOT)*</version>'
	matcher ? matcher[0] : null
}
