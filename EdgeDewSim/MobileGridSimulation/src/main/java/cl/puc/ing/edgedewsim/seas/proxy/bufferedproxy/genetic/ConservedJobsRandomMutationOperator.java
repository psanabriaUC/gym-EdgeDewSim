package cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy.genetic;

import java.util.Arrays;
import java.util.Random;

public class ConservedJobsRandomMutationOperator extends MutationOperator {

    private final Random geneValueGen;
    private final Random geneCountGen;
    private final Random genePositionGen;

    public ConservedJobsRandomMutationOperator(double mutRate, int geneMaxValue) {
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
            while (individual[genePosition] == -1) {//means that the job in the individual is
                //not assigned, we do not want to assigned
                //a job that was not originally assigned
                genePosition = genePositionGen.nextInt(individualLength);
            }
            //now that we identified a job that was originally assigned to a node, we assign it to
            //to another node including the possibility of left it out of the currently assigned jobs
            mutatedIndividual[genePosition] = (short) (geneValueGen.nextInt(geneMaxValue + 1) - 1);
        }

        return mutatedIndividual;
    }
}
