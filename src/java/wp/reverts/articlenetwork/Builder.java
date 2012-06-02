package wp.reverts.articlenetwork;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import wp.reverts.common.Revert;
import wp.reverts.common.RevertGraph;
import wp.reverts.common.RevertReader;

import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;

public class Builder {
    private RevertGraph graph;
    private PrintStream out;

    public Builder(RevertGraph graph, PrintStream out) {
        this.graph = graph;
        this.out = out;
    }

    public void summarizeUserCounts() {
        TIntIntMap counts = new TIntIntHashMap();
        for (Revert r : graph.getGraph().edgeSet()) {
            counts.adjustOrPutValue(r.getRevertedUser().getId(), 1, 1);
            counts.adjustOrPutValue(r.getRevertingUser().getId(), 1, 1);
        }
        out.println("found " + counts.size() + " users");
        int[] values = counts.values();
        Arrays.sort(values);

        for (double p : new double [] { 0.1, 0.5, 1, 2, 5, 10, 20, 30, 40, 50}) {
            double f = p / 100.0;
            int i = (int) ((1.0 - f ) * values.length);
            out.println("top " + p + "% with rank " + (values.length - i) + " is " + values[i]);
        }

        out.println("");
    }

    public static void main(String args[]) {
        long start = System.currentTimeMillis();
        RevertReader rr = new RevertReader(new File(args[0]));
        System.err.println("reading reverts from " + args[0]);
        RevertGraph g = new RevertGraph(rr);
        System.err.println("statistics on entire graph:");
        Builder b = new Builder(g, System.out);
        b.summarizeUserCounts();
        long elapsed = System.currentTimeMillis() - start;
        System.err.println("elapsed time is " + (elapsed / 1000.0));

    }
}
