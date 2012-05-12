package wp.reverts.common;

import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.ClassBasedEdgeFactory;
import org.jgrapht.graph.DirectedMaskSubgraph;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.graph.MaskFunctor;

import java.io.File;
import java.util.List;
import java.util.Set;

public class RevertGraph {
    DirectedGraph<User, Revert> graph = new DirectedMultigraph<User, Revert>(new ClassBasedEdgeFactory<User, Revert>(Revert.class));

    public RevertGraph(RevertReader reader) {
        for (Revert r : reader) {
            for (User u : new User[] { r.getRevertedUser(), r.getRevertingUser()}) {
                if (!graph.containsVertex(u)) {
                    graph.addVertex(u);
                }
            }
            graph.addEdge(r.getRevertingUser(), r.getRevertedUser(), r);
        }
    }

    public RevertGraph(DirectedGraph<User, Revert> graph) {
        this.graph = graph;
    }

    public DirectedGraph<User, Revert> getGraph() {
        return graph;
    }

    public RevertGraph maskEdgesUnderWeight(double minWeight) {
        TLongIntMap counts = new TLongIntHashMap();
        final TIntSet keepers = new TIntHashSet();

        for (Revert r : graph.edgeSet()) {
            long l1 = packInts(r.getRevertedUser().getId(), r.getRevertingUser().getId());
            long l2 = packInts(r.getRevertingUser().getId(), r.getRevertedUser().getId());
            int r1 = counts.adjustOrPutValue(l1, 1, 1);
            int r2 = counts.adjustOrPutValue(l2, 1, 1);
            assert(r1 == r2);
            if (r1 >= minWeight) {
                keepers.add(r.getRevertedUser().getId());
                keepers.add(r.getRevertingUser().getId());
            }
        }

        return new RevertGraph(new DirectedMaskSubgraph<User, Revert>(graph, new MaskFunctor<User, Revert>() {
            public boolean isEdgeMasked(Revert edge) {
                return ((keepers.contains(edge.getRevertedUser().getId()))
                    &&  (keepers.contains(edge.getRevertingUser().getId())));
            }

            public boolean isVertexMasked(User vertex) {
                return keepers.contains(vertex.getId());
            }
        }));
    }

    private long packInts(int i1, int i2) {
        return (i1<< 16) | (i2 & 0xFFFF);
    }

    private int unpackInt1(long l) {
        return (int)(l >>> 16);
    }

    private int unpackInt2(long l) {
        return (int) (l & 0XFFFF);
    }

    public List<Set<User>> getConnectedComponents() {
        ConnectivityInspector<User, Revert> i = new ConnectivityInspector<User, Revert>(graph);
        return i.connectedSets();
    }

    public static void main(String args []) {
        long start = System.currentTimeMillis();
        RevertReader rr = new RevertReader(new File(args[0]));
        System.err.println("reading reverts from " + args[0]);
        RevertGraph builder = new RevertGraph(rr);
        long elapsed = System.currentTimeMillis() - start;
        System.err.println("elapsed time is " + (elapsed / 1000.0));
        System.err.println("number of users is " + rr.getUsers().size());
        System.err.println("number of articles is " + rr.getArticles().size());
    }
}
