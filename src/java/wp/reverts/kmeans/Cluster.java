package wp.reverts.kmeans;


import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntFloatHashMap;
import wp.reverts.common.Namer;

import java.util.Arrays;

public class Cluster {
    public static int NUM_FAKE_DOCUMENTS = 10;

    FeatureList features = new FeatureList(new TIntArrayList(), new TFloatArrayList());

    TIntFloatHashMap sums = new TIntFloatHashMap();
    TIntArrayList docIds = new TIntArrayList();

    private double workingLength = -1;
    private int id;
    private ClusterStats stats = null;
    private ClusterStats nextStats;
    private int numFake = NUM_FAKE_DOCUMENTS;

    public Cluster(int id, Document d) {
        this.id = id;
        this.nextStats = new ClusterStats(this.id, sums.keySet());
        this.addDocument(d, 1.0);
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
        workingLength = -1;
    }

    public void finalize() {
        int ids[] = sums.keys();
        Arrays.sort(ids);
        float values [] = new float [ids.length];
        for (int i = 0; i < ids.length; i++) {
            int id = ids[i];
//            System.err.print("mean with " + docIds.size() + " from " + (sums.get(id) / docIds.size()) + " to ");
            values[i] = sums.get(id) / (docIds.size() + numFake);
//            System.err.println("" + values[i]);
            if (Float.isNaN(values[i])) {
                throw new IllegalStateException("in cluster " + id + " found NaN for id " + ids[i]);
            }
        }
        this.features = new FeatureList(ids, values);
//        this.features.normalize();
        this.sums.clear();
        this.docIds.clear();
        this.stats = nextStats;
        this.nextStats = new ClusterStats(this.id, sums.keySet());
        workingLength = -1;
    }

    public FeatureList getFeatures() {
        return features;
    }

    public void setFeatures(FeatureList features) {
        this.features = features;
    }

    public double getWorkingLength() {
        if (workingLength < 0) {
            double sum = 0.0;
            for (float v : sums.values()) {
                sum += v*v;
            }
            workingLength = Math.sqrt(sum);
        }
        return workingLength;
    }

    public double getSimilarity(Document d, boolean useWorking) {
        if (useWorking) {
            double sum = 0.0;
            for (int i = 0; i < d.getFeatures().getSize(); i++) {
                int id = d.getFeatures().getId(i);
                if (sums.containsKey(id)) {
                    double val = d.getFeatures().getValue(i);
                    sum += sums.get(id) / (docIds.size() + numFake);
                }
            }
            return sum;
        } else {
            return features.dot(d.getFeatures());
        }
    }

    public double getCosineSimilarity(Document d, boolean useWorking) {
        if (useWorking) {
            double dot = getSimilarity(d, useWorking);
            return dot / (d.getFeatures().getLength() * getWorkingLength());
        } else {
            return features.cosineSimilarity(d.getFeatures());
        }
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

    public void setNumFake(int numFake) {
        this.numFake = numFake;
    }

    public void debug(Namer namer, int n) {
        System.err.println("top features for cluster" + id + ":");
        features.showTop(namer, n);
    }
}
