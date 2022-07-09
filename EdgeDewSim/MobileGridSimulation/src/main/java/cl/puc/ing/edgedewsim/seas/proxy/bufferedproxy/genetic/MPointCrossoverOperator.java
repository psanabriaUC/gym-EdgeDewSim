package cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy.genetic;

import java.util.*;


public class MPointCrossoverOperator extends CrossoverOperator {

    private final int crossoverPointNmb;

    public MPointCrossoverOperator(int geneAmount, int point, double crossOverRate) {
        super(geneAmount, crossOverRate);
        this.crossoverPointNmb = point;
    }

    @Override
    public ArrayList<Short[]> recombine(Short[] parent1, Short[] parent2) {

        ArrayList<Integer> crossingPoints = getCrossingPoints();

        ArrayList<Short> child1 = new ArrayList<Short>();
        ArrayList<Short> child2 = new ArrayList<Short>();

        int fromIndex = 0;
        int toIndex = 0;
        boolean switchparent = false;
        for (Iterator<Integer> iterator = crossingPoints.iterator(); iterator.hasNext() || toIndex < this.genesAmount; ) {
            toIndex = iterator.hasNext() ? iterator.next() : this.genesAmount;
            if (!switchparent) {
                child1.addAll(getSubGenes(parent1, fromIndex, toIndex));
                child2.addAll(getSubGenes(parent2, fromIndex, toIndex));
                switchparent = true;
            } else {
                child1.addAll(getSubGenes(parent2, fromIndex, toIndex));
                child2.addAll(getSubGenes(parent1, fromIndex, toIndex));
                switchparent = false;
            }
            fromIndex = toIndex;
        }

        ArrayList<Short[]> ret = new ArrayList<Short[]>();
        ret.add(child1.toArray(new Short[this.genesAmount]));
        ret.add(child2.toArray(new Short[this.genesAmount]));

        return ret;
    }


    private List<Short> getSubGenes(Short[] parent, int fromIndex, int toIndex) {
        return Arrays.asList(Arrays.copyOfRange(parent, fromIndex, toIndex));
    }

    private ArrayList<Integer> getCrossingPoints() {
        ArrayList<Integer> crossingPoints = new ArrayList<Integer>();
        Random rand = new Random();

        for (int i = 0; i < crossoverPointNmb; i++) {
            int point = rand.nextInt(genesAmount);
            while (crossingPoints.contains(point))
                point = rand.nextInt(genesAmount);
            insertInOrder(crossingPoints, point);
        }
        return crossingPoints;
    }

    private void insertInOrder(ArrayList<Integer> crossingPoints, int point) {
        int i = 0;
        while (i < crossingPoints.size() && crossingPoints.get(i) < point)
            i++;
        if (i == crossingPoints.size()) crossingPoints.add(point);
        else crossingPoints.add(i, point);

    }

}
