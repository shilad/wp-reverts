package wp.reverts.kmeans;


import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.map.hash.TIntShortHashMap;

import java.util.Arrays;

public class Cluster {
    public static int NUM_FAKE_DOCUMENTS = 10;

    FeatureList features = new FeatureList(new TIntArrayList(), new TFloatArrayList());

    TIntFloatHashMap sums = new TIntFloatHashMap();
    TIntArrayList docIds = new TIntArrayList();
    private int id;
    private ClusterStats stats = null;
    private ClusterStats nextStats;

    public Cluster(int id, Document d) {
        this.id = id;
        this.nextStats = new ClusterStats(this.id, sums.keySet());
        this.addDocument(d, 1.0);
        this.finalize();
    }

    public void addDocument(Document d, double sim) {
        d.setCluster(id);
        docIds.add(d.getId());
        for (int i = 0; i < d.getFeatures().getSize(); i++) {
            int featureId = d.getFeatures().getId(i);
            float featureValue = d.getFeatures().getValue(i);
            sums.adjustOrPutValue(featureId, featureValue, featureValue);
        }
        nextStats.addDocument(d, sim);
    }

    public void finalize() {
        int ids[] = sums.keys();
        Arrays.sort(ids);
        float values [] = new float [ids.length];
        for (int i = 0; i < ids.length; i++) {
            int id = ids[i];
            values[i] = sums.get(id) / (docIds.size() + NUM_FAKE_DOCUMENTS);
        }
        this.features = new FeatureList(ids, values);
//        this.features.normalize();
        this.sums.clear();
        this.docIds.clear();
        this.stats = nextStats;
        this.nextStats = new ClusterStats(this.id, sums.keySet());
    }

    public double getSimilarity(Document d) {
        return features.dot(d.getFeatures());
    }

    public ClusterStats getStats() {
        return stats;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
