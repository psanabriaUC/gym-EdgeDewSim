package cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;
import cl.puc.ing.edgedewsim.mobilegrid.jobs.JobStatsUtils;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.mobilegrid.node.SchedulerProxy;
import cl.puc.ing.edgedewsim.seas.node.DefaultFiniteBatteryManager;
import cl.puc.ing.edgedewsim.seas.proxy.RSSIDataJoulesEvaluator;
import cl.puc.ing.edgedewsim.simulator.Simulation;

import java.util.ArrayList;
import java.util.HashMap;

public class BacktrackingBasedProxy extends BufferedSchedulerProxy {

    private final HashMap<Short, ArrayList<Short>> currentAssignment;
    private final HashMap<Short, Double> accDataPerDevice;
    private final HashMap<Short, Short[]> bestAssignment;
    private final HashMap<Short, Device> deviceIds;
    private final RSSIDataJoulesEvaluator energyEvaluator = new RSSIDataJoulesEvaluator();
    private double bestFitness = 0;
    private double currentFitnessValue = 0;
    private short deviceQuantity = 0;

    public BacktrackingBasedProxy(String name, String bufferValue, Simulation simulation) {
        super(name, bufferValue, simulation);
        currentAssignment = new HashMap<>();
        bestAssignment = new HashMap<>();
        accDataPerDevice = new HashMap<>();
        deviceIds = new HashMap<>();
    }

    @Override
    protected void queueJob(Job job) {
        bufferedJobs.add(job);
    }

    @Override
    protected void assignBufferedJobs() {
        mapDevicesWithIDs();
        initializeStructures();
        Long init = System.currentTimeMillis();
        generateBestAssignment((short) 0);
        Long elapsed = init - System.currentTimeMillis();
        System.out.println("Backtracking elapsed time:" + JobStatsUtils.getInstance(simulation).timeToMinutes(elapsed));
        assignJobs();
    }

    private void mapDevicesWithIDs() {
        deviceQuantity = 0;
        for (Device device : devices.values()) {
            deviceIds.put(deviceQuantity, device);
            deviceQuantity++;
        }
    }

    private void initializeStructures() {
        for (short devNmb = 0; devNmb < deviceQuantity; devNmb++) {
            currentAssignment.put(devNmb, new ArrayList<Short>());
            accDataPerDevice.put(devNmb, 0d);
        }
    }

    private void assignJobs() {
        Short[] jobIds;
        for (Short aShort : bestAssignment.keySet()) {
            jobIds = bestAssignment.get(aShort);
            sendJobsToDevice(deviceIds.get(aShort), jobIds);
        }
    }

    private void sendJobsToDevice(Device device, Short[] jobIds) {
        for (int jobIndex = 0; jobIndex < jobIds.length; jobIndex++) {
            Job job = bufferedJobs.get(jobIndex);
            queueJobTransferring(device, job);
			/*
			Logger.logEntity(this, "Job assigned to ", job.getJobId() ,device);
			long time=NetworkModel.getModel().send(this, device, idSend++,  job.getInputSize(), job);
			long currentSimTime = Simulation.getTime();
			JobStatsUtils.transfer(job, device, time-currentSimTime,currentSimTime);
			*/
        }

    }

    private void generateBestAssignment(Short nextJob) {
        if (nextJob == bufferedJobs.size()) {
            currentFitnessValue = evaluateCurrentSolution();
            if (currentFitnessValue > bestFitness) {
                saveCurrentAssignment();
            }
        } else {
            for (short devNmb = 0; devNmb < deviceQuantity; devNmb++) {
                ArrayList<Short> jobs = currentAssignment.get(devNmb);
                jobs.add(nextJob);
                double jobDataInMb = (((bufferedJobs.get(nextJob).getInputSize() + bufferedJobs.get(nextJob).getOutputSize()) / 1024d) / 1024d);
                Double accData = accDataPerDevice.get(devNmb);
                accDataPerDevice.put(devNmb, accData + jobDataInMb);

                generateBestAssignment((short) (nextJob + 1));

                int jobCount = jobs.size();
                if (jobCount > 0) jobs.remove(jobCount - 1);
                accDataPerDevice.put(devNmb, accData - jobDataInMb);
            }
        }
    }

    private void saveCurrentAssignment() {
        bestFitness = currentFitnessValue;
        bestAssignment.clear();
        for (Short devNmb : currentAssignment.keySet()) {
            ArrayList<Short> assignedJobs = currentAssignment.get(devNmb);
            Short[] assignmentsToSave = new Short[assignedJobs.size()];
            assignmentsToSave = assignedJobs.toArray(assignmentsToSave);
            bestAssignment.put(devNmb, assignmentsToSave);
        }
    }

    private double evaluateCurrentSolution() {
        double totalJoulesConsumed = 0;
        int totalJobsTransferred = 0;
        for (short devNmb = 0; devNmb < deviceQuantity; devNmb++) {
            //calculating energy consumed
            Device device = deviceIds.get(devNmb);
            double devJoulesConsumed = energyEvaluator.getValue(accDataPerDevice.get(devNmb), device);
            totalJoulesConsumed += devJoulesConsumed;

            //calculating jobs transfered
            double devicePerOfAvailableEnergy = SchedulerProxy.getProxyInstance(simulation).getLastReportedSOC(device) /
                    DefaultFiniteBatteryManager.PROFILE_ONE_PERCENT_REPRESENTATION;
            double deviceJoulesAvailable = (devicePerOfAvailableEnergy * device.getTotalBatteryCapacityInJoules() /
                    (double) 100);
            if ((deviceJoulesAvailable - devJoulesConsumed) >= (0d)) {
                totalJobsTransferred += currentAssignment.get(devNmb).size();
            }
        }

        return (((double) totalJobsTransferred) / totalJoulesConsumed);
    }
}
