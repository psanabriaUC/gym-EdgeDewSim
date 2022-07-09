package cl.puc.ing.edgedewsim.seas.proxy;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;
import cl.puc.ing.edgedewsim.mobilegrid.jobs.JobStatsUtils;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.mobilegrid.node.SchedulerProxy;
import cl.puc.ing.edgedewsim.simulator.Event;
import cl.puc.ing.edgedewsim.simulator.Logger;
import cl.puc.ing.edgedewsim.simulator.Simulation;

import java.util.Collection;

public class RRProxy extends SchedulerProxy {

    private int next = 0;

    public RRProxy(String name, Simulation simulation) {
        super(name, simulation);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void processEvent(Event event) {
        if (EVENT_JOB_ARRIVE != event.getEventType()) throw new IllegalArgumentException("Unexpected event");
        Job job = event.getData();
        JobStatsUtils.getInstance(simulation).addJob(job, this);
        Logger.getInstance(simulation).logEntity(this, "Job arrived ", job.getJobId());

        if (devices.size() == 0) {
            JobStatsUtils.getInstance(simulation).rejectJob(job, simulation.getTime());
            Logger.getInstance(simulation).logEntity(this, "Job rejected = " + job.getJobId() + " at " + simulation.getTime() +
                    " simulation time");
            return;
        }

        if (next >= devices.size()) next = 0;

        Collection<Device> deviceList = this.devices.values();
        Device[] devArray = new Device[deviceList.size()];
        deviceList.toArray(devArray);
        Device current = devArray[next];
        next++;
        Logger.getInstance(simulation).logEntity(this, "Job assigned to ", job.getJobId(), current);

        queueJobTransferring(current, job);
        // NetworkModel.getModel().send(this, current, idSend++, job.getInputSize(), job);
    }


    @Override
    public boolean runsOnBattery() {
        return false;
    }

}
