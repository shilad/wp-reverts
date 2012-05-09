package wp.reverts.kmeans;

import gnu.trove.list.TFloatList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReverseIndex {
    private List<Cluster> clusters;
    private List<Double> clusterLengths = new ArrayList<Double>();
    Map<Integer, FeatureClusters> reverseIndex = new HashMap<Integer, FeatureClusters>();



    public ReverseIndex(List<Cluster> clusters) {
        this.clusters = clusters;
    }

    public void finalizeToIndex() {
        reverseIndex.clear();
        clusterLengths.clear();
        for (Cluster c : clusters) {
            c.finalize();
            FeatureList fl = c.getFeatures();
            clusterLengths.add(fl.getLength());
            for (int i = 0; i < fl.getSize(); i++) {
                int id = fl.getId(i);
                float value = fl.getValue(i);
                if (!reverseIndex.containsKey(id)) {
                    reverseIndex.put(id, new FeatureClusters());
                }
                FeatureClusters fc = reverseIndex.get(id);
                fc.clusters.add(c.getId());
                fc.values.add(value);
            }
        }
    }

    public double [] cosineSim(Document d) {
        double sims[] = new double[clusters.size()];
        for (int i = 0; i < d.getFeatures().getSize(); i++) {
            int id = d.getFeatures().getId(i);
            float val = d.getFeatures().getValue(i);
            FeatureClusters fc = reverseIndex.get(id);
            if (fc != null) {
                for (int j = 0; j < fc.clusters.size(); j++) {
                    sims[fc.clusters.get(j)] += fc.values.get(j) * val;
                }
            }
        }

        for (int i = 0; i < clusters.size(); i++) {
            if (clusterLengths.get(i) > 0) {
                sims[i] /= (clusterLengths.get(i) * d.getFeatures().getLength());
            }
        }
        return sims;
    }

    static class FeatureClusters {
        TIntList clusters = new TIntArrayList();
        TFloatList values = new TFloatArrayList();
    }
}
