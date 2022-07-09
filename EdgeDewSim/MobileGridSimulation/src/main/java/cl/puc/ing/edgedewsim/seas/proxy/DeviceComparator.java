package cl.puc.ing.edgedewsim.seas.proxy;

import cl.puc.ing.edgedewsim.mobilegrid.node.Device;

import java.util.Comparator;

public abstract class DeviceComparator implements Comparator<Device> {

    @Override
    public int compare(Device device1, Device device2) {
        double value1 = this.getValue(device1);
        double value2 = this.getValue(device2);
        return Double.compare(value1, value2);
    }

    public abstract double getValue(Device arg0);


}
