package wp.reverts.kmeans;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

public class ClusterStats {
    private int numDocuments = 0;
    private int numNonZero = 0;
    private double simSum = 0.0;
    private TIntSet featureIds = new TIntHashSet();
    private int totalDistinctFeatures = 0;

    private int id;

    public ClusterStats(int id) {
        this.id = id;
    }

    public ClusterStats(int id, TIntSet featureIds) {
        this.id = id;
        this.featureIds = featureIds;
    }

    public void addDocument(Document d, double sim) {
        // feature ids will implicitly be updated
        // because its assumed to be passed-in
        numDocuments++;
        numNonZero += d.getFeatures().getSize();
        simSum += sim;
    }

    public void merge(ClusterStats other) {
        featureIds.addAll(other.featureIds);
        numDocuments += other.numDocuments;
        numNonZero += other.numNonZero;
        simSum += other.simSum;
        totalDistinctFeatures += other.featureIds.size();
    }

    public void debug() {
        System.err.println("information for cluster " + id);
        System.err.println("\tnumDocs: " + numDocuments);
        if (numDocuments > 0) {
            System.err.println("\tnumFeatures: " + featureIds.size());
            System.err.println("\tnumTotalFeatures: " + totalDistinctFeatures);
            System.err.println("\tnumNonZero: " + numNonZero);
            System.err.println("\tmeanSim: " + simSum / numDocuments);
        }
    }
}
