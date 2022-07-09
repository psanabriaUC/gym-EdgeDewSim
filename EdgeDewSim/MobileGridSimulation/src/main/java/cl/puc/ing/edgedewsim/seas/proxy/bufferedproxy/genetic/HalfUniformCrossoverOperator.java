package cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy.genetic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class HalfUniformCrossoverOperator extends CrossoverOperator {


    public HalfUniformCrossoverOperator(int genesAmount, double crossoverRate) {
        super(genesAmount, crossoverRate);

    }

    @Override
    public ArrayList<Short[]> recombine(Short[] parent1, Short[] parent2) {

        ArrayList<Integer> positionsWithdifferentGenes = getPositionsWithDifferentGenes(parent1, parent2);
        ArrayList<Short[]> childs = new ArrayList<Short[]>();
        Random rand = new Random();

        Short[] child1 = parent1.clone();
        Short[] child2 = parent2.clone();

        if (positionsWithdifferentGenes.size() > 0) {
            int half = Math.round((float) (positionsWithdifferentGenes.size() / 2)) + 1;
            for (int flipCount = 0; flipCount < half; flipCount++) {
                HashMap<Integer, Integer> alreadyFlipped = new HashMap<Integer, Integer>();
                int geneToExchange = rand.nextInt(half);
                while (alreadyFlipped.containsKey(geneToExchange))
                    geneToExchange = rand.nextInt(half);
                alreadyFlipped.put(geneToExchange, geneToExchange);
                child1[geneToExchange] = parent2[geneToExchange];
                child2[geneToExchange] = parent1[geneToExchange];
            }
            childs.add(child1);
            childs.add(child2);
        } else {
            childs.add(parent1);
            childs.add(parent2);
        }

        return childs;
    }

    private ArrayList<Integer> getPositionsWithDifferentGenes(Short[] parent1, Short[] parent2) {
        ArrayList<Integer> ret = new ArrayList<Integer>();

        for (int i = 0; i < parent1.length; i++)
            if (parent1[i] != parent2[i]) ret.add(i);

        return ret;
    }

}
