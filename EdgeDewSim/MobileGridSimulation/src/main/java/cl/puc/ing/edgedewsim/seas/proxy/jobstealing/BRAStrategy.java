package cl.puc.ing.edgedewsim.seas.proxy.jobstealing;

import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.seas.proxy.DeviceComparator;

import java.util.Collection;
import java.util.Iterator;

public class BRAStrategy implements StealingStrategy {

    @Override
    public Device getVictim(StealerProxy sp, Device stealer) {
        Collection<Device> devices = sp.getDevices();
        if (devices.size() == 0) return null;
        DeviceComparator comparator = sp.getDevComp();
        Iterator<Device> iterator = devices.iterator();
        Device current = iterator.next();
        for (Device next : devices)
            if ((comparator.compare(next, current) > 0) && (next.getWaitingJobs() > 0) && (next != stealer))
                current = next;
        if (current.getWaitingJobs() == 0) return null;
        if (current == stealer) return null;
        return current;
    }

}
