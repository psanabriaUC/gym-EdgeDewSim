package cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy.genetic;

import java.util.ArrayList;

public abstract class PopulationReplacementStrategy {

    protected int numberOfBestIndividuals;
    protected GAExecInformation gaInfo = null;

    public PopulationReplacementStrategy(int numberOfBestIndividuals) {
        this.numberOfBestIndividuals = numberOfBestIndividuals;
    }

    public abstract ArrayList<Short[]> filterBestIndividuals(ArrayList<Short[]> currentPop);

    public void setGAExecInformation(GAExecInformation gaInfo) {
        this.gaInfo = gaInfo;
    }

}
