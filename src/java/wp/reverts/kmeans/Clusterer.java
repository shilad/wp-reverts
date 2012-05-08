package wp.reverts.kmeans;

import gnu.trove.map.hash.TIntIntHashMap;

import java.io.File;
import java.util.*;

/**
 * Scalable K-Means ++ algorithm
 */
public class Clusterer {
    File inputPath;
    DocumentReader reader;
    List<Cluster> clusters = new ArrayList<Cluster>();

    private int numClusters;

    public Clusterer(File inputPath, int numClusters) {
        this.inputPath = inputPath;
        this.numClusters = numClusters;
        reader = new DocumentReader(inputPath);
    }

    public void initialPass() {
        double sum = 0.0;
        int i = 0;
        List<Document> initial = new ArrayList<Document>();
        TIntIntHashMap docIdsToIndexes = new TIntIntHashMap ();

        Random rand = new Random();

        // pass 1; pick a random document as initial cluster
        for (Document d : reader) {
            docIdsToIndexes.put(d.getId(), i);
            if (initial.size() < numClusters) {
                initial.add(d);
            } else if (rand.nextDouble() < 1.0 * numClusters / (i+1)) {
                initial.set(rand.nextInt(initial.size()), d);
            }
            i++;
        }

        for (Document d : initial) {
            System.err.println("cluster " + clusters.size() + " intialized to doc #" + docIdsToIndexes.get(d.getId()) + " id=" + d.getId());
            clusters.add(new Cluster(clusters.size(), d));
        }
    }

    public ClusterScore findClosest(Document d, boolean working) {
        ClusterScore cs = new ClusterScore();
        for (Cluster c : clusters) {
            double s = c.getSimilarity(d, working);
            if (s > cs.score) {
                cs.score = s;
                cs.cluster = c;
            }
        }
        return cs;
    }

    public void iteration() {
        int i = 0;
        double sum = 0.0;
        for (Document d : reader) {
            d.getFeatures().normalize();
            ClusterScore cs = findClosest(d, false);

            // check if we should create a new cluster with this
            if (cs.score > 0.0) {
                cs.cluster.addDocument(d);
            }
            sum += cs.score;
        }
        System.err.println("mean sim is " + (sum / i));

        for (Cluster c : clusters) {
            c.finalize();
        }
    }

    static class ClusterScore {
        Cluster cluster = null;
        double score = 0.0;
    }

    public static void main(String args[]) {
        if (args.length != 2) {
            System.err.println("usage: Clusterer input_path num_clusters");
            System.exit(1);
        }
        Clusterer c = new Clusterer(new File(args[0]), Integer.valueOf(args[1]));
        c.initialPass();
        for (int i = 0; i < 10; i++) {
            c.iteration();
        }
    }
}
