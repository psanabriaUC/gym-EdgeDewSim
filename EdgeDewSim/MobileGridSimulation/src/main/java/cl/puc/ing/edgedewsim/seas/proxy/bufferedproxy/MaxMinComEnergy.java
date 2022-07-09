package cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.seas.proxy.DataAssignment;
import cl.puc.ing.edgedewsim.seas.proxy.DescendingDataAssignmentComparator;
import cl.puc.ing.edgedewsim.seas.proxy.RemainingDataTransferringEvaluator;
import cl.puc.ing.edgedewsim.simulator.Simulation;

import java.util.Collections;
import java.util.Comparator;

public class MaxMinComEnergy extends BufferedSchedulerProxy {

    public MaxMinComEnergy(String name, String bufferValue, Simulation simulation) {
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
                if (currentJobDataTransferRequirement < dataTransferRequirement) {
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
        DataAssignment.evaluator = new RemainingDataTransferringEvaluator();
        Comparator<DataAssignment> comp = new DescendingDataAssignmentComparator(DataAssignment.evaluator);
        Collections.sort(totalDataPerDevice, comp);

        for (Job dataJob : bufferedJobs) {
            int assignment = -1;
            double assignment_remaining_energy = -1;

            for (int index = 0; index < totalDataPerDevice.size(); index++) {
                DataAssignment da = totalDataPerDevice.get(index);
                double job_energy = da.getDevice().getEnergyWasteInTransferringData(dataJob.getInputSize());
                job_energy += da.getDevice().getEnergyWasteInTransferringData(dataJob.getOutputSize());
                double rem_energy = DataAssignment.evaluator.eval(da) - job_energy;
                if (rem_energy > 0 && rem_energy > assignment_remaining_energy) {
                    assignment = index;
                    assignment_remaining_energy = rem_energy;
                }
            }
            if (assignment != -1) {
                totalDataPerDevice.get(assignment).scheduleJob(dataJob);
                Collections.sort(totalDataPerDevice, comp);
            }
        }

        for (DataAssignment deviceAssignment : totalDataPerDevice) {
            Device current = deviceAssignment.getDevice();
            for (Job job : deviceAssignment.getAssignedJobs()) {
                queueJobTransferring(current, job);

				/*
				JobStatsUtils.setJobAssigned(job);
				Logger.logEntity(this, "Job assigned to ", job.getJobId(), current);
				current.incrementIncomingJobs();

                queueJobTransferring(current, job);
                */

                /*

				long subMessagesCount = (long) Math.ceil(job.getInputSize() / (double) MESSAGE_SIZE);
				long lastMessageSize = job.getInputSize() - (subMessagesCount - 1) * MESSAGE_SIZE;
				TransferInfo transferInfo = new TransferInfo(current, job, subMessagesCount, 0,
						lastMessageSize);




				transfersPending.put(job.getJobId(), transferInfo);

				if (prev == null) {
					idSend++;
					long messageSize = transferInfo.messagesCount == 1 ? transferInfo.lastMessageSize : MESSAGE_SIZE;

					 // temporal cast (int)messageSize, message size must be long

					long time = NetworkModel.getModel().send(this, current, job.getJobId(), (int) messageSize, job);
					long currentSimTime = Simulation.getTime();
					JobStatsUtils.transfer(job, current, time - currentSimTime, currentSimTime);
				} else
					prev.nextJobId = job.getJobId();
				prev = transferInfo;

				*/
            }
        }
    }
}
