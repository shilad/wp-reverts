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

    public Cluster(int id) {
        this.id = id;
    }

    public Cluster(int id, Document d) {
        this.id = id;
        this.addDocument(d);
        this.finalize();
    }

    public void addDocument(Document d) {
        d.setCluster(id);
        docIds.add(d.getId());
        for (int i = 0; i < d.getFeatures().getSize(); i++) {
            int featureId = d.getFeatures().getId(i);
            float featureValue = d.getFeatures().getValue(i);
            sums.adjustOrPutValue(featureId, featureValue, featureValue);
        }
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
        this.sums.clear();
        this.docIds.clear();
    }

    public double getSimilarity(Document d, boolean useWorking) {
        double dot = 0;
        if (useWorking) {
            double sum = 0.0;
            for (int i = 0; i < d.getFeatures().getSize(); i++) {
                int id = d.getFeatures().getId(i);
                if (sums.containsKey(id)) {
                    double val = d.getFeatures().getValue(i);
                    sum += sums.get(id) / (docIds.size() + NUM_FAKE_DOCUMENTS);
                }
            }
            dot = sum;
        } else {
            dot = features.dot(d.getFeatures());
        }
        return 1.0 * dot / (d.getFeatures().getSize());
    }
}
