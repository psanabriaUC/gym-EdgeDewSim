package cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy.genetic;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;

import java.util.ArrayList;

public class UniformCrossoverOperator extends CrossoverOperator {

    public UniformCrossoverOperator(int geneAmount, double crossoverRate) {
        super(geneAmount, crossoverRate);
    }

    @Override
    public ArrayList<Short[]> recombine(Short[] parent1, Short[] parent2) {
        EnumeratedIntegerDistribution rDistribution = new EnumeratedIntegerDistribution(new int[]{0, 1}, new double[]{0.5d, 0.5d});
        Short[] child1 = new Short[genesAmount];
        Short[] child2 = new Short[genesAmount];

        ArrayList<Short[]> parents = new ArrayList<Short[]>();
        parents.add(parent1);
        parents.add(parent2);

        int parentNmb = rDistribution.sample();
        for (int gene = 0; gene < genesAmount; gene++) {
            child1[gene] = parents.get(parentNmb)[gene];
            child2[gene] = parents.get(((parentNmb + 1) % 2))[gene];
            parentNmb = rDistribution.sample();
        }

        ArrayList<Short[]> ret = new ArrayList<Short[]>();
        ret.add(child1);
        ret.add(child2);

        return ret;
    }

}
