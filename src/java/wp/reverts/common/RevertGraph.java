package wp.reverts.common;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.ClassBasedEdgeFactory;
import org.jgrapht.graph.DirectedMultigraph;

import java.io.File;
import java.util.List;
import java.util.Set;

public class RevertGraph {
    DirectedMultigraph<User, Revert> graph = new DirectedMultigraph<User, Revert>(new ClassBasedEdgeFactory<User, Revert>(Revert.class));

    public void build(RevertReader reader) {
        for (Revert r : reader) {
            for (User u : new User[] { r.getRevertedUser(), r.getRevertingUser()}) {
                if (!graph.containsVertex(u)) {
                     graph.addVertex(u);
                }
            }
            graph.addEdge(r.getRevertingUser(), r.getRevertedUser(), r);
        }
    }

    public List<Set<User>> getConnectedComponents() {
        ConnectivityInspector<User, Revert> i = new ConnectivityInspector<User, Revert>(graph);
        return i.connectedSets();
    }

    public static void main(String args []) {
        long start = System.currentTimeMillis();
        RevertReader rr = new RevertReader(new File(args[0]));
        System.err.println("reading reverts from " + args[0]);
        RevertGraph builder = new RevertGraph();
        builder.build(rr);
        long elapsed = System.currentTimeMillis() - start;
        System.err.println("elapsed time is " + (elapsed / 1000.0));
        System.err.println("number of users is " + rr.getUsers().size());
        System.err.println("number of articles is " + rr.getArticles().size());
    }
}
