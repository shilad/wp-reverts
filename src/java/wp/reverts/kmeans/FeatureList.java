package wp.reverts.kmeans;

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import wp.reverts.core.Namer;

import java.util.*;

public final class FeatureList {
    private int [] ids;
    private float [] values;
    private double length = -1;

    public FeatureList(TIntArrayList ids, TFloatArrayList values) {
        this.ids = ids.toArray();
        this.values = values.toArray();
    }

    public FeatureList(int [] ids, float [] values) {
        for (int i = 0; i < ids.length-1; i++) {
            assert(ids[i] < ids[i+1]);
        }
        this.ids = ids;
        this.values = values;
    }

    public int getSize() {
        return ids.length;
    }

    public int getId(int i) {
        return ids[i];
    }

    public void truncate(int n) {
        if (ids.length > n) {
            ids = Arrays.copyOf(ids, n);
            values = Arrays.copyOf(values, n);
            length = -1;
        }
    }

    public float getValue(int i) {
        return values[i];
    }

    public void normalize() {
        double sum = 0;
        for (int i = 0; i < values.length; i++) {
            sum += values[i] * values[i];
        }
        double scale = Math.sqrt(sum);
        for (int i = 0; i < values.length; i++) {
            values[i] /= scale;
        }
        length = -1;
    }

    public double getLength() {
        if (length < 0) {
            double sum = 0;
            for (int i = 0; i < values.length; i++) {
                sum += values[i] * values[i];
            }
            length =Math.sqrt(sum);
        }
        return length;
    }

    public void smartNormalize() {
        if (values.length == 0) {
            return;
        }
        double sum = 0;
        for (int i = 0; i < values.length; i++) {
            sum += values[i];
        }
        double mean = sum / values.length;
        double dev = 0.0;
        for (int i = 0; i < values.length; i++) {
            dev += (values[i] - mean) * (values[i] - mean);
        }
        dev = Math.sqrt(dev / values.length);
        double min = 0.0;
        for (int i = 0; i < values.length; i++) {
            values[i] = (float) ((values[i] - mean) / dev);
            min = Math.min(min, values[i]);
        }
        // Have all the values start at 0.5 (penalize non-entries).
        for (int i = 0; i < values.length; i++) {
            values[i] = (float) (values[i] - min + 5.0);
            if (Float.isNaN(values[i])) {
                throw new IllegalStateException("in featurelist found NaN for id " + ids[i]);
            }
        }
        length = -1;
    }

    public double dot(FeatureList rhs) {
        int i = 0;
        int j = 0;
        double sum = 0;
        while (i < ids.length && j < rhs.ids.length) {
            if (ids[i] < rhs.ids[j]) {
                i++;
            } else if (ids[i] > rhs.ids[j]) {
                j++;
            } else {
                sum += values[i] + rhs.values[j];
                i++;
                j++;
            }
        }
        return sum;
    }

    public void adjust(IdfAdjuster adjuster) {
        for (int i = 0; i < ids.length; i++) {
            values[i] = adjuster.adjust(ids[i], values[i]);
        }
    }

    public double cosineSimilarity(FeatureList rhs) {
        if (rhs.getLength() == 0 || this.getLength() == 0) {
            return 0.0;
        }
        return dot(rhs) / (getLength() * rhs.getLength());
    }

    /**
     * Creates a feature list from a list of space separated id:value pairs
     * @param inputString
     */
    public static FeatureList parse(String inputString) {
        TIntArrayList ids = new TIntArrayList();
        TFloatArrayList values = new TFloatArrayList();
        for (String pair : inputString.trim().split(" ")) {
            int i = pair.indexOf(":");
            if (i < 0) {
                throw new RuntimeException("no colon in pair: " + pair);
            }
            int id = Integer.valueOf(pair.substring(0, i));
            float value = Float.valueOf(pair.substring(i+1));
            ids.add(id);
            values.add(value);
        }
        return new FeatureList(ids, values);
    }

    public void showTop(Namer namer, int n) {
        List<Integer> indexes = new ArrayList<Integer>();
        for (int i = 0; i < ids.length; i++) {
            indexes.add(i);
        }
        Collections.sort(indexes, new Comparator<Integer>() {
            public int compare(Integer i, Integer j) {
                if (values[i] < values[j]) {
                    return +1;
                } else if (values[i] > values[j]) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
        for (int i = 0; i < n && i < indexes.size(); i++) {
            int id = ids[indexes.get(i)];
            double v = values[indexes.get(i)];
            System.err.println("\t" + (i+1) + ". " + namer.getName(id) + " (" + v + ")");
        }
    }
}
