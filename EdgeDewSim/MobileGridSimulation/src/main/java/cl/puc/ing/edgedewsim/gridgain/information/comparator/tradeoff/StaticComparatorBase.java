package cl.puc.ing.edgedewsim.gridgain.information.comparator.tradeoff;

import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.mobilegrid.node.SchedulerProxy;
import cl.puc.ing.edgedewsim.seas.proxy.DeviceComparator;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class StaticComparatorBase extends DeviceComparator {

    protected Map<Long, Double> properties;

    public StaticComparatorBase(String propFile) {
        properties = new HashMap<>();
        try {
            Properties properties = new Properties();
            properties.load(new FileInputStream(propFile));
            for (String property : properties.stringPropertyNames())
                this.properties.put(Long.parseLong(property), Double.parseDouble(properties.getProperty(property)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public double getValue(Device device) {
        double nJobs = SchedulerProxy.getProxyInstance(device.getSimulation()).getIncomingJobs(device) + device.getNumberOfJobs() + 1;
        return ((double) SchedulerProxy.getProxyInstance(device.getSimulation()).getLastReportedSOC(device)) / properties.get(device.getMIPS()) / nJobs;
    }
}
