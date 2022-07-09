package cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy.genetic;

import java.util.ArrayList;

public abstract class CrossoverOperator {

    protected double crossOverRate;
    protected int genesAmount;
    protected String name;

    public CrossoverOperator(int genesAmount, double crossoverRate) {
        this.crossOverRate = crossoverRate;
        this.genesAmount = genesAmount;
    }

    public abstract ArrayList<Short[]> recombine(Short[] parent1, Short[] parent2);

    public double getCrossOverRate() {
        return crossOverRate;
    }

    public void setCrossOverRate(double crossOverRate) {
        this.crossOverRate = crossOverRate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
