package cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy.genetic;

import java.util.ArrayList;

public class DeterministicCrowding extends PopulationReplacementStrategy {

    private final int FIRST_PARENT = 0;
    private final int SECOND_PARENT = 1;
    private final int FIRST_CHILD = 2;
    private final int SECOND_CHILD = 3;
    private final FitnessFunction fitnessFunction;

    public DeterministicCrowding(FitnessFunction fitnessFunction, int numberOfBestIndividuals) {
        super(numberOfBestIndividuals);
        this.fitnessFunction = fitnessFunction;
    }

    @Override
    /**it is assumed that the currentPop param contains four elements where in its first two positions are the parents and the rest are the childrens*/
    public ArrayList<Short[]> filterBestIndividuals(ArrayList<Short[]> parentsAndChildren) {
        ArrayList<Short[]> survivors = new ArrayList<Short[]>();

        survivors.add(getBestIndividual(parentsAndChildren.get(FIRST_PARENT), parentsAndChildren.get(SECOND_PARENT), parentsAndChildren.get(FIRST_CHILD)));
        survivors.add(getBestIndividual(parentsAndChildren.get(FIRST_PARENT), parentsAndChildren.get(SECOND_PARENT), parentsAndChildren.get(SECOND_CHILD)));

        return survivors;
    }

    private Short[] getBestIndividual(Short[] parent1, Short[] parent2, Short[] child) {
        Short[] mostSimilarParent = getMostSimilarParent(parent1, parent2, child);
        if (fitnessFunction.evaluate(mostSimilarParent).getValue() >= fitnessFunction.evaluate(child).getValue()) {
            gaInfo.registerParentChildComparison(false);
            return mostSimilarParent;
        } else {
            gaInfo.registerParentChildComparison(true);
            return child;
        }
    }

    private Short[] getMostSimilarParent(Short[] parent1, Short[] parent2, Short[] child) {
        int equalGenesWithparent1 = getEqualGenes(parent1, child);
        int equalGenesWithparent2 = getEqualGenes(parent2, child);

        if (equalGenesWithparent1 > equalGenesWithparent2)
            return parent1;
        else
            return parent2;
    }

    private int getEqualGenes(Short[] parent1, Short[] child) {
        int equalGenes = 0;
        for (int gene = 0; gene < parent1.length; gene++) {
            if (parent1[gene] == child[gene]) equalGenes++;
        }
        return equalGenes;
    }

}
