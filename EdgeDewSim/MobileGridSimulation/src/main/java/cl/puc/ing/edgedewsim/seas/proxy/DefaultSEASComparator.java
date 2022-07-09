package cl.puc.ing.edgedewsim.seas.proxy;

import cl.puc.ing.edgedewsim.mobilegrid.node.Device;

/**
 * SEAS node rank implementation. Devices with a higher rank are given priority when assigning jobs.
 * For more information, see: <a href="https://link.springer.com/article/10.1007/s10723-016-9387-6">
 * A Two-Phase Energy-Aware Scheduling Approach for CPU-Intensive Jobs in Mobile Grids</a>.
 */
public class DefaultSEASComparator extends DeviceComparator {

    @Override
    public double getValue(Device device) {
        double mips = device.getMIPS();
        double uptime = device.getEstimatedUptime();
        double nJobs = device.getNumberOfJobs() + 1;
        return (mips * uptime) / nJobs;
    }


}
