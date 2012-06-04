package wp.reverts.articlenetwork;

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TLongDoubleMap;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.*;
import gnu.trove.procedure.TIntIntProcedure;
import wp.reverts.common.ArticleClusterReader;
import wp.reverts.common.Revert;
import wp.reverts.common.RevertGraph;
import wp.reverts.common.RevertReader;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

public class Builder {
    private RevertGraph graph;
    private PrintStream out;
    private int maxArticlesPerUser = 200;
    private TIntIntMap articleClusters;

    public Builder(RevertGraph graph, PrintStream out, TIntIntMap articleClusters) {
        this.graph = graph;
        this.out = out;
        this.articleClusters = articleClusters;
    }

    public Map<Integer, TIntIntMap> getUserArticleCounts(boolean truncate) {
        Map<Integer, TIntIntMap> counts = new HashMap<Integer, TIntIntMap>();
        for (Revert r : graph.getGraph().edgeSet()) {
            int aid = r.getArticle().getId();
            for (int uid : new int[] { r.getRevertedUser().getId(), r.getRevertedUser().getId()}) {
                if (!counts.containsKey(uid)) {
                    counts.put(uid, new TIntIntHashMap());
                }
                counts.get(uid).adjustOrPutValue(aid, 1, 1);
            }
        }
        if (!truncate) {
            return counts;
        }

        long total = 0;
        long truncated = 0;
        for (int uid : counts.keySet()) {
            TIntIntMap ucounts = counts.get(uid);
            int threshold = 0;
            if (counts.size() > maxArticlesPerUser) {
                int [] values = ucounts.values();
                Arrays.sort(values);
                threshold = values[values.length - maxArticlesPerUser];
            }
            final int finalThreshold = threshold;
            ucounts.retainEntries(new TIntIntProcedure() {
                public boolean execute(int key, int val) {
                    return val >= finalThreshold;
                }
            });
            total += ucounts.size() * ucounts.size();
        }

        out.println("truncated " + truncated + " users at " + maxArticlesPerUser);
        out.println("found " + total + " coocurrence pairs");
        return counts;
    }

    public void exploreDegrees(TLongIntMap adjacencies) {
        out.println("calculating degree distribution...");
        TIntIntMap degrees = new TIntIntHashMap();
        for (long pair : adjacencies.keys()) {
            int a1 = unpackX(pair);
            int a2 = unpackY(pair);
            degrees.adjustOrPutValue(a1, 1, 1);
            degrees.adjustOrPutValue(a2, 1, 1);
        }
        TIntIntMap degreeCounts = new TIntIntHashMap();
        int maxDegree = 0;
        for (int d : degrees.values()) {
            int x = degreeCounts.adjustOrPutValue(d, 1, 1);
            maxDegree = Math.max(x, maxDegree);
        }
        for (int d = 1; d <= maxDegree; d++) {
            if (degreeCounts.containsKey(d)) {
                out.println("degree " + d + ": " + degreeCounts.get(d) + " edges");
            }
        }
    }

    public void summarizeGraph() {
        Map<Integer, TIntIntMap> userArticleCounts = getUserArticleCounts(true);
        TLongIntMap adjacencies = buildArticleAdjacencies(userArticleCounts);
        exploreDegrees(adjacencies);
        exploreWeights(adjacencies);
        double q = modularity(adjacencies);
        out.println("modularity is " + q);
    }

    private TLongIntMap buildArticleAdjacencies(Map<Integer, TIntIntMap> userArticleCounts) {
        TLongIntMap adjacencies = new TLongIntHashMap();
        int i = 0;
        for (TIntIntMap articles : userArticleCounts.values()) {
            if (i++ % 1000 == 0) {
                out.println("doing user " + i + " of " + userArticleCounts.size());
            }
            int [] articleArray = articles.values();
            for (int aid : articleArray) {
                int n1 = articles.get(aid);
                for (int aid2 : articleArray) {
                    int n2 = articles.get(aid2);
                    adjacencies.adjustOrPutValue(pack(aid, aid2), 1, 1);
                }
            }
        }

        out.println("found " + adjacencies.size() + " unique coocurrence pairs");
        return adjacencies;
    }

