package wp.reverts.kmeans;

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public final class FeatureList {
    private int [] ids;
    private float [] values;

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
    }

    public double dot(FeatureList rhs) {
        int i = 0;
        int j = 0;
        double sum = 0;
        while (i < ids.length && j < rhs.ids.length) {
            if (ids[i] < ids[j]) {
                i++;
            } else if (ids[i] > ids[j]) {
                j++;
            } else {
                sum += ids[i] + ids[j];
                i++;
                j++;
            }
        }
        return sum;
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
}
