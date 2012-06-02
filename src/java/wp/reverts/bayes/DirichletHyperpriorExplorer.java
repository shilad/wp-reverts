package wp.reverts.bayes;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.math3.util.FastMath;

import java.io.*;
import java.util.Arrays;

/**
 * Findings for optimal N:
 * - users: 820
 * - articles:
 *
 */
public class DirichletHyperpriorExplorer {

    // parallel arrays of observations
    // for each entry group ids contains the grouping term
    // results contains the categorical choice.
    private TIntList groups;
    private TIntList categories;

    // number of categories
    int k;

    // background proportions for each category
    double proportions[];

    public DirichletHyperpriorExplorer(File input) throws IOException {
        readInput(input);
        calculateProportions();
    }

    public double searchParameters() {
        double expectation = 0.0;
        double likelihoodSum = 0.0;
        for (int n = 600; n < 1500; n++) {
            double x = n;
            double logLikelihood = evaluateParameter(x);
            double likelihood = FastMath.exp(logLikelihood);
            System.out.println("x=" + x + " has log likelihood " + logLikelihood + " or " + likelihood);
            likelihoodSum += likelihood;
            expectation += likelihood * x;
        }
        return expectation / likelihoodSum;
    }

    private double evaluateParameter(double n) {
        int counts[] = new int[k];
        double fit = 0.0;
        int lastGroup = -1;
        int lastGroupCount = 0;

        for (int i = 0; i < groups.size(); i++) {
            int g = groups.get(i);
            int c = categories.get(i);
            if (lastGroup != g) {
                Arrays.fill(counts, 0);
                lastGroupCount = 0;
                lastGroup = g;
            }
            double alpha = counts[c] + proportions[c] * n;
            double alphaPlusBeta = lastGroupCount + n;
            fit += FastMath.log(alpha / alphaPlusBeta);

            counts[c]++;
            lastGroupCount++;
        }

        return fit;
    }

    private void calculateProportions() {
        proportions = new double[k];
        Arrays.fill(proportions, 1.0);  // laplacian smoothing
        for (int c : categories.toArray()) {
            assert(c <= k);
            proportions[c] += 1;
        }
        int sum = proportions.length + categories.size();
        for (int c = 0; c < proportions.length; c++) {
            proportions[c] /= sum;
        }
    }

    private void readInput(File input) throws IOException {
        TIntIntMap categoryMap = new TIntIntHashMap();  // used to make the categories dense.

        groups = new TIntArrayList();
        categories = new TIntArrayList();

        BufferedReader reader = new BufferedReader(new FileReader(input));
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            String tokens[] = line.split("\\s+");
            if (tokens.length != 2) {
                System.err.println("invalid line: " + line);
                continue;
            }
            groups.add(Integer.valueOf(tokens[0]));
            int realC = Integer.valueOf(tokens[1]);
            categoryMap.putIfAbsent(realC, categoryMap.size());
            int denseC = categoryMap.get(realC);
            categories.add(denseC);
        }

        k = categoryMap.size();
    }

    public static void main(String args[]) throws IOException {
        DirichletHyperpriorExplorer explorer = new DirichletHyperpriorExplorer(new File(args[0]));
        System.out.println("map estimate for n is " + explorer.searchParameters());
    }
}
