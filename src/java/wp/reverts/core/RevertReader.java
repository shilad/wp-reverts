package wp.reverts.core;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import wp.reverts.kmeans.Document;
import wp.reverts.kmeans.IdfAdjuster;

import java.io.*;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

public class RevertReader implements Iterable<Revert> {
    TIntObjectMap<User> users = new TIntObjectHashMap<User>();
    TIntObjectMap<Article> articles = new TIntObjectHashMap<Article>();

    private File path;
    private int truncationLength = Integer.MAX_VALUE;
    private IdfAdjuster idfAdjuster;
    private boolean saveRevertingComment = true;
    private boolean saveRevertedComment = true;
    private RevertParser parser = new RevertParser();

    public RevertReader(File path) {
        this.path = path;
    }

    public Iterator<Revert> iterator() {
        return new MyIterator(path);
    }

    public class MyIterator implements Iterator<Revert>{
        private BufferedReader reader;
        private String lineBuff = null;
        private boolean eof = false;
        private int lineNum = 0;

        public MyIterator(File path) {
            try {
                if (path.toString().toLowerCase().endsWith(".gz")) {
                    InputStream fileStream = new FileInputStream(path);
                    InputStream gzipStream = new GZIPInputStream(fileStream);
                    Reader decoder = new InputStreamReader(gzipStream, "UTF-8");
                    reader = new BufferedReader(decoder);
                } else {
                    this.reader = new BufferedReader(new FileReader(path));
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                eof = true;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                eof = true;
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                eof = true;
            }
        }


        public boolean hasNext() {
            return fillBuff();
        }

        public Revert next() {
            if (!fillBuff()) return null;
            Revert r = parser.parse(lineBuff, users, articles);
            if (!saveRevertedComment) r.setRevertedComment(null);
            if (!saveRevertingComment) r.setRevertingComment(null);
            lineBuff = null;
            if (lineNum % 10000 == 0) {
                System.err.println("reading line " + lineNum + " of " + path);
            }
            return r;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        private boolean fillBuff() {
            if (lineBuff != null) {
                return true;
            }
            if (!eof) {
                try {
                    lineBuff = reader.readLine();
                    lineNum++;
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    eof = true;
                    return false;
                }
                if (lineBuff == null) {
                    eof = true;
                }
            }
            return (lineBuff != null);
        }
    }

    public static void main(String args []) {
        long start = System.currentTimeMillis();
        int numReverts = 0;
        for (String path : args) {
            RevertReader rr = new RevertReader(new File(path));
            System.err.println("reading reverts from " + path);
            for (Revert r : rr) {
                if (numReverts++ % 1000 == 0) {
                    System.err.println("reading document number " + numReverts);
                }
            }
        }
        long elapsed = System.currentTimeMillis() - start;
        System.err.println("elapsed time is " + (elapsed / 1000.0));
        System.err.println("docs per second is " + (1000 * numReverts / elapsed));
    }
}
