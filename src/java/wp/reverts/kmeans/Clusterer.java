package wp.reverts.kmeans;

import gnu.trove.map.hash.TIntIntHashMap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Scalable K-Means ++ algorithm
 */
public class Clusterer {
    private final List<DocumentReader> readers = new ArrayList<DocumentReader>();
    private final List<Cluster> clusters = new ArrayList<Cluster>();
    private final Namer namer;

    private final int numClusters;
    private final int numThreads;
    ExecutorService threadPool;
    ExecutorCompletionService completionPool;
    IdfAdjuster idfAdjuster = new IdfAdjuster();

    public Clusterer(List<File> inputPaths, int numClusters, int numThreads) throws IOException {
        this.namer = new Namer("dat/filtered_articles_to_ids.txt");
        this.numClusters = numClusters;
        this.numThreads = numThreads;
        this.threadPool = Executors.newFixedThreadPool(numThreads);
        this.completionPool = new ExecutorCompletionService(threadPool);
        for (File path : inputPaths) {
            DocumentReader dr = new DocumentReader(path);
            dr.setTruncation(200);
            dr.setIdfAdjuster(idfAdjuster);
            readers.add(dr);
        }
    }

    public void calculateIdf() {
        System.err.println("calculating idf...");
        for (DocumentReader reader : readers) {
            idfAdjuster.readDocs(reader);
        }
    }

    public void initialPass() throws InterruptedException {
        int i = 0;
        for (DocumentReader r : readers) {
            int n = numClusters / readers.size();
            if (i< numClusters % readers.size()) {
                n++;    // leftovers
            }
            ClusterSeeder s = new ClusterSeeder(i, r, clusters, n, numClusters);
            completionPool.submit(s);
        }
        long start = System.currentTimeMillis();
        i = 0;
        for (DocumentReader r : readers) {
            i++;
            completionPool.take();
            if (i % 10 == 0 || i == readers.size()) {
                double mean = (System.currentTimeMillis() - start) / 1000.0 / i;
                System.err.println("mean completion time for " + i + " of " + readers.size() + " is " + mean + " seconds");
            }
        }
//        finalizeClusters();
    }

    public void finalizeClusters() {
        ClusterStats overall = new ClusterStats(0);
        for (Cluster c : clusters) {
            c.finalize();
            c.getStats().debug();
            c.debug(namer, 20);
            overall.merge(c.getStats());
        }
        System.err.println("============OVERALL STATS================");
        overall.debug();
        System.err.println("");
    }

    public void iteration(boolean finalize) throws InterruptedException, ExecutionException {
        double sum = 0;
        int i = 0;
        for (DocumentReader r : readers) {
            completionPool.submit(new ClusterIteration(i++, r, clusters));
        }
        long start = System.currentTimeMillis();
        i = 0;
        for (DocumentReader r : readers) {
            i++;
            sum += (Double)(completionPool.take().get());
            if (i % 10 == 0 || i == readers.size()) {
                double mean = (System.currentTimeMillis() - start) / 1000.0 / i;
                System.err.println("mean completion time for " + i + " of " + readers.size() + " is " + mean + " seconds");
            }
        }
        if (finalize) {
            finalizeClusters();
        }
    }

    public void shutdown() {
        threadPool.shutdownNow();
    }

    public void writeClusters(File file) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        for (Cluster c : clusters) {
            for (int docId : c.docIds.toArray()) {
                writer.write("" + docId + " ");
            }
            writer.write("\n");
        }
        writer.close();
    }

    public static void main(String args[]) throws InterruptedException, ExecutionException, IOException {
        if (args.length < 3) {
            System.err.println("usage: Clusterer num_clusters num_threads input paths...");
            System.exit(1);
        }
        List<File> paths = new ArrayList<File>();
        for (int i = 2; i < args.length; i++) {
            paths.add(new File(args[i]));
        }
        Clusterer c = new Clusterer(paths, Integer.valueOf(args[0]), Integer.valueOf(args[1]));
        c.calculateIdf();
        c.initialPass();
        for (int i = 0; i < 10; i++) {
            System.err.println("starting iteration " + i);
            c.iteration(true);
        }
        c.iteration(false);
        c.writeClusters(new File("articles/clusters.txt"));
        c.shutdown();
    }
}
