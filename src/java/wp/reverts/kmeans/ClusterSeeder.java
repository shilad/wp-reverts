package wp.reverts.kmeans;

import java.util.*;
import java.util.concurrent.Callable;

public class ClusterSeeder implements Callable {
    private DocumentReader reader;
    private int id;
    private List<Cluster> current;
    private List<Cluster> currentSnapshot;
    private int k;
    private int totalK;

    public ClusterSeeder(int id, DocumentReader reader, List<Cluster> current, int k, int totalK) {
        this.id = id;
        this.reader = reader;
        this.current = current;
        this.k = k;
        this.totalK = totalK;
    }

    private void takeClusterSnapshot() {
        synchronized (current) {
            this.currentSnapshot = new ArrayList<Cluster>(current);
        }
    }

    static class DocInfo {
        Document d;
        ClusterScore cs;
    }
    public Object call() {
        takeClusterSnapshot();
        List<Cluster> newClusters = new ArrayList<Cluster>();

        Random rand = new Random();

        for (Document d : reader) {
            if (!isUsefulDocument(d)) {
                continue;
            }
            d.getFeatures().smartNormalize();
//                d.debug(namer, 100);
            if (newClusters.size() < k) {
                newClusters.add(new Cluster(current.size(), d));
            } else {
                newClusters.get(rand.nextInt(k)).addDocument(d, 1.0);
            }
        }
        synchronized (current) {
            for (Cluster c : newClusters) {
//                c.finalize();
                c.setId(current.size());
                current.add(c);
            }
        }
        return "finished";
    }

    public Object call1() {
        try {
        takeClusterSnapshot();
        Random rand = new Random();

        // Perform a canopy clustering to get the requisite number of clusters
        List<DocInfo> docs = new LinkedList<DocInfo>();

        for (Document d : reader) {
            if (isUsefulDocument(d)) {
                d.getFeatures().smartNormalize();
                DocInfo di = new DocInfo();
                di.d = d;
                di.cs = findClosest(d);
                docs.add(di);
            }
        }
        int numDocs = docs.size();

        // Step 1: assign things to other clusters when obvious
        List<Double> sims = new ArrayList<Double>();
        for (DocInfo di : docs) { sims.add(di.cs.score); }
        Collections.sort(sims);
        Collections.reverse(sims);

        // How many should we already have covered?
        assert(current.size() < totalK);
        int thresholdIndex = docs.size() * current.size() / totalK;
        double threshold = sims.get(thresholdIndex);

        for (Iterator<DocInfo> iter = docs.iterator(); iter.hasNext();) {
            DocInfo di = iter.next();
            if (threshold > 0 && di.cs.score >= threshold) {
                di.cs.cluster.addDocument(di.d, di.cs.score);
                iter.remove();
            }
        }

        // Step 2: create a new "canopy" for leftovers
        for (int i = 0; i < k; i++) {
            Collections.shuffle(docs);

            // Consider how much each candidate improves the clustering
            List<DocInfo> candidates = docs.subList(0, 10);
            Cluster top = null;
            double topSum = 0;
            for (DocInfo c : candidates) {
                Cluster cc = new Cluster(c.d.getId(), c.d);
                cc.setNumFake(1);
                cc.finalize();
                double sum = 0.0;
                for (DocInfo di : docs) {
                    double s = cc.getCosineSimilarity(di.d, false);
                    sum += Math.max(0, s - di.cs.score);
                }
                System.err.println("sum for candidate " + c.d.getId() + " is " + sum);
                if (top == null || sum > topSum) {
                    top = cc;
                }
            }

            // Look at what documents have been improved
            sims.clear();
            for (DocInfo di : docs) {
                double s = top.getCosineSimilarity(di.d, false);
                if (s > di.cs.score) {
                    di.cs.score = s;
                    di.cs.cluster = top;
                    sims.add(s);
                }
            }

            System.err.println("num improved is " + sims.size());
            if (sims.isEmpty()) {
                break;
            }

            // Suck some documents into the clustering
            Collections.sort(sims);
            Collections.reverse(sims);
            threshold =  sims.get(Math.min(sims.size() - 1, sims.size() / (k - i)));

            for (Iterator<DocInfo> iter = docs.iterator(); iter.hasNext();) {
                DocInfo di = iter.next();
                if (di.cs.cluster == top && di.cs.score > threshold) {
                    top.addDocument(di.d, di.cs.score);
                    iter.remove();
                }
            }
            System.err.println("num docs added is " + top.docIds.size() + " with threshold " + threshold);
            top.setNumFake(Cluster.NUM_FAKE_DOCUMENTS);
            top.finalize();

            synchronized (current) {
                top.setId(current.size());
                current.add(top);
                takeClusterSnapshot();
            }
        }
        } catch (RuntimeException e) {
            e.printStackTrace();
            System.err.println("executor failed:");
        }
        return "finished";
    }


    public ClusterScore findClosest(final Document d) {
        ClusterScore cs = new ClusterScore();
        for (Cluster c : currentSnapshot) {
            double s = c.getCosineSimilarity(d, false);
            System.err.println("score for " + c.getId() + " is " + s);
            if (s >= cs.score) {
                cs.score = s;
                cs.cluster = c;
            }
        }
        return cs;
    }

    public boolean isUsefulDocument(Document d) {
        return (d.getFeatures().getSize() > 20);
    }
}
