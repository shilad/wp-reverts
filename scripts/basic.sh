CP=bin/:`ls lib/*.jar | tr '\n' ':'`

javac -cp $CP -sourcepath src/java/ -d bin/ src/java/wp/reverts/*/*.java &&
java -server -Xmx24000M -cp $CP wp.reverts.analysis.Basic dat/filtered_reverts.txt
