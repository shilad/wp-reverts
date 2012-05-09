package wp.reverts.kmeans;

public class Document {
    private FeatureList features;
    private int id;
    private int cluster = -1;

    /**
     * Construct a new document in svm light format
     * @param data
     */
    public Document(String data) {
        String idAndFeatures [] = data.trim().split(" ", 2);
        this.id = Integer.valueOf(idAndFeatures[0]);
        this.features = FeatureList.parse(idAndFeatures[1]);
    }

    public FeatureList getFeatures() {
        return features;
    }

    public int getId() {
        return id;
    }

    public int getCluster() {
        return cluster;
    }

    public void setCluster(int cluster) {
        this.cluster = cluster;
    }

    public void debug(Namer namer, int n) {
        System.err.println("top features for document " + namer.getName(id) + "(id " + id + ")");
        features.showTop(namer, n);
    }
}
