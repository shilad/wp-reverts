# cat articles/translate/*.gz >./doc_sims.txt.gz

mkdir -p bin

CP=bin/:lib/trove-3.0.2.jar

for k in 10 25 50 100 200 500 1000 2000; do
    echo "===============DOING CLUSTERING $k==========================="
    javac -cp $CP -sourcepath src/java/ -d bin/ src/java/wp/reverts/*/*.java &&
    java -server -Xmx24000M -cp $CP wp.reverts.kmeans.Clusterer $k 20 articles/clusters.$k.txt ./articles/translated/*.gz
done
