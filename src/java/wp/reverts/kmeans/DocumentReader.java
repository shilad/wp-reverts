package wp.reverts.kmeans;

import java.io.*;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

public class DocumentReader implements Iterable<Document> {
    File path;

    public DocumentReader(File path) {
        this.path = path;
    }

    public Iterator<Document> iterator() {
        return new MyIterator(path);
    }

    public class MyIterator implements Iterator<Document>{
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

        public Document next() {
            if (!fillBuff()) return null;
            Document d = new Document(lineBuff);
            lineBuff = null;
            if (lineNum % 10000 == 0) {
                System.err.println("reading line " + lineNum + " of " + path);
            }
            return d;
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
        int numDocs = 0;
        for (String path : args) {
            DocumentReader dr = new DocumentReader(new File(path));
            System.err.println("reading documents from " + path);
            for (Document d : dr) {
                if (numDocs++ % 1000 == 0) {
                    System.err.println("reading document number " + numDocs);
                }
            }
        }
        long elapsed = System.currentTimeMillis() - start;
        System.err.println("elapsed time is " + (elapsed / 1000.0));
        System.err.println("docs per second is " + (1000 * numDocs / elapsed));
    }
}
