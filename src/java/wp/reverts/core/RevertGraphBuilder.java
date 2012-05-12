package wp.reverts.core;

import org.jgrapht.graph.ClassBasedEdgeFactory;
import org.jgrapht.graph.DirectedMultigraph;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RevertGraphBuilder {
    DirectedMultigraph<User, Revert> graph = new DirectedMultigraph<User, Revert>(new ClassBasedEdgeFactory<User, Revert>(Revert.class));

    public void build(RevertReader reader) {
        for (Revert r : reader) {
            graph.addEdge(r.getRevertingUser(), r.getRevertedUser(), r);
        }
    }

    public static void main(String args []) {
        long start = System.currentTimeMillis();
        RevertReader rr = new RevertReader(new File(args[0]));
        System.err.println("reading reverts from " + args[0]);
        RevertGraphBuilder builder = new RevertGraphBuilder();
        builder.build(rr);
        long elapsed = System.currentTimeMillis() - start;
        System.err.println("elapsed time is " + (elapsed / 1000.0));
        System.err.println("number of users is " + rr.getUsers().size());
        System.err.println("number of articles is " + rr.getArticles().size());
    }
}
