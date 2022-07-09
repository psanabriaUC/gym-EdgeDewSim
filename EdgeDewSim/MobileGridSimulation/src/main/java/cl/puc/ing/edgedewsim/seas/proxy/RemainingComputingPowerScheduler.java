package cl.puc.ing.edgedewsim.seas.proxy;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;
import cl.puc.ing.edgedewsim.mobilegrid.jobs.JobStatsUtils;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.mobilegrid.node.SchedulerProxy;
import cl.puc.ing.edgedewsim.simulator.Event;
import cl.puc.ing.edgedewsim.simulator.Logger;
import cl.puc.ing.edgedewsim.simulator.Simulation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

public class RemainingComputingPowerScheduler extends SchedulerProxy {

    /**
     * stores the max flop that a device type (identified by flops) is able to execute
     */
    private final HashMap<Long, Long> maxExecFlop = new HashMap<Long, Long>() {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        {
            put(61665000L, 1615086267840L);//A100
            put(17070000L, 850153716690L);//viewpad
            put(7602000L, 267025031658L);//i5500
        }
    };


    private int currentDeviceIDIndex = 0;
    private long maxDeviceExecPower = Long.MAX_VALUE;
    private int maxDevices;
    private String[] devicesIDs = null;

    public RemainingComputingPowerScheduler(String name, Simulation simulation) {
        super(name, simulation);
    }

    @Override
    public boolean runsOnBattery() {
        // TODO Auto-generated method stub
        return false;
    }

    private Device getDevice(long jobOps) {
        if (maxDeviceExecPower < 0) { //means that the device computing capacity is exceeded
            if (currentDeviceIDIndex + 1 == maxDevices) //means that there are no more candidates nodes with remaining computing capacity
                return null;
            else { //select the next device in the list of candidates
                currentDeviceIDIndex++;
                maxDeviceExecPower = maxExecFlop.get(devices.get(devicesIDs[currentDeviceIDIndex]).getMIPS());
            }
        }
        maxDeviceExecPower -= jobOps;
        return devices.get(devicesIDs[currentDeviceIDIndex]);
    }

    @Override
    public void processEvent(Event event) {
        if (EVENT_JOB_ARRIVE != event.getEventType()) throw new IllegalArgumentException("Unexpected event");
        Job job = event.getData();
        JobStatsUtils.getInstance(simulation).addJob(job, this);
        Logger.getInstance(simulation).logEntity(this, "Job arrived ", Objects.requireNonNull(job).getJobId());

        if (devicesIDs == null) {
            maxDevices = devices.size();
            devicesIDs = new String[maxDevices];
            devices.keySet().toArray(devicesIDs);
            Arrays.sort(devicesIDs);
            maxDeviceExecPower = maxExecFlop.get(devices.get(devicesIDs[currentDeviceIDIndex]).getMIPS());
        }

        Device dev = getDevice(job.getOps());
        if (dev != null) {
            queueJobTransferring(dev, job);
        } else {
            JobStatsUtils.getInstance(simulation).rejectJob(job, simulation.getTime());
            Logger.getInstance(simulation).logEntity(this, "Job rejected = " + job.getJobId() + " at " + simulation.getTime() +
                    " simulation time");
        }

    }

}
