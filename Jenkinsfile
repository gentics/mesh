stage 'Test'
def splits = splitTests parallelism: [$class: 'CountDrivenParallelism', size: 16], generateInclusions: true
def branches = [:]
for (int i = 0; i < splits.size(); i++) {
  def split = splits[i]
  branches["split${i}"] = {
    node('dockerSlave') {
      sh 'rm -rf *'
      writeFile file: (split.includes ? 'inclusions.txt' : 'exclusions.txt'), text: split.list.join("\n")
      writeFile file: (split.includes ? 'exclusions.txt' : 'inclusions.txt'), text: ''
      sh 'cat inclusions.txt'
      sh 'cat exclusions.txt'
    }
  }
}
parallel branches

