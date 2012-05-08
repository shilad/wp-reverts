package wp.reverts.kmeans;

import java.io.*;
import java.util.Iterator;

public class DocumentReader implements Iterable<Document> {
    File path;

    public DocumentReader(File path) {
        this.path = path;
    }

    public Iterator<Document> iterator() {
        return new MyIterator(path);
    }

    public static class MyIterator implements Iterator<Document>{
        private BufferedReader reader;
        private String lineBuff = null;
        private boolean eof = false;

        public MyIterator(File path) {
            try {
                this.reader = new BufferedReader(new FileReader(path));
            } catch (FileNotFoundException e) {
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
}
