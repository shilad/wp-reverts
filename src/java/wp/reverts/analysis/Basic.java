package wp.reverts.analysis;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import wp.reverts.common.*;

import java.io.File;
import java.util.Set;

public class Basic {
    private RevertGraph graph;

    public Basic(RevertGraph graph) {
        this.graph = graph;
    }

    public void counts() {
        TIntSet users = new TIntHashSet();
        TIntSet articles = new TIntHashSet();

        for (Revert r: graph.getGraph().edgeSet()) {
            users.add(r.getRevertedUser().getId());
            users.add(r.getRevertingUser().getId());
            articles.add(r.getArticle().getId());
        }

        System.out.println("number of reverts is " + graph.getGraph().edgeSet().size());
        System.out.println("number of users is " + users.size());
        System.out.println("number of articles is " + articles.size());

    }

    public void analyzeComponents() {
        System.out.println("arboricity is " + new Arboricity().calculate(graph.getGraph()));
        TIntList sizes = new TIntArrayList();
        for (Set<User> component : graph.getConnectedComponents()) {
            sizes.add(component.size());
        }
        sizes.sort();
        System.out.println("components:");
        System.out.println("\tdiscovered " + sizes.size() + " components");
        System.out.println("\tmaximum size: " + sizes.get(sizes.size()-1));
        for (int p : new int[] {10, 20, 30, 40, 50, 60, 70, 80, 90, 95, 96, 97, 98, 99}) {
            int i = p * sizes.size() / 100;
            System.out.println("\tpercentile " + p + ": " + sizes.get(i));
        }
        System.err.println("largest components:");
        for (int i = 0; i < 50; i++) {
            System.out.println("\tcomponent #" + (i+1) + ": " + sizes.get(sizes.size() - i - 1));
        }
    }

    public static void main(String args[]) {
        long start = System.currentTimeMillis();
        RevertReader rr = new RevertReader(new File(args[0]));
        System.err.println("reading reverts from " + args[0]);
        RevertGraph g = new RevertGraph(rr);
        System.err.println("statistics on entire graph:");
        Basic b = new Basic(g);
        b.counts();
        b.analyzeComponents();
        System.err.println("\n\n\nstatistics on graph with edge threshold 2:");
        b = new Basic(g.maskEdgesUnderWeight(2.01));
        b.counts();
        b.analyzeComponents();
        long elapsed = System.currentTimeMillis() - start;
        System.err.println("elapsed time is " + (elapsed / 1000.0));

    }
}
