package cl.puc.ing.edgedewsim.edge.proxy;

import cl.puc.ing.edgedewsim.mobilegrid.node.BatteryManager;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.seas.proxy.DeviceComparator;

import java.util.HashMap;

public class BatchProcessingComparator extends DeviceComparator {
    private final HashMap<String, Long> jobsLoad;

    public BatchProcessingComparator(HashMap<String, Long> jobsLoad) {
        this.jobsLoad = jobsLoad;
    }

    @Override
    public double getValue(Device device) {
        long ops = jobsLoad.get(device.getName());
        long flops = device.getMIPS();

        return (double) ops * BatteryManager.PROFILE_ONE_PERCENT_REPRESENTATION / flops * device.getBatteryLevel();
    }
}
