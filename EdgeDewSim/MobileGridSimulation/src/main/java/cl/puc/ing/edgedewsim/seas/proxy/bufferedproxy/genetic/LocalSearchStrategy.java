package cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy.genetic;

public class LocalSearchStrategy extends MutationOperator {

    private final FitnessFunction fitnessCalculator;
    private long[] jobDataSizes;

    public LocalSearchStrategy(FitnessFunction ff, double mutRate, int geneMaxValue) {
        super(mutRate, geneMaxValue);
        fitnessCalculator = ff;
    }

    /**
     * public LocalSearchStrategy(FitnessFunction ff){
     * fitnessCalculator = ff;
     * <p>
     * }
     */

    public Short[] mutate(Short[] individual) {
        return performLocalSearch(individual);
    }

    /**
     * this method should be called once just after the Genetic Assignment Round
     * was defined. It initialized the interal structure of jobs data sizes
     */
    public void setupLocalSearch() {
        jobDataSizes = fitnessCalculator.getJobAggregatedDataSizes();
    }

    public Short[] performLocalSearch(Short[] individual) {
        DataPlusCPUFitnessValue fitnessValue = (DataPlusCPUFitnessValue) fitnessCalculator.evaluate(individual);
        Short[] improvedIndividual = individual;

        /**STEP 1: nodes selection*/
        int[] nodesJobAssigned = fitnessValue.getNodesJobQuantityAssigned();
        int[] nodesJobExpected = fitnessValue.getNodesJobQuantityExpected();
        int underLoadedValue = 0;
        int overLoadedValue = 0;
        int underLoadedNode = -1;
        int overLoadedNode = -1;
        //int[] nodesLoad = new int[nodesJobAssigned.length];

        //get the most overloaded and underloaded node
        for (int i = 0; i < nodesJobAssigned.length; i++) {
            int dif = nodesJobAssigned[i] - nodesJobExpected[i];
            if (dif > 0 && dif > overLoadedValue) {
                overLoadedValue = dif;
                overLoadedNode = i;
            } else if (dif < 0 && dif < underLoadedValue) {
                underLoadedValue = dif;
                underLoadedNode = i;
            }
        }

        /**STEP 2: Perform a job exchange between selected nodes so that to achieve an
         * approximation to the desired balance of jobs, but aiming, at the
         * same time, to conserve the amount data transfered.
         * The strategy adopted here is: select a job from the underloaded node and exchanges
         * it by two jobs of the overloaded node so that, the aggregated data of both jobs
         * differ in the minimun value to the data w.r.t the single job. Explore all pair of
         * jobs of the overloaded node. Limit the exploration to jobs whose individual size
         * is less than the single job.*/
        if (underLoadedNode != -1 && overLoadedNode != -1) {
            //Assuming that jobs of the individual are sorted in ascending order w.r.t the
            //amount of data transfer requirements, find the biggest job of the underloaded node
            int biggestJobOfUnderloadedNodeIndex = individual.length - 1;
            for (; biggestJobOfUnderloadedNodeIndex >= 0; biggestJobOfUnderloadedNodeIndex--) {
                if (individual[biggestJobOfUnderloadedNodeIndex] == underLoadedNode) break;
            }

            if (biggestJobOfUnderloadedNodeIndex > -1 && biggestJobOfUnderloadedNodeIndex < individual.length) {
                long jobSize = jobDataSizes[biggestJobOfUnderloadedNodeIndex];
                int[] jobsToExchange = solveSubsetSumProblem(individual, jobSize, biggestJobOfUnderloadedNodeIndex, overLoadedNode);

                if (jobsToExchange[0] != -1) {
                    //Logger.logLocalSearchResult("Individual before local search="+fitnessValue.getValue());
                    improvedIndividual = individual.clone();
                    //perform job exchange operation over a copy of the individual received as argument
                    improvedIndividual[jobsToExchange[0]] = (short) underLoadedNode;
                    improvedIndividual[jobsToExchange[1]] = (short) underLoadedNode;
                    improvedIndividual[biggestJobOfUnderloadedNodeIndex] = (short) overLoadedNode;
                    //Logger.logLocalSearchResult("Individual after local search="+fitnessCalculator.evaluate(improvedIndividual).getValue());
                }
            } else {
                //Logger.logLocalSearchResult("Failed result of local search: the under loaded device could not be found");
            }
        } else {
            //Logger.logLocalSearchResult("Failed result of local search: there are not devices with complement load");
        }
        return improvedIndividual;

    }

    /**
     * the search requires that jobs of jobDataSizes array be sorted in ascending order w.r.t data transfer
     * requirement
     */
    private int[] solveSubsetSumProblem(Short[] individual, long targetSum, int jobLimit, int overLoadedNode) {
        //Identify the job set of the overloaded node whose datasizes could sum less than
        //or equal targetSum parameter
        int[] overLoadedJobsSet = new int[jobLimit];
        int sizeOfOverloadedNodeSet = 0;
        for (int jobIndex = 0; jobIndex < jobLimit; jobIndex++) {
            if (individual[jobIndex] == overLoadedNode) {
                overLoadedJobsSet[sizeOfOverloadedNodeSet] = jobIndex;
                sizeOfOverloadedNodeSet++;
            }
        }

        //now find a subset of two jobs whose aggregated data is the closest to the sum
        //argument
        int[] ret = new int[2];//will contains indexes of jobs that can be used as index of the "individual" array
        ret[0] = -1;
        ret[1] = -1;
        long smallestDif = Long.MAX_VALUE;
        if (sizeOfOverloadedNodeSet > 1)
            for (int firstJob = 0; firstJob < sizeOfOverloadedNodeSet - 1; firstJob++) {
                long firstJobSize = jobDataSizes[overLoadedJobsSet[firstJob]];
                for (int secondJob = firstJob + 1; secondJob < sizeOfOverloadedNodeSet; secondJob++) {
                    long dif = targetSum - (firstJobSize + jobDataSizes[overLoadedJobsSet[secondJob]]);
                    if (dif >= 0 && dif < smallestDif) {
                        smallestDif = dif;
                        ret[0] = overLoadedJobsSet[firstJob];
                        ret[1] = overLoadedJobsSet[secondJob];
                    }
                }
            }

        //logging exchange operation result
        if (ret[0] != -1) {
            //Logger.logLocalSearchResult("Success result of local search: job exchange dif="+(targetSum-(jobDataSizes[ret[0]]+jobDataSizes[ret[1]])));
        } else {
            //Logger.logLocalSearchResult("Failed result of local search: job exchange is not possible");
        }
        return ret;
    }


}
