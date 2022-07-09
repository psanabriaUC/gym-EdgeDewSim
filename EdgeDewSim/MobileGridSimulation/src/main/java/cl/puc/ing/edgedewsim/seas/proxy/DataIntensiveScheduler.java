package cl.puc.ing.edgedewsim.seas.proxy;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;
import cl.puc.ing.edgedewsim.mobilegrid.jobs.JobStatsUtils;
import cl.puc.ing.edgedewsim.mobilegrid.network.Message;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.mobilegrid.node.SchedulerProxy;
import cl.puc.ing.edgedewsim.simulator.Event;
import cl.puc.ing.edgedewsim.simulator.Logger;
import cl.puc.ing.edgedewsim.simulator.Simulation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public abstract class DataIntensiveScheduler extends SchedulerProxy {

    protected static final int FIRST = 0;
    protected int idSend = 0;
    protected HashMap<Job, DataAssignment> jobAssignments = null;
    protected HashMap<Device, DataAssignment> deviceToAssignmentsMap = null;
    protected ArrayList<DataAssignment> totalDataPerDevice = null;
    protected boolean device_assignments_initialized = false;

    public DataIntensiveScheduler(String name, Simulation simulation) {
        super(name, simulation);
        jobAssignments = new HashMap<Job, DataAssignment>();
        totalDataPerDevice = new ArrayList<DataAssignment>();
        deviceToAssignmentsMap = new HashMap<Device, DataAssignment>();
    }

    @Override
    public boolean runsOnBattery() {
        return false;
    }

    @Override
    public void processEvent(Event event) {
        if (EVENT_JOB_ARRIVE != event.getEventType()) throw new IllegalArgumentException("Unexpected event");
        if (!device_assignments_initialized) initializeDeviceAssignments();

        Job j = (Job) event.getData();
        JobStatsUtils.getInstance(simulation).addJob(j, this);
        Logger.getInstance(simulation).logEntity(this, "Job arrived ", Objects.requireNonNull(j).getJobId());

        assignJob(j);
    }

    protected abstract void assignJob(Job job);

    protected void initializeDeviceAssignments() {
        for (Device device : devices.values()) {
            DataAssignment dataAssignment = new DataAssignment(device);
            totalDataPerDevice.add(dataAssignment);
            deviceToAssignmentsMap.put(device, dataAssignment);
        }
        this.device_assignments_initialized = true;
    }

    @Override
    public void remove(Device device) {
        DataAssignment dataAssignment = deviceToAssignmentsMap.get(device);
        if (totalDataPerDevice.contains(dataAssignment))
            totalDataPerDevice.get(totalDataPerDevice.indexOf(dataAssignment));
        deviceToAssignmentsMap.remove(device);
        super.remove(device);
    }

    @Override
    public <T> void onMessageReceived(@NotNull Message<T> message) {
        if (message.getData() instanceof Job) { //Update sent and received data of the corresponding node
            Job jobResult = (Job) message.getData();
            DataAssignment assignment = jobAssignments.get(jobResult);
            if (assignment != null) {
                assignment.setMbToBeReceived(assignment.getMbToBeReceived() - (double) jobResult.getInputSize() /
                        (double) (1024 * 1024));
                assignment.setMbToBeSend(assignment.getMbToBeSend() - (double) jobResult.getOutputSize() /
                        (double) (1024 * 1024));
            }
        }
        super.onMessageReceived(message);
    }
}
