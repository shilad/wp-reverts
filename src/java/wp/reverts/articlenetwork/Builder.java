package wp.reverts.articlenetwork;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import wp.reverts.common.Revert;
import wp.reverts.common.RevertGraph;
import wp.reverts.common.RevertReader;

import java.io.File;
import java.io.PrintStream;
import java.util.*;

public class Builder {
    private RevertGraph graph;
    private PrintStream out;
    private int maxArticlesPerUser = 10;

    public Builder(RevertGraph graph, PrintStream out) {
        this.graph = graph;
        this.out = out;
    }

    public void buildGraph() {
        Map<Integer, TIntIntMap> userCounts = new HashMap<Integer, TIntIntMap>();
        for (Revert r : graph.getGraph().edgeSet()) {
            int aid = r.getArticle().getId();
            for (int uid : new int[] { r.getRevertedUser().getId(), r.getRevertedUser().getId()}) {
                if (!userCounts.containsKey(uid)) {
                    userCounts.put(uid, new TIntIntHashMap());
                }
                userCounts.get(uid).adjustOrPutValue(aid, 1, 1);
            }
        }

        List<TIntList> userArticles = new ArrayList<TIntList>();
        long total = 0;
        for (int uid : userCounts.keySet()) {
            TIntIntMap counts = userCounts.get(uid);
            int threshold = 0;
            if (counts.size() > maxArticlesPerUser) {
                int [] values = counts.values();
                Arrays.sort(values);
                threshold = values[values.length - maxArticlesPerUser];
            }
            TIntList topArticles = new TIntArrayList();
            for (int aid : counts.keys()) {
                if (counts.get(aid) >= threshold) {
                    topArticles.add(aid);
                }
            }
            userArticles.add(topArticles);
            total += topArticles.size() * topArticles.size();
        }

        out.println("found " + total + " coocurrence pairs");

        TLongIntMap adjacencies = new TLongIntHashMap();
        for (TIntList articles : userArticles) {
            for (int aid : articles.toArray()) {
                for (int aid2 : articles.toArray()) {
                    adjacencies.adjustOrPutValue(pack(aid, aid2), 1, 1);
                }
            }
        }

        out.println("found " + adjacencies.size() + " unique coocurrence pairs");
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

    public static void main(String args[]) {
        long start = System.currentTimeMillis();
        RevertReader rr = new RevertReader(new File(args[0]));
        System.err.println("reading reverts from " + args[0]);
        RevertGraph g = new RevertGraph(rr);
        System.err.println("statistics on entire graph:");
        Builder b = new Builder(g, System.out);
        b.summarizeUserCounts();
        b.buildGraph();
        long elapsed = System.currentTimeMillis() - start;
        System.err.println("elapsed time is " + (elapsed / 1000.0));

    }
}
