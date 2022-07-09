package cl.puc.ing.edgedewsim.seas.proxy;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;
import cl.puc.ing.edgedewsim.mobilegrid.jobs.JobStatsUtils;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.mobilegrid.node.SchedulerProxy;
import cl.puc.ing.edgedewsim.simulator.Event;
import cl.puc.ing.edgedewsim.simulator.Logger;
import cl.puc.ing.edgedewsim.simulator.Simulation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class WeightedRandomProxy extends SchedulerProxy {
    private final LinkedHashMap<Long, Device> weightMap = new LinkedHashMap<>();
    protected int idSend = 0;
    private long totalWeight = 0;


    public WeightedRandomProxy(String name, Simulation simulation) {
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
        Device current = selectDevice();

        Logger.getInstance(simulation).logEntity(this, "Job assigned to ", job.getJobId(), current);
        queueJobTransferring(current, job);

    }

    @Override
    public void addDevice(Device device) {
        super.addDevice(device);
        totalWeight += device.getMIPS();
        Logger.getInstance(simulation).logEntity(this, "totalWeight ", totalWeight);
        weightMap.put(totalWeight, device);
    }

    private Device selectDevice() {
        long targetNumber = ThreadLocalRandom.current().nextLong(totalWeight);
        ArrayList<Long> weights = new ArrayList<>(weightMap.keySet());
        ArrayList<Device> devices = new ArrayList<>(weightMap.values());
        int begin = 0;
        int mid = 0;
        int end = weights.size() - 1;

        while (begin <= end) {
            mid = (begin + end) / 2;

            if (targetNumber <= weights.get(mid) && weights.get(mid) - devices.get(mid).getMIPS() <= targetNumber)
                break;
            else if (targetNumber <= weights.get(mid)) {
                end = mid - 1;
            } else {
                begin = mid + 1;
            }
        }
        return devices.get(mid);
    }

    @Override
    public boolean runsOnBattery() {
        return false;
    }

}
