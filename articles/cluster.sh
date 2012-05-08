# cat articles/translate/*.gz >./doc_sims.txt.gz

mkdir -p bin

CP=bin/:lib/trove-3.0.2.jar
javac -cp $CP -sourcepath src/java/ -d bin/ src/java/wp/reverts/kmeans/*.java &&
java -server -Xmx20000M -cp $CP wp.reverts.kmeans.Clusterer 10 1500 ./articles/translated/*.gz
