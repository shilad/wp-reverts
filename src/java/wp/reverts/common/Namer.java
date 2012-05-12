package wp.reverts.common;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Namer {
    private Map<Integer, String> map = new HashMap<Integer, String>();

    public Namer(String path) throws IOException {
        System.err.println("reading names from " + path);
        BufferedReader reader = new BufferedReader(new FileReader(path));
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            String tokens[] = line.split("\t");
            if (tokens.length == 2) {
                map.put(Integer.valueOf(tokens[1].trim()), tokens[0]);
            } else {
                System.err.println("invalid line in " + path + ": " + line.trim());
            }
        }
        System.err.println("finished reading " + map.size() + " names");
    }

    public String getName(int id) {
        if (map.containsKey(id)) {
            return map.get(id);
        } else {
            return "Unknown";
        }
    }
}
