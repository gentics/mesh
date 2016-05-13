stage 'Test'
def splits = splitTests parallelism: [$class: 'CountDrivenParallelism', size: 16], generateInclusions: true
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
        retry (3) {
            sh "${mvnHome}/bin/mvn -pl '!demo,!doc,!server' -B clean test -Dmaven.test.failure.ignore"
        }
        step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/*.xml'])
      }
    }
  }
}
parallel branches
