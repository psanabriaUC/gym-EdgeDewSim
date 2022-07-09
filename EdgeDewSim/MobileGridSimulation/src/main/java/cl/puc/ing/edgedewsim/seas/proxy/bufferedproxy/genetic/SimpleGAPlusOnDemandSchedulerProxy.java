package cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy.genetic;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;
import cl.puc.ing.edgedewsim.mobilegrid.network.Message;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.seas.proxy.DataAssignment;
import cl.puc.ing.edgedewsim.seas.proxy.DataIntensiveScheduler;
import cl.puc.ing.edgedewsim.simulator.Simulation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class SimpleGAPlusOnDemandSchedulerProxy extends SimpleGASchedulerProxy {


    public SimpleGAPlusOnDemandSchedulerProxy(String name, String bufferValue, Simulation simulation) {
        super(name, bufferValue, simulation);
    }

    @Override
    public <T> void onMessageReceived(@NotNull Message<T> message) {
        super.onMessageReceived(message);

        if (message.getData() instanceof Job)
            sendNextJobToNode((Device) message.getSource());
    }

    @Override
    protected void scheduleJobs(ArrayList<DataAssignment> solution) {
        for (DataAssignment da : solution) {
            Device dev = da.getDevice();

            if (!deviceToAssignmentsMap.containsKey(dev))
                deviceToAssignmentsMap.put(dev, da);
            else {
                DataAssignment devAssignment = deviceToAssignmentsMap.get(dev);
                devAssignment.scheduleJobs(da.getAssignedJobs());
            }

            sendNextJobToNode(dev);
        }
    }

    private void sendNextJobToNode(Device dev) {

        DataAssignment deviceAssignment = deviceToAssignmentsMap.get(dev);
        if (deviceAssignment.getAssignedJobs().size() > 0) { //send the next job to the iddle device
            Job job = deviceAssignment.getAssignedJobs().remove(DataIntensiveScheduler.FIRST);
            queueJobTransferring(dev, job);

			/*
			Logger.logEntity(this, "Job assigned to ", job.getJobId() , dev);
			long time=NetworkModel.getModel().send(this, dev, idSend++,  job.getInputSize(), job);
			long currentSimTime = Simulation.getTime();
			JobStatsUtils.transfer(job, dev, time-currentSimTime,currentSimTime);
			*/
        }
    }

}
