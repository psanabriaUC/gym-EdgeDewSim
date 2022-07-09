package cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy.genetic;

import cl.puc.ing.edgedewsim.mobilegrid.node.BatteryManager;
import cl.puc.ing.edgedewsim.mobilegrid.node.SchedulerProxy;
import cl.puc.ing.edgedewsim.seas.proxy.DataAssignment;
import cl.puc.ing.edgedewsim.seas.proxy.dataevaluator.OverflowPenaltyDataEvaluator;
import cl.puc.ing.edgedewsim.simulator.Simulation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

public class DataCPUEnergyAwareFitnessFunction extends FitnessFunction {

    private float dataWeight = 0.5f;
    private float cpuWeight = 0.5f;

    public DataCPUEnergyAwareFitnessFunction(int maxAvailable, float dataWeight, float cpuWeight, Simulation simulation) {
        super(simulation);
        this.setMaxEnergyAllowedForDataTransfer(maxAvailable);
        this.dataWeight = dataWeight;
        this.cpuWeight = cpuWeight;
        DataAssignment.evaluator = new OverflowPenaltyDataEvaluator(getMaxEnergyAllowedForDataTransfer());
    }

    public DataCPUEnergyAwareFitnessFunction(float dataWeight, float cpuWeight, Simulation simulation) {
        super(simulation);
        this.dataWeight = dataWeight;
        this.cpuWeight = cpuWeight;
        DataAssignment.evaluator = new OverflowPenaltyDataEvaluator(getMaxEnergyAllowedForDataTransfer());
    }

    /**
     * Return a double representing a fitness value that contemplates not only
     * the feasibility of transferring the assigned jobs with the available
     * grid energy but also execute the jobs with the remaining energy. The fitness
     * is composed by a weighted data aware score plus a weighted CPU aware score.
     * The first results from the application of a DataEvaluator function that gives
     * a indicates how well the data transferring is supported with the available
     * energy. The CPU aware score results from the Euclidean distance between the
     * number of assigned jobs to each device and the number of jobs that such devices
     * are expected to execute according to its processing capability. The last data
     * is obtained by means of a criterion, e.g., E-SEAS, JEC, FWC, used to schedule
     * CPU-bound jobs.
     * The weights of the data and CPU aware components are set in the constructor of
     * the class
     */

