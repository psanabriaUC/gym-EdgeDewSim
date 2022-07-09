package cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy.genetic;

import java.util.ArrayList;

public abstract class ParentSelector {

    protected ArrayList<Short[]> selectableIndividuals;
    private String name;

    /**
     * This method returns a parent candidate of the population
     *
     * @throws Exception
     */
    public abstract Short[] getParent() throws Exception;

    /**
     * This method set the list of individuals that are able to participate in the tournaments
     */
    public void setPopulation(ArrayList<Short[]> selectableIndividuals) {
        this.selectableIndividuals = selectableIndividuals;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
