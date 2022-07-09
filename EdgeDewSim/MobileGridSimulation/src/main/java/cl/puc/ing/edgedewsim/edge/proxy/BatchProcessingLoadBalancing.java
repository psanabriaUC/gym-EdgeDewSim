package cl.puc.ing.edgedewsim.edge.proxy;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;
import cl.puc.ing.edgedewsim.mobilegrid.jobs.JobStatsUtils;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.mobilegrid.node.SchedulerProxy;
import cl.puc.ing.edgedewsim.seas.proxy.DeviceComparator;
import cl.puc.ing.edgedewsim.simulator.Event;
import cl.puc.ing.edgedewsim.simulator.Logger;
import cl.puc.ing.edgedewsim.simulator.Simulation;

import java.util.HashMap;
import java.util.Objects;

public class BatchProcessingLoadBalancing extends SchedulerProxy {
    private final DeviceComparator comparator;
    private final HashMap<String, Long> assignedJobs;

    public BatchProcessingLoadBalancing(String name, Simulation simulation) {
        super(name, simulation);
        assignedJobs = new HashMap<>();
        comparator = new BatchProcessingComparator(assignedJobs);
    }

    @Override
    public void processEvent(Event event) {
        if (EVENT_END_JOB_ARRIVAL_BURST == event.getEventType())
            return;
        if (event.getEventType() != EVENT_JOB_ARRIVE)
            throw new IllegalArgumentException("Unexpected event");
        Job job = event.getData();
        JobStatsUtils.getInstance(simulation).addJob(job, this);
        Logger.getInstance(simulation).logEntity(this, "Job arrived to edge proxy", Objects.requireNonNull(job).getJobId());
        assignJob(job);
    }

    private void assignJob(Job job) {
        if (this.devices.isEmpty()) {
            JobStatsUtils.getInstance(simulation).rejectJob(job, simulation.getTime());
            Logger.getInstance(simulation).logEntity(this, "Job rejected = " + job.getJobId() + " at " + simulation.getTime() +
                    " simulation time");
        } else {
            Device selectedDevice = null;
            for (Device device : this.devices.values()) {
                if (selectedDevice == null || comparator.compare(device, selectedDevice) <= 0) {
                    selectedDevice = device;
                }
            }

            if (selectedDevice != null) {
                queueJobTransferring(selectedDevice, job);
                assignedJobs.put(selectedDevice.getName(), assignedJobs.get(selectedDevice.getName()) + job.getOps());
            }
        }
    }

    @Override
    public void remove(Device device) {
        super.remove(device);
        assignedJobs.remove(device.getName());
    }

    @Override
    public void addDevice(Device device) {
        super.addDevice(device);
        assignedJobs.put(device.getName(), 0L);
        device.setOnFinishJobListener((device1, job) -> {
            long ops = assignedJobs.get(device1.getName()) - job.getOps();
            if (ops < 0)
                throw new IllegalStateException("Negative remaining OPS");
            assignedJobs.put(device1.getName(), ops);
        });
    }

    @Override
    public boolean runsOnBattery() {
        return false;
    }
}
