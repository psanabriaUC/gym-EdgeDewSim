package cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.seas.proxy.DataAssignment;
import cl.puc.ing.edgedewsim.seas.proxy.DataIntensiveScheduler;
import cl.puc.ing.edgedewsim.seas.proxy.DescendingDataAssignmentComparator;
import cl.puc.ing.edgedewsim.seas.proxy.RemainingDataTransferringEvaluator;
import cl.puc.ing.edgedewsim.simulator.Simulation;

import java.util.Collections;
import java.util.Comparator;

public class GreedyBufferedSchedulerProxy extends BufferedSchedulerProxy {

    public GreedyBufferedSchedulerProxy(String name, String bufferValue, Simulation simulation) {
        super(name, bufferValue, simulation);
    }

    @Override
    protected void queueJob(Job job) {
        int dataTransferRequirement = job.getInputSize() + job.getOutputSize();
        boolean inserted = false;
        int queueIndex = 0;
        while (!inserted) {
            if (queueIndex < bufferedJobs.size()) {
                Job currentJob = bufferedJobs.get(queueIndex);
                int currentJobDataTransferRequirement = currentJob.getInputSize() + currentJob.getOutputSize();
                if (currentJobDataTransferRequirement > dataTransferRequirement) {
                    bufferedJobs.add(queueIndex, job);
                    inserted = true;
                } else {
                    queueIndex++;
                }
            } else {
                bufferedJobs.add(job);
                inserted = true;
            }
        }
    }

    @Override
    protected void assignBufferedJobs() {
        //TODO: update node availability and deviceAssignments every time this method is called
        DataAssignment.evaluator = new RemainingDataTransferringEvaluator();
        Comparator<DataAssignment> comp = new DescendingDataAssignmentComparator(DataAssignment.evaluator);
        Collections.sort(totalDataPerDevice, comp);

        for (Job dataJob : bufferedJobs) {
            totalDataPerDevice.get(DataIntensiveScheduler.FIRST).scheduleJob(dataJob);
            Collections.sort(totalDataPerDevice, comp);
        }

        for (DataAssignment deviceAssignment : totalDataPerDevice) {
            Device current = deviceAssignment.getDevice();
            for (Job job : deviceAssignment.getAssignedJobs()) {
                queueJobTransferring(current, job);
            }
        }
    }

}
