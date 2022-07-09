package cl.puc.ing.edgedewsim.seas.proxy;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;
import cl.puc.ing.edgedewsim.mobilegrid.jobs.JobStatsUtils;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.mobilegrid.node.SchedulerProxy;
import cl.puc.ing.edgedewsim.simulator.Event;
import cl.puc.ing.edgedewsim.simulator.Logger;
import cl.puc.ing.edgedewsim.simulator.Simulation;

import java.util.Collection;
import java.util.Objects;

public class WeightedRRProxy extends SchedulerProxy {

    private int next = 0;
    private int currentAvailableJobs = 0;
    private boolean first = true;

    public WeightedRRProxy(String name, Simulation simulation) {
        super(name, simulation);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void processEvent(Event event) {
        if (EVENT_END_JOB_ARRIVAL_BURST == event.getEventType())
            return;
        if (EVENT_JOB_ARRIVE != event.getEventType()) throw new IllegalArgumentException("Unexpected event");
        Job job = event.getData();
        JobStatsUtils.getInstance(simulation).addJob(job, this);
        Logger.getInstance(simulation).logEntity(this, "Job arrived ", Objects.requireNonNull(job).getJobId());

        if (devices.size() == 0) {
            JobStatsUtils.getInstance(simulation).rejectJob(job, simulation.getTime());
            Logger.getInstance(simulation).logEntity(this, "Job rejected = " + job.getJobId() + " at " + simulation.getTime() +
                    " simulation time");
            return;
        }


        Collection<Device> deviceList = this.devices.values();
        Device[] devArray = new Device[deviceList.size()];
        deviceList.toArray(devArray);
        Device current = devArray[next];

        if (first) {
            currentAvailableJobs = getDeviceMaxJobs(current);
            first = false;
        }

        if (currentAvailableJobs == 0) {
            next++;
            if (next >= devices.size()) next = 0;
            Device nextDevice = devArray[next];
            currentAvailableJobs = getDeviceMaxJobs(nextDevice);
        }
        currentAvailableJobs--;
        Logger.getInstance(simulation).logEntity(this, "Job assigned to ", job.getJobId(), current);

        queueJobTransferring(current, job);
        // NetworkModel.getModel().send(this, current, idSend++, job.getInputSize(), job);
    }

    private int getDeviceMaxJobs(Device device) {
        int maxJobs;
        if (device.getMIPS() < 10000000) {
            maxJobs = 1;
        } else if (device.getMIPS() < 75000000) {
            maxJobs = 7;
        } else if (device.getMIPS() < 100000000) {
            maxJobs = 10;
        } else {
            maxJobs = 20;
        }
        return maxJobs;
    }

    @Override
    public boolean runsOnBattery() {
        return false;
    }

}
