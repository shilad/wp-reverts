package wp.reverts.kmeans;

import gnu.trove.map.hash.TIntIntHashMap;

public class IdfAdjuster {
    private TIntIntHashMap counts = new TIntIntHashMap();

    public IdfAdjuster() {
    }

    public void readDocs(DocumentReader reader) {
        for (Document d: reader) {
            for (int i = 0; i < d.getFeatures().getSize(); i++) {
                int id = d.getFeatures().getId(i);
                counts.adjustOrPutValue(id, 1, 1);
            }
        }
    }

    public float adjust(int id, float value) {
        if (counts.containsKey(id)) {
            System.err.println("adjusting by " + Math.log(counts.get(id) + 10));
            return (float) (value / Math.log(counts.get(id) + 10));
        } else {
            System.err.println("not adjusting " + id);
            return value;
        }
    }
}
