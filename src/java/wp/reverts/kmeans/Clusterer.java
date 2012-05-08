package wp.reverts.kmeans;

import gnu.trove.map.hash.TIntIntHashMap;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;

/**
 * Scalable K-Means ++ algorithm
 */
public class Clusterer {
    private final List<DocumentReader> readers = new ArrayList<DocumentReader>();
    private final List<Cluster> clusters = new ArrayList<Cluster>();

    private final int numClusters;
    private final int numThreads;
    ExecutorService threadPool;
    ExecutorCompletionService completionPool;

    public Clusterer(List<File> inputPaths, int numClusters, int numThreads) {
        this.numClusters = numClusters;
        this.numThreads = numThreads;
        this.threadPool = Executors.newFixedThreadPool(numThreads);
        this.completionPool = new ExecutorCompletionService(threadPool);
        for (File path : inputPaths) {
            readers.add(new DocumentReader(path));
        }
    }

    public class InitialRunnable implements Callable {
        private DocumentReader reader;
        private int id;

        public InitialRunnable(int id, DocumentReader reader) {
            this.id = id;
            this.reader = reader;
        }

        public Object call() {
            int n = numClusters / readers.size();
            if (id < numClusters % readers.size()) {
                n++;    // leftovers
            }
            List<Cluster> newClusters = new ArrayList<Cluster>();

            Random rand = new Random();

            for (Document d : reader) {
                if (newClusters.size() < n) {
                    newClusters.add(new Cluster(clusters.size(), d));
                } else {
                    newClusters.get(rand.nextInt(n)).addDocument(d, 1.0);
                }
            }

            synchronized (clusters) {
                for (Cluster c : newClusters) {
                    c.setId(clusters.size());
                    clusters.add(c);
                }
            }

            return "finished";
        }
    }

    public void initialPass() throws InterruptedException {
        int i = 0;
        for (DocumentReader r : readers) {
            completionPool.submit(new InitialRunnable(i++, r));
        }
        long start = System.currentTimeMillis();
        i = 0;
        for (DocumentReader r : readers) {
            i++;
            completionPool.take();
            if (i % 100 == 0 || i == readers.size()) {
                double mean = (System.currentTimeMillis() - start) / 1000.0 / i;
                System.err.println("mean completion time for " + i + " of " + readers.size() + " is " + mean + " seconds");
            }
        }
        finalizeClusters();
    }

    public ClusterScore findClosest(final Document d) {
        ClusterScore cs = new ClusterScore();
        for (Cluster c : clusters) {
            double s = c.getSimilarity(d);
            if (s >= cs.score) {
                cs.score = s;
                cs.cluster = c;
            }
        }
        return cs;
    }

    public class IterationRunnable implements Callable {
        private DocumentReader reader;
        private int id;

        public IterationRunnable(int id, DocumentReader reader) {
            this.id = id;
            this.reader = reader;
        }

        public Object call() {
            double sum = 0.0;
            int i = 0;

            for (Document d : reader) {
                ClusterScore cs = findClosest(d);

                // check if we should create a new cluster with this
                if (cs.score > 0.0) {
                    synchronized (cs.cluster) {
                        cs.cluster.addDocument(d, cs.score);
                    }
                }
                sum += cs.score;
                i++;
            }

            return new Double(sum);
        }
    }

    public void finalizeClusters() {
        ClusterStats overall = new ClusterStats(0);
        for (Cluster c : clusters) {
            c.finalize();
            c.getStats().debug();
            overall.merge(c.getStats());
        }
        System.err.println("============OVERALL STATS================");
        overall.debug();
        System.err.println("");
    }

    public void iteration() throws InterruptedException, ExecutionException {
        double sum = 0;
        int i = 0;
        for (DocumentReader r : readers) {
            completionPool.submit(new IterationRunnable(i++, r));
        }
        long start = System.currentTimeMillis();
        i = 0;
        for (DocumentReader r : readers) {
            i++;
            sum += (Double)(completionPool.take().get());
            if (i % 100 == 0 || i == readers.size()) {
                double mean = (System.currentTimeMillis() - start) / 1000.0 / i;
                System.err.println("mean completion time for " + i + " of " + readers.size() + " is " + mean + " seconds");
            }
        }
        finalizeClusters();
    }

    public void shutdown() {
        threadPool.shutdownNow();
    }

    static class ClusterScore {
        Cluster cluster = null;
        double score = 0.0;
    }

    public static void main(String args[]) throws InterruptedException, ExecutionException {
        if (args.length < 3) {
            System.err.println("usage: Clusterer num_clusters num_threads input paths...");
            System.exit(1);
        }
        List<File> paths = new ArrayList<File>();
        for (int i = 2; i < args.length; i++) {
            paths.add(new File(args[i]));
        }
        Clusterer c = new Clusterer(paths, Integer.valueOf(args[0]), Integer.valueOf(args[1]));
        c.initialPass();
        for (int i = 0; i < 10; i++) {
            System.err.println("starting iteration " + i);
            c.iteration();
        }
        c.shutdown();
    }
}
