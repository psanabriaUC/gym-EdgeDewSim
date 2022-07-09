package cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy.genetic;

public abstract class MutationOperator {

    protected double mutationRate;
    protected int geneMaxValue;

    public MutationOperator(double mutRate, int geneMaxValue) {
        this.mutationRate = mutRate;
        this.geneMaxValue = geneMaxValue;
    }


    public abstract Short[] mutate(Short[] individual);


    public double getMutationRate() {
        return mutationRate;
    }

}
