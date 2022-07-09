package cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.seas.proxy.DataAssignment;
import cl.puc.ing.edgedewsim.seas.proxy.DescendingDataAssignmentComparator;
import cl.puc.ing.edgedewsim.seas.proxy.RemainingDataTransferringEvaluator;
import cl.puc.ing.edgedewsim.simulator.Simulation;

import java.util.Collections;
import java.util.Comparator;

public class DataAnalyticBufferedSchedulerProxy extends BufferedSchedulerProxy {

    public DataAnalyticBufferedSchedulerProxy(String name, String bufferValue, Simulation simulation) {
        super(name, bufferValue, simulation);
        DataAssignment.evaluator = new RemainingDataTransferringEvaluator();
    }

    @Override
    protected void queueJob(Job job) {
        bufferedJobs.add(job);
    }

    @Override
    protected void assignBufferedJobs() {
        //TODO: update node availability and deviceAssignments every time this method is called
        DataAssignment.evaluator = new RemainingDataTransferringEvaluator();
        Comparator<DataAssignment> comp = new DescendingDataAssignmentComparator(DataAssignment.evaluator);
        Collections.sort(totalDataPerDevice, comp);

        for (Job dataJob : bufferedJobs) {
            int assignment = FIRST;
            while (assignment < totalDataPerDevice.size() && DataAssignment.evaluator.eval(totalDataPerDevice.get(assignment)) <= 0)
                assignment++;
            if (assignment < totalDataPerDevice.size()) {
                totalDataPerDevice.get(assignment).scheduleJob(dataJob);
                Collections.sort(totalDataPerDevice, comp);
            } else
                break;
        }

        for (DataAssignment deviceAssignment : totalDataPerDevice) {
            Device current = deviceAssignment.getDevice();
            for (Job job : deviceAssignment.getAssignedJobs()) {
                queueJobTransferring(current, job);
				/*
				Logger.logEntity(this, "Job assigned to ", job.getJobId() ,current);
				long time=NetworkModel.getModel().send(this, current, idSend++,  job.getInputSize(), job);
				long currentSimTime = Simulation.getTime();
				JobStatsUtils.transfer(job, current, time-currentSimTime,currentSimTime);
				*/
            }
        }
    }

}
