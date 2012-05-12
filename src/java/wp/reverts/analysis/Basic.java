package wp.reverts.analysis;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import wp.reverts.common.RevertGraph;
import wp.reverts.common.RevertReader;
import wp.reverts.common.User;

import java.io.File;
import java.util.Set;

public class Basic {
    private RevertGraph graph;
    private RevertReader reader;

    public Basic(RevertReader reader, RevertGraph graph) {
        this.reader = reader;
        this.graph = graph;
    }

    public void counts() {
        System.out.println("number of users is " + reader.getUsers().size());
        System.out.println("number of articles is " + reader.getArticles().size());

    }

    public void analyzeComponents() {
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
        RevertGraph g = new RevertGraph();
        g.build(rr);
        Basic b = new Basic(rr, g);
        b.counts();
        b.analyzeComponents();
        long elapsed = System.currentTimeMillis() - start;
        System.err.println("elapsed time is " + (elapsed / 1000.0));

    }
}
