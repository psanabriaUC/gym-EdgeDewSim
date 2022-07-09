package cl.puc.ing.edgedewsim.edge.proxy.jobstealing;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;
import cl.puc.ing.edgedewsim.mobilegrid.jobs.JobStatsUtils;
import cl.puc.ing.edgedewsim.mobilegrid.network.Message;
import cl.puc.ing.edgedewsim.mobilegrid.network.UpdateMessage;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.mobilegrid.node.SchedulerProxy;
import cl.puc.ing.edgedewsim.seas.proxy.DefaultSEASComparator;
import cl.puc.ing.edgedewsim.seas.proxy.jobstealing.StealerProxy;
import cl.puc.ing.edgedewsim.simulator.Logger;
import cl.puc.ing.edgedewsim.simulator.Simulation;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class EdgeStealerProxy extends StealerProxy {
    private final HashMap<String, Long> assignedJobs;

    public EdgeStealerProxy(String name, Simulation simulation) {
        super(name, simulation);
        assignedJobs = new HashMap<>();
        devComp = new DefaultSEASComparator();
    }

    @Override
    protected void assignJob(Job job) {
        if (this.devices.isEmpty()) {
            JobStatsUtils.getInstance(simulation).rejectJob(job, simulation.getTime());
            Logger.getInstance(simulation).logEntity(this, "Job rejected = " + job.getJobId() + " at " + simulation.getTime() +
                    " simulation time");
        } else {
            Device selectedDevice = null;
            for (Device device : this.devices.values()) {
                if (!device.runsOnBattery() && (selectedDevice == null || devComp.compare(device, selectedDevice) <= 0)) {
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
    public boolean runsOnBattery() {
        return false;
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
    }

    @Override
    public <T> void onMessageReceived(@NotNull Message<T> message) {
        super.onMessageReceived(message);
        if (message.getData() instanceof UpdateMessage) {
            UpdateMessage msg = (UpdateMessage) message.getData();
            Device device = devices.get(msg.getNodeId());

            if (device.getWaitingJobs() == 0) {
                ((StealerProxy) SchedulerProxy.getProxyInstance(simulation)).steal(device);
            }
        }
    }
}
