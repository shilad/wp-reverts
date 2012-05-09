package wp.reverts.kmeans;

import gnu.trove.map.hash.TIntIntHashMap;

public class IdfAdjuster {
    private TIntIntHashMap counts = new TIntIntHashMap();

    public IdfAdjuster() {
    }

    public void readDocs(DocumentReader reader) {
        TIntIntHashMap tmpCounts = new TIntIntHashMap();
        for (Document d: reader) {
            for (int i = 0; i < d.getFeatures().getSize(); i++) {
                int id = d.getFeatures().getId(i);
                tmpCounts.adjustOrPutValue(id, 1, 1);
            }
        }
        synchronized (counts) {
            for (int id : tmpCounts.keys()) {
                int n = tmpCounts.get(id);
                counts.adjustOrPutValue(id, n, n);
            }
        }
    }

    public float adjust(int id, float value) {
        if (counts.containsKey(id)) {
            return (float) (value / Math.log(counts.get(id) + 10));
        } else {
            return value;
        }
    }
}
