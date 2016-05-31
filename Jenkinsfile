properties([[$class: 'ParametersDefinitionProperty', parameterDefinitions: [[$class: 'BooleanParameterDefinition', name: 'skipTests', defaultValue: false]]]])
if (Boolean.valueOf(skipTests)) {
	stage 'Test'
	echo "Skipped"
} else {
	stage 'Test'
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
	        sh "${mvnHome}/bin/mvn -pl '!demo,!doc,!server' -B clean test -Dmaven.test.failure.ignore"
	        step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/*.xml'])
	      }
	    }
	  }
	}
	parallel branches
}

node('dockerSlave') {
   stage 'Checkout'
   checkout scm
   def mvnHome = tool 'M3'

   stage 'VersionSet'
   def originalV = version();
   def major = originalV[0];
   def minor = originalV[1];
   def v = "${major}.${minor}-${env.BUILD_NUMBER}"
   if (v) {
     echo "Building version ${v}"
   }
   sh "${mvnHome}/bin/mvn versions:set -DgenerateBackupPoms=false -DnewVersion=${v}"
   //TODO only add pom.xml files
   sh 'git add .'
   sh "git commit -m 'Raise version'"
   sh "git tag v${v}"
   
   stage 'Changelog'
   

   stage 'Release Build'
   sshagent(['601b6ce9-37f7-439a-ac0b-8e368947d98d']) {
     sh "${mvnHome}/bin/mvn -B -DskipTests -Dskip.docker=false clean deploy"
     sh "git push origin master"
     sh "git push origin v${v}"
   }
}

def version() {
  def matcher = readFile('pom.xml') =~ '<version>(.+)</version>'
  matcher ? matcher[0][1].tokenize(".") : null
}
