package cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy.genetic;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.seas.proxy.DataAssignment;
import cl.puc.ing.edgedewsim.simulator.Simulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public abstract class FitnessFunction {

    public static HashMap<Integer, Device> devicesId;
    public static HashMap<Device, Integer> devicesObjects;
    public GeneticAssignmentRound gar;
    protected HashMap<Short[], FitnessValue> cachedFitnessValues;
    protected Simulation simulation;
    private int maxEnergyAllowedForDataTransfer = 100;

    public FitnessFunction(Simulation simulation) {
        cachedFitnessValues = new HashMap<>();
        this.simulation = simulation;
    }

    protected abstract FitnessValue getFitness(Short[] individual);


    public long[] getJobAggregatedDataSizes() {
        ArrayList<Job> jobsToSchedule = gar.getJobsToSchedule();
        long[] aggregatedJobsData = new long[jobsToSchedule.size()];
        for (int j = 0; j < jobsToSchedule.size(); j++) {
            Job job = jobsToSchedule.get(j);
            aggregatedJobsData[j] = job.getInputSize() + job.getOutputSize();
        }
        return aggregatedJobsData;
    }

    /**
     * return the fitness value of the individual
     */
    public FitnessValue evaluate(Short[] individual) {
        if (!cachedFitnessValues.containsKey(individual)) {
            FitnessValue fitness = getFitness(individual);
            cachedFitnessValues.put(individual, fitness);
            return fitness;
        } else {
            return cachedFitnessValues.get(individual);
        }
    }


    protected HashMap<Integer, DataAssignment> convertIntoDeviceAssignments(Short[] assignments) {
        HashMap<Integer, DataAssignment> deviceAssignments = new HashMap<Integer, DataAssignment>();

        for (int job = 0; job < assignments.length; job++) {
            int node_id = assignments[job];
            if (node_id != -1) {
                DataAssignment nodeAssignment = null;
                if (!deviceAssignments.containsKey(node_id)) {
                    nodeAssignment = new DataAssignment(devicesId.get(node_id));
                    deviceAssignments.put(node_id, nodeAssignment);
                } else
                    nodeAssignment = deviceAssignments.get(node_id);

                nodeAssignment.scheduleJob(gar.getJob(job));
            }
        }
        return deviceAssignments;
    }

    public void removeCachedAssignment(Short[] assignment) {
        cachedFitnessValues.remove(assignment);
    }

    public int getMaxEnergyAllowedForDataTransfer() {
        return maxEnergyAllowedForDataTransfer;
    }

    public void setMaxEnergyAllowedForDataTransfer(
            int maxEnergyAllowedForDataTransfer) {
        this.maxEnergyAllowedForDataTransfer = maxEnergyAllowedForDataTransfer;
    }

    public void clearCachedAssignments() {
        cachedFitnessValues.clear();
    }

    public void refreshCachedAssignments(ArrayList<Short[]> population) {
        HashMap<Short[], FitnessValue> refreshedCached = new HashMap<Short[], FitnessValue>();
        for (Iterator<Short[]> iterator = population.iterator(); iterator.hasNext(); ) {
            Short[] assignment = iterator.next();
            FitnessValue fitnessvalue = (cachedFitnessValues.containsKey(assignment)) ? cachedFitnessValues.get(assignment) : this.evaluate(assignment);
            refreshedCached.put(assignment, fitnessvalue);
        }
        cachedFitnessValues = refreshedCached;
    }

    public Short[] getBestIndividual() {
        double currentGenerationBestFitness = Double.NEGATIVE_INFINITY;
        Short[] bestIndividual = null;
        for (Iterator<Short[]> iterator = cachedFitnessValues.keySet().iterator(); iterator.hasNext(); ) {
            Short[] individual = iterator.next();
            double currentIndividualFitness = evaluate(individual).getValue();
            if (currentIndividualFitness > currentGenerationBestFitness) {
                currentGenerationBestFitness = currentIndividualFitness;
                bestIndividual = individual;
            }
        }
        return bestIndividual;
    }

    public ArrayList<DataAssignment> mapIndividualToSolution(Short[] individual) {
        HashMap<Integer, DataAssignment> deviceAssignments = convertIntoDeviceAssignments(individual);
        return new ArrayList<DataAssignment>(deviceAssignments.values());
    }


    public Short getDeviceId(Device device) {
        return devicesObjects.get(device).shortValue();
    }

    public void setGeneticAssignmentDataRound(GeneticAssignmentRound gar) {
        this.gar = gar;
    }

    public Device getDevice(Integer devId) {
        return devicesId.get(devId);
    }

}
