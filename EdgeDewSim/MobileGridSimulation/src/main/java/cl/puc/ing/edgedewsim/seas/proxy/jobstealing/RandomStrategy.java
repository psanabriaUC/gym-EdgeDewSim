package cl.puc.ing.edgedewsim.seas.proxy.jobstealing;

import cl.puc.ing.edgedewsim.mobilegrid.node.Device;

import java.util.Collection;

public class RandomStrategy implements StealingStrategy {

    @Override
    public Device getVictim(StealerProxy sp, Device stealer) {
        Collection<Device> devs = sp.getDevices();
        Device[] devicesArray = new Device[devs.size()];
        devs.toArray(devicesArray);
        if (devs.size() == 1) return null;
        Device d = stealer;
        int tries = 0;
        while (tries > 1000) {
            //TODO revisar
            d = devicesArray[((int) (devs.size() * Math.random()))];
            tries++;
            if (d == stealer) return null;
        }
        return d;
    }

}
