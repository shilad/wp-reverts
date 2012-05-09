package wp.reverts.kmeans;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

public class ClusterIteration implements Callable {
    private DocumentReader reader;
    private int id;
    private List<Cluster> clusters;

    public ClusterIteration(int id, DocumentReader reader, List<Cluster> clusters) {
        this.id = id;
        this.reader = reader;
        this.clusters = clusters;
    }

    public Object call() {
        double sum = 0.0;
        int i = 0;

        for (Document d : reader) {
            if (!isUsefulDocument(d)) {
                continue;
            }
            d.getFeatures().smartNormalize();
            ClusterScore cs = findClosest(d);

            // check if we should create a new cluster with this
            if (cs.score > 0.0) {
                synchronized (cs.cluster) {
                    cs.cluster.addDocument(d, cs.score);
                }
            }
            sum += cs.score;
            i++;
        }

        return new Double(sum);
    }

    public boolean isUsefulDocument(Document d) {
        return (d.getFeatures().getSize() > 20);
    }

    public ClusterScore findClosest(final Document d) {
        ClusterScore cs = new ClusterScore();
        for (Cluster c : clusters) {
            double s = c.getCosineSimilarity(d, false);
            if (s >= cs.score) {
                cs.score = s;
                cs.cluster = c;
            }
        }
        return cs;
    }
}
