package cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy.genetic;

public class DataPlusCPUFitnessValue extends FitnessValue {

    /**
     * most of the fields of a DataPlusCPUFitnessValue object store information
     * for local search step and/or logging purposes
     **/

    public static float dataWeight;
    public static float cpuWeight;

    //transferring related information of the individual
    private final int totalAssignedJobs;
    private final int nonCompletelyTransferedJobs;
    private final int totalJobs;
    private final double cpuMaxScore;
    //CPU related information of the individual
    protected int[] nodesJobsQuantityAssigned;
    protected int[] nodesJobsQuantityExpected;
    private int completelyTransferedJobs;
    private double cpuScore;
    private double gridRemainingEnergy;

    public DataPlusCPUFitnessValue(int[] nodesJobAssigned, int[] nodesJobExpected, int totalJobs, int totalAssignedJobs,
                                   int compJobs, double nonCompJobs, double cpuScore, double cpuMaxScore,
                                   double gridRemEnergy) {
        this.nodesJobsQuantityAssigned = nodesJobAssigned;
        this.nodesJobsQuantityExpected = nodesJobExpected;
        this.totalJobs = totalJobs;
        this.totalAssignedJobs = totalAssignedJobs;
        this.completelyTransferedJobs = compJobs;
        this.nonCompletelyTransferedJobs = (int) nonCompJobs;
        this.cpuScore = cpuScore;
        this.cpuMaxScore = cpuMaxScore;
        this.gridRemainingEnergy = gridRemEnergy;

        //usando OverFlowDataAssignementEvaluator
        double jobsTransferedPercentage = (completelyTransferedJobs) / totalJobs;
        //double jobsTransferedPercentage = (completelyTransferedJobs-nonCompletelyTransferedJobs) / this.totalAssignedJobs;
        double normalizedCpuDistance = cpuScore / cpuMaxScore;

        fitnessValue = (dataWeight * jobsTransferedPercentage) + (cpuWeight * (1 - normalizedCpuDistance));
    }


    @Override
    public String toString() {
        return "fitness=" + fitnessValue + " completedTransfJobs=" + completelyTransferedJobs + " nonCompletelyTransfJobs=" + this.nonCompletelyTransferedJobs + " cpuScore=" + cpuScore / cpuMaxScore + " RemainingEnergy=" + gridRemainingEnergy;
    }

    public int[] getNodesJobQuantityAssigned() {
        return nodesJobsQuantityAssigned;
    }

    public int[] getNodesJobQuantityExpected() {
        return nodesJobsQuantityExpected;
    }

    public void setCompletelyTransferedJobs(int completelyTransferedJobs) {
        this.completelyTransferedJobs = completelyTransferedJobs;
    }

    public double getCpuScore() {
        return cpuScore;
    }

    public void setCpuScore(double cpuScore) {
        this.cpuScore = cpuScore;
    }

    public double getGridRemainingEnergy() {
        return gridRemainingEnergy;
    }

    public void setGridRemainingEnergy(double gridRemainingEnergy) {
        this.gridRemainingEnergy = gridRemainingEnergy;
    }

}
