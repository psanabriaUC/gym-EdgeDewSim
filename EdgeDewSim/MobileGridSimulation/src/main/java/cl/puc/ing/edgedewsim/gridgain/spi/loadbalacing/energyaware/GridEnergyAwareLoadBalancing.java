package cl.puc.ing.edgedewsim.gridgain.spi.loadbalacing.energyaware;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;
import cl.puc.ing.edgedewsim.mobilegrid.jobs.JobStatsUtils;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.mobilegrid.node.SchedulerProxy;
import cl.puc.ing.edgedewsim.seas.proxy.DefaultSEASComparator;
import cl.puc.ing.edgedewsim.seas.proxy.DeviceComparator;
import cl.puc.ing.edgedewsim.simulator.Event;
import cl.puc.ing.edgedewsim.simulator.Logger;
import cl.puc.ing.edgedewsim.simulator.Simulation;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class GridEnergyAwareLoadBalancing extends SchedulerProxy {

    protected DeviceComparator devComp = new DefaultSEASComparator();

    public GridEnergyAwareLoadBalancing(String name, Simulation simulation) {
        super(name, simulation);
    }

    @Override
    public void processEvent(@NotNull Event event) {
        if (EVENT_END_JOB_ARRIVAL_BURST == event.getEventType())
            return;
        if (EVENT_JOB_ARRIVE != event.getEventType()) throw new IllegalArgumentException("Unexpected event");
        Job job = event.getData();
        JobStatsUtils.getInstance(simulation).addJob(job, this);
        Logger.getInstance(simulation).logEntity(this, "Job arrived ", Objects.requireNonNull(job).getJobId());
        assignJob(job);
    }

    /**
     * Assigns a job to the device in the grid with the highest node rank according to the SEAS algorithm.
     *
     * @param job The job to assign.
     */
    protected void assignJob(Job job) {
        if (this.devices.values().isEmpty()) {
            JobStatsUtils.getInstance(simulation).rejectJob(job, simulation.getTime());
            Logger.getInstance(simulation).logEntity(this, "Job rejected = " + job.getJobId() + " at " + simulation.getTime() +
                    " simulation time");
        } else {
            Device selectedDevice = null;
            for (Device device : this.devices.values()) {
                if (selectedDevice == null || this.devComp.compare(device, selectedDevice) > 0) {
                    selectedDevice = device;
                }
            }
            queueJobTransferring(selectedDevice, job);
        }
    }

    public DeviceComparator getDevComp() {
        return this.devComp;
    }

    public void setDevComp(DeviceComparator devComp) {
        this.devComp = devComp;
        Logger.getInstance(simulation).logEntity(this, "Using Comparator", devComp.getClass().getName());
    }

    @Override
    public boolean runsOnBattery() {
        return false;
    }
}
