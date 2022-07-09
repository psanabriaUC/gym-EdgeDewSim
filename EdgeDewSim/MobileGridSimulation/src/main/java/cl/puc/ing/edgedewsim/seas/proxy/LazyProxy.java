package cl.puc.ing.edgedewsim.seas.proxy;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;
import cl.puc.ing.edgedewsim.mobilegrid.jobs.JobStatsUtils;
import cl.puc.ing.edgedewsim.mobilegrid.network.Message;
import cl.puc.ing.edgedewsim.mobilegrid.network.UpdateMessage;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.mobilegrid.node.Node;
import cl.puc.ing.edgedewsim.mobilegrid.node.SchedulerProxy;
import cl.puc.ing.edgedewsim.simulator.Entity;
import cl.puc.ing.edgedewsim.simulator.Event;
import cl.puc.ing.edgedewsim.simulator.Logger;
import cl.puc.ing.edgedewsim.simulator.Simulation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * Scheduler that assigns a single {@link Job} to every {@link Entity} in the grid, then waits until a device finishes
 * executing his job before re-assigning it a new one.
 */
public class LazyProxy extends SchedulerProxy {

    private static final int FIRST = 0;
    protected ArrayList<Job> inQueueJobs = null;
    protected ArrayList<Device> idleDevices = null;
    protected ArrayList<Long> startIdleTimes = null;

    public LazyProxy(String name, Simulation simulation) {
        super(name, simulation);
        inQueueJobs = new ArrayList<>();
        idleDevices = new ArrayList<>();
        startIdleTimes = new ArrayList<>();
    }

    @Override
    public void onDeviceFail(Node e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void incomingData(@NotNull Node scr, int id) {
        // TODO Auto-generated method stub

    }

    @Override
    public <T> void onMessageReceived(@NotNull Message<T> message) {
        if (message.getData() instanceof Job) {
            Job jobResult = (Job) message.getData();
            JobStatsUtils.getInstance(simulation).successTransferBack(jobResult);
            if (!inQueueJobs.isEmpty()) {
                Job job = inQueueJobs.remove(FIRST);
                queueJobTransferring((Device) message.getSource(), job);
				/*
				Logger.logEntity(this, "Job assigned to ", job.getJobId() ,scr);
				long time=NetworkModel.getModel().send(this, scr, idSend++,  job.getInputSize(), job);
				long currentSimTime = Simulation.getTime();
				JobStatsUtils.transfer(job, scr, time-currentSimTime,currentSimTime);
				*/
            } else {
                idleDevices.add((Device) message.getSource());
                startIdleTimes.add(simulation.getTime());
            }
        } else if (message.getData() instanceof UpdateMessage) {
            UpdateMessage updateMessage = (UpdateMessage) message.getData();
            Device device = devices.get(updateMessage.getNodeId());
            updateDeviceSOC(device, updateMessage.getPercentageOfRemainingBattery());
            JobStatsUtils.getInstance(simulation).registerUpdateMessage(message.getSource(), updateMessage);
        }

    }

    @Override
    public <T> void onMessageSentAck(@NotNull Message<T> message) {
        // TODO Auto-generated method stub

    }

    @Override
    public <T> void fail(@NotNull Message<T> message) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isOnline() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public void startTransfer(@NotNull Node dst, int id, @Nullable Object data) {
        // TODO Auto-generated method stub

    }

    @Override
    public void failReception(@NotNull Node scr, int id) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean runsOnBattery() {
        return false;
    }

    @Override
    public void processEvent(Event event) {
        if (EVENT_JOB_ARRIVE != event.getEventType()) throw new IllegalArgumentException("Unexpected event");
        Job job = (Job) event.getData();
        JobStatsUtils.getInstance(simulation).addJob(job, this);
        Logger.getInstance(simulation).logEntity(this, "Job arrived ", job.getJobId());
        inQueueJobs.add(job);
        assignJob();
    }

    private void assignJob() {
        if (!idleDevices.isEmpty() && !inQueueJobs.isEmpty()) {
            Device device = idleDevices.remove(FIRST);
            Job job = inQueueJobs.remove(FIRST);
            queueJobTransferring(device, job);
        }
    }

    @Override
    public void remove(Device device) {
        this.devices.remove(device.getName());
        int deviceIndex = idleDevices.indexOf(device);
        if (deviceIndex > -1) {
            idleDevices.remove(deviceIndex);
            startIdleTimes.remove(deviceIndex);
        }
    }

    @Override
    public void addDevice(Device device) {
        this.devices.put(device.getName(), device);
        idleDevices.add(device);
        startIdleTimes.add(simulation.getTime());
    }

}
