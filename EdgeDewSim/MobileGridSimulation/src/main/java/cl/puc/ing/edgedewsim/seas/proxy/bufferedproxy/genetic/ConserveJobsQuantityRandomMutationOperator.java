package cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy.genetic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class ConserveJobsQuantityRandomMutationOperator extends MutationOperator {

    private final Random geneValueGen;
    private final Random geneCountGen;
    private final Random genePositionGen;

    public ConserveJobsQuantityRandomMutationOperator(double mutRate, int geneMaxValue) {
        super(mutRate, geneMaxValue);
        geneValueGen = new Random();
        geneCountGen = new Random();
        genePositionGen = new Random();
    }

    @Override
    public Short[] mutate(Short[] individual) {
        int individualLength = individual.length;
        Short[] mutatedIndividual = Arrays.copyOf(individual, individualLength);
        int mutNotAssignedToAssigned = 0;
        int mutAssignedToNotAssigned = 0;

        int geneCount = geneCountGen.nextInt(individualLength);
        for (int gene = 0; gene < geneCount; gene++) {
            int genePosition = genePositionGen.nextInt(individualLength);
            short originalAssignment = individual[genePosition];
            short mutatedAssignment = (short) (geneValueGen.nextInt(geneMaxValue + 1) - 1);
            mutatedIndividual[genePosition] = mutatedAssignment;
            if (originalAssignment != -1 && mutatedAssignment == -1)
                mutAssignedToNotAssigned++;
            else if (originalAssignment == -1 && mutatedAssignment != -1)
                mutNotAssignedToAssigned++;
        }

        //check whether the same quantity of jobs changed from not assigned to assigned
        //and from assigned to not assigned. If such condition is not met, then apply a
        //correction to the mutated individual in order to restore the AMOUNT of jobs
        //that were originally assigned and not assigned.
        if (mutAssignedToNotAssigned - mutNotAssignedToAssigned != 0)
            return applyCorrection(mutatedIndividual, mutAssignedToNotAssigned, mutNotAssignedToAssigned);

        return mutatedIndividual;
    }

    private Short[] applyCorrection(Short[] mutatedIndividual, int mutAssignedToNotAssigned, int mutNotAssignedToAssigned) {
        ArrayList<Integer> notAssignedJobsPos = new ArrayList<Integer>();
        ArrayList<Integer> assignedJobsPos = new ArrayList<Integer>();

        //identified all not assigned and assigned jobs of the mutatedIndividual
        for (int i = 0; i < mutatedIndividual.length; i++) {
            if (mutatedIndividual[i] == -1)
                notAssignedJobsPos.add(i);
            else
                assignedJobsPos.add(i);
        }

        //apply correction operator
        if (mutAssignedToNotAssigned > mutNotAssignedToAssigned)
            while (mutAssignedToNotAssigned > 0) {
                int notAssignedIndex = genePositionGen.nextInt(notAssignedJobsPos.size());
                //force an assignment to the job
                int job = notAssignedJobsPos.remove(notAssignedIndex);
                mutatedIndividual[job] = (short) (geneValueGen.nextInt(geneMaxValue));
                mutAssignedToNotAssigned--;
            }
        else if (mutNotAssignedToAssigned > mutAssignedToNotAssigned)
            while (mutNotAssignedToAssigned > 0) {
                int assignedJobIndex = genePositionGen.nextInt(assignedJobsPos.size());
                //force a not assignment to the job
                int job = assignedJobsPos.remove(assignedJobIndex);
                mutatedIndividual[job] = -1;
                mutNotAssignedToAssigned--;
            }

        return mutatedIndividual;

    }

}
		
	
