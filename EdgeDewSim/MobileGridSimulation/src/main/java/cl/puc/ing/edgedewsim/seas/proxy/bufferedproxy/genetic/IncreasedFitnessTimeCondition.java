package cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy.genetic;

import java.util.ArrayList;

/**
 * This termination condition checks whether the fitness value improved with respect to the last best fitness
 * value found during evolution. The termination condition is satisfied either when the maxTimeSameFitness
 * (provided as argument) is reached or when the maxEvolutionTotalTime is reached
 */
public class IncreasedFitnessTimeCondition implements TerminationCondition {

    private final FitnessFunction fitnessEvaluator;
    private double lastBestFitness = Double.NEGATIVE_INFINITY;
    private long maxTimeSameFitness = 60000L;//this is the time that indicate how long running the genetic algorithm in the search of new individuals
    private long startTimeSameFitness = 0;

    /**
     * this is the max time the evolutionary algorithm is able to run. When the lastBestFitness value constantly
     * //changes within the time window indicated by maxTimeSameFitness the search may fall into a infinite loop.
     * //To avoid that, a max evolution time window is also checked.
     **/
    private long maxEvolutionTotalTime = 300000L;

    /**
     * Unlike startTimeSameFitness this value is not reset throughout evolution cycles and is permanently
     * checked until it reaches the maxEvolutionTotalTime
     */
    private long startEvolutionTime = 0;

    public IncreasedFitnessTimeCondition(Long maxTimeSameFitness, Long maxEvolutionTotalTime, FitnessFunction fitnessEvaluator) {
        this.maxTimeSameFitness = maxTimeSameFitness;
        this.maxEvolutionTotalTime = maxEvolutionTotalTime;
        this.fitnessEvaluator = fitnessEvaluator;
    }

    @Override
    public boolean satisfiedCondition(ArrayList<Short[]> population) {

        FitnessValue currentBestFitness = this.fitnessEvaluator.evaluate(this.fitnessEvaluator.getBestIndividual());

        if (lastBestFitness < currentBestFitness.getValue()) {
            lastBestFitness = currentBestFitness.getValue();
            startTimeSameFitness = System.currentTimeMillis();
            System.out.println(startTimeSameFitness + " " + currentBestFitness.toString());
        }

        return System.currentTimeMillis() - startTimeSameFitness > maxTimeSameFitness
                || System.currentTimeMillis() - startEvolutionTime > maxEvolutionTotalTime;
    }

    @Override
    public String toString() {
        return IncreasedFitnessTimeCondition.class.getSimpleName() + " maxTimeSameFitness=" + this.maxTimeSameFitness + " maxEvolutionTotalTime=" + this.maxEvolutionTotalTime;
    }

    /**
     * this method initializes the startEvolutionTime and startTimeSameFitness fields with the
     * System.currentTimeMillis(). The method should be invoked just before start the evolution process
     */
    public void startEvolution() {
        this.startEvolutionTime = System.currentTimeMillis();
        this.startTimeSameFitness = System.currentTimeMillis();
        this.lastBestFitness = Double.NEGATIVE_INFINITY;
    }

    @Override
    public String getName() {
        return toString();
    }

}
