package cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy.genetic;

import java.util.Arrays;
import java.util.Random;

public class RandomMutationOperator extends MutationOperator {

    private final Random geneValueGen;
    private final Random geneCountGen;
    private final Random genePositionGen;

    public RandomMutationOperator(double mutRate, int geneMaxValue) {
        super(mutRate, geneMaxValue);
        geneValueGen = new Random();
        geneCountGen = new Random();
        genePositionGen = new Random();
    }

    @Override
    public Short[] mutate(Short[] individual) {
        int individualLength = individual.length;
        Short[] mutatedIndividual = Arrays.copyOf(individual, individualLength);

        int geneCount = geneCountGen.nextInt(individualLength);
        for (int gene = 0; gene < geneCount; gene++) {
            int genePosition = genePositionGen.nextInt(individualLength);
            mutatedIndividual[genePosition] = (short) (geneValueGen.nextInt(geneMaxValue + 1) - 1);
        }

        return mutatedIndividual;
    }
}
