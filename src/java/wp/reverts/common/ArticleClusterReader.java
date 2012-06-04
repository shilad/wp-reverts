package wp.reverts.common;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.io.*;
import java.util.logging.Logger;

public class ArticleClusterReader {
    private final Logger logger = Logger.getLogger(this.getClass().getName());

    public TIntIntMap read(File path) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(path));
        TIntIntMap articleClusters = new TIntIntHashMap();
        int clusterId = 0;
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            for (String s : line.split("\\s+")) {
                articleClusters.put(Integer.valueOf(s), clusterId);
            }
            clusterId++;
        }
        logger.info("read " + clusterId + " clusters containing " + articleClusters.size() + " from " + path);
        return articleClusters;
    }
}