    private void exploreWeights(TLongIntMap adjacencies) {
        out.println("calculating weights...");
        TIntIntMap weights = new TIntIntHashMap();
        int maxWeight = 0;
        for (int w : adjacencies.values()) {
            int x = weights.adjustOrPutValue(w, 1, 1);
            maxWeight = Math.max(x, maxWeight);
        }
        for (int w = 1; w <= maxWeight; w++) {
            if (weights.containsKey(w)) {
                out.println("weight " + w + ": " + weights.get(w) + " edges");
            }
        }
    }

    public double modularity(TLongIntMap adjacencies) {
        double totalWeight = 0;
        for (int v : adjacencies.values()) {
            totalWeight += v;
        }

        int missing = 0;
        TIntDoubleMap clusterWeightSums = new TIntDoubleHashMap();
        TLongDoubleMap clusterSelfWeights = new TLongDoubleHashMap();
        for (long packed : adjacencies.keys()) {
            int aid1 = unpackX(packed);
            int aid2 = unpackY(packed);
            int w = adjacencies.get(packed);
            if (articleClusters.containsKey(aid1) && articleClusters.containsKey(aid2)) {
                int cid1 = articleClusters.get(aid1);
                int cid2 = articleClusters.get(aid2);
                clusterWeightSums.adjustOrPutValue(cid1, w, w);
                clusterWeightSums.adjustOrPutValue(cid2, w, w);
                if (cid1 == cid2) {
                    clusterSelfWeights.adjustOrPutValue(cid1, w, w);
                }
            } else {
                missing++;
            }
        }

        double Q = 0.0;
        for (int c : clusterWeightSums.keys()) {
            double sumWeights = clusterWeightSums.get(c);
            double selfWeight = clusterSelfWeights.containsKey(c) ? clusterSelfWeights.get(c) : 0.0;
            Q += selfWeight - sumWeights * sumWeights;
        }

        return Q;
    }

    public static long pack(int x, int y) {
        long xPacked = ((long)x) << 32;
        long yPacked = y & 0xFFFFFFFFL;
        return xPacked | yPacked;
    }

    public static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    public static int unpackY(long packed) {
        return (int) (packed & 0xFFFFFFFFL);
    }

    public void summarizeUserCounts() {
        final Map<Integer, Integer> counts = new HashMap<Integer, Integer>();
        TIntObjectHashMap<String> names = new TIntObjectHashMap<String>();
        for (Revert r : graph.getGraph().edgeSet()) {
            int id1 = r.getRevertedUser().getId();
            int id2 = r.getRevertingUser().getId();
            names.put(id1, r.getRevertedUser().getName());
            names.put(id2, r.getRevertingUser().getName());
            counts.put(id1, 1 + (counts.containsKey(id1) ? counts.get(id1) : 0));
            counts.put(id2, 1 + (counts.containsKey(id2) ? counts.get(id2) : 0));
        }
        out.println("found " + counts.size() + " users");
        List<Integer> userIds = new ArrayList<Integer>(counts.keySet());
        Collections.sort(userIds, new Comparator<Integer>() {
            public int compare(Integer uid1, Integer uid2) {
                return -1 * (counts.get(uid1) - counts.get(uid2));
            }
        });

        for (double p : new double [] { 0.1, 0.5, 1, 2, 5, 10, 20, 30, 40, 50}) {
            double f = p / 100.0;
            int i = (int) (f * counts.size());
            int n = counts.get(userIds.get(i));
            out.println("top " + p + "% with rank " + i + " is " + n);
        }
        out.println("");

        for (int i = 0; i < 100; i++) {
            int userId = userIds.get(i);
            String name = names.get(userId);
            out.println("" + (i+1) + ". " + userId + "@" + name + " = " + counts.get(userId));
        }
    }

    public static void main(String args[]) throws IOException {
        long start = System.currentTimeMillis();
        RevertReader rr = new RevertReader(new File(args[0]));
        System.err.println("reading reverts from " + args[0]);
        RevertGraph g = new RevertGraph(rr);
        System.err.println("statistics on entire graph:");
        TIntIntMap articleClusters = new ArticleClusterReader().read(new File(args[1]));
        Builder b = new Builder(g, System.out, articleClusters);
        b.summarizeUserCounts();
        b.summarizeGraph();
        long elapsed = System.currentTimeMillis() - start;
        System.err.println("elapsed time is " + (elapsed / 1000.0));

    }
}