    public FitnessValue getFitness(Short[] individual) {
        HashMap<Integer, DataAssignment> deviceAssignments = convertIntoDeviceAssignments(individual);

        int[] nodesRemainingEnergyLevel = new int[devicesId.size()];
        int[] nodesJobCountAssignments = new int[devicesId.size()];
        Arrays.fill(nodesJobCountAssignments, 0);
        double[] nodesEnergySpent = new double[devicesId.size()];
        double gridEnergy = SchedulerProxy.getProxyInstance(simulation).getCurrentAggregatedNodesEnergy();
        int gridJobsTransfered = 0;
        for (Iterator<DataAssignment> iterator = deviceAssignments.values().iterator(); iterator.hasNext(); ) {
            DataAssignment da = iterator.next();
            DataAssignment.evaluator.eval(da);

            short devId = getDeviceId(da.getDevice());
            nodesEnergySpent[devId] = da.getDeviceEnergyWasted();
            gridEnergy -= nodesEnergySpent[devId];
            nodesRemainingEnergyLevel[devId] = SchedulerProxy.getProxyInstance(simulation).getLastReportedSOC(da.getDevice()) -
                    (int) (((da.getDeviceEnergyWasted() * 100) / da.getDevice().getTotalBatteryCapacityInJoules()) * BatteryManager.PROFILE_ONE_PERCENT_REPRESENTATION);
            nodesJobCountAssignments[devId] = da.getAssignedJobs().size();
            gridJobsTransfered += da.getAffordableJobCompletelyTransfered();
            //Logger.println("Expected node energy after jobs data transferrings:"+((Entity)da.getDevice()).getName()+" "+(nodesRemainingEnergyLevel[devId]));
        }

        int[] expectedNodesJobCountAssignments = new int[devicesId.size()];
        Arrays.fill(expectedNodesJobCountAssignments, 0);
        int assignedJobs = getAssignedJobCount(individual);
        for (int i = 0; i < assignedJobs; i++) {
            int bestDev = 0;
            double rankBestDev = -1;
            for (int dev = 0; dev < devicesId.size(); dev++) {
                //rank nodes with E-SEAS criterion
                double devRank = (getDevice(dev).getMIPS() * nodesRemainingEnergyLevel[dev]) / (expectedNodesJobCountAssignments[dev] + 1);
                if (devRank > rankBestDev) {
                    rankBestDev = devRank;
                    bestDev = dev;
                }
            }
            expectedNodesJobCountAssignments[bestDev] += 1;
        }
        double jobsTransferedPercentage = (gridJobsTransfered / individual.length);
        double normalizedGridRemainingEnergy = gridEnergy / SchedulerProxy.getProxyInstance(simulation).getCurrentAggregatedNodesEnergy();

        double cpuAssignmentScore = getEuclideanDistance(expectedNodesJobCountAssignments, nodesJobCountAssignments);
        double cpuMaxDistance = getMaxCPUScore(expectedNodesJobCountAssignments, assignedJobs);
        double normalizedCpuDistance = cpuAssignmentScore / cpuMaxDistance;

        double fitness = (dataWeight * jobsTransferedPercentage) + (cpuWeight * (1 - normalizedCpuDistance)) + (0.4 * normalizedGridRemainingEnergy);
        DataCPUEnergyFitnessValue fitnessValue = new DataCPUEnergyFitnessValue(fitness, gridJobsTransfered, cpuAssignmentScore, gridEnergy);

        //Logger.println("Fitness="+fitness+" composed by: jobTransferedPercentage="+jobsTransferedPercentage+" weighted="+(dataWeight * jobsTransferedPercentage)+" cpuAssignmentScore(ideal 1)="+cpuAssignmentScore + " weighted="+(cpuWeight * (1/(cpuAssignmentScore+1))));

        return fitnessValue;
    }

    private double getMaxCPUScore(int[] expectedNodesJobCountAssignments, int jobCount) {
        int leastIndex = -1;
        int leastValue = Integer.MAX_VALUE;
        long[] squaredDifference = new long[expectedNodesJobCountAssignments.length];
        double sumOfSquared = 0;
        for (int i = 0; i < expectedNodesJobCountAssignments.length; i++) {
            if (expectedNodesJobCountAssignments[i] < leastValue) {
                leastValue = expectedNodesJobCountAssignments[i];
                leastIndex = i;
            }
            squaredDifference[i] = (long) Math.pow(expectedNodesJobCountAssignments[i], 2);
            sumOfSquared += squaredDifference[i];
        }
        sumOfSquared -= squaredDifference[leastIndex];
        squaredDifference[leastIndex] = (long) Math.pow((double) expectedNodesJobCountAssignments[leastIndex] - jobCount, 2);
        sumOfSquared += squaredDifference[leastIndex];
        return Math.sqrt(sumOfSquared);
    }

    private double getEuclideanDistance(int[] expectedNodesJobCountAssignments, int[] nodesJobCountAssignments) {
        int squareSum = 0;

        for (int i = 0; i < expectedNodesJobCountAssignments.length; i++)
            squareSum += Math.pow(expectedNodesJobCountAssignments[i] - nodesJobCountAssignments[i], 2);

        return Math.sqrt(squareSum);

    }

    private int getAssignedJobCount(Short[] individual) {
        int assigned = 0;
        for (int i = 0; i < individual.length; i++) {
            if (individual[i] != -1) assigned++;
        }
        return assigned;
    }

}
