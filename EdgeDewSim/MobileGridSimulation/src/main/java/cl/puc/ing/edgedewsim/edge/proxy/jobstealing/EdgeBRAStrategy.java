package cl.puc.ing.edgedewsim.edge.proxy.jobstealing;

import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.seas.proxy.DeviceComparator;
import cl.puc.ing.edgedewsim.seas.proxy.jobstealing.StealerProxy;
import cl.puc.ing.edgedewsim.seas.proxy.jobstealing.StealingStrategy;

import java.util.Collection;

public class EdgeBRAStrategy implements StealingStrategy {
    @Override
    public Device getVictim(StealerProxy sp, Device stealer) {
        Collection<Device> devices = sp.getDevices();

        if (devices.size() == 0)
            return null;

        DeviceComparator comparator = sp.getDevComp();
        Device current = null;

        for (Device next : devices) {
            if (!next.runsOnBattery()) {
                if (current == null) {
                    current = next;
                } else {
                    if ((comparator.compare(next, current) > 0) && (next.getWaitingJobs() > 0) && (next != stealer))
                        current = next;
                }
            }
        }

        if (current == null) return null;
        if (current.getWaitingJobs() == 0) return null;
        if (current == stealer) return null;

        return current;
    }
}
