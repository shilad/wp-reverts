package wp.reverts.common;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.util.UnionFind;


public class Arboricity {

    public int calculate(DirectedGraph<User, Revert> g) {
        UnionFind<User> forest = new UnionFind<User>(g.vertexSet());
        int numMerges = 0;

        for (Revert r : g.edgeSet()) {
            // Find representives for each user's tree
            User representative1 = forest.find(r.getRevertedUser());
            User representative2 = forest.find(r.getRevertingUser());

            // we can safely merge different trees without creating a cycle
            if (!representative1.equals(representative2)) {
                forest.union(r.getRevertedUser(), r.getRevertingUser());
                numMerges++;
            }
        }

        // number of trees (edge merge reduces # by 1)
        return g.edgeSet().size() - numMerges;
    }
}
