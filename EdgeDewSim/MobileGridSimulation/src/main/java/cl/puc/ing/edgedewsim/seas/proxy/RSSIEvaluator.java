package cl.puc.ing.edgedewsim.seas.proxy;

import cl.puc.ing.edgedewsim.mobilegrid.node.Device;

abstract class RSSIEvaluator implements DataAssignmentEvaluatorIF {

    protected double getRSSIEnergyPercentageConsumption(Device device) {
        switch (device.getWifiRSSI()) {
            case -50:
                return 0.0018648 * 100 / device.getTotalBatteryCapacityInJoules();
            case -80:
                return 0.0022644 * 100 / device.getTotalBatteryCapacityInJoules();
            case -85:
                return 0.0033 * 100 / device.getTotalBatteryCapacityInJoules();
            case -90:
                return 0.012654 * 100 / device.getTotalBatteryCapacityInJoules();
            default:
                return 100; //if signal is not in the above contemplated values then return an energy consumption of 100%
        }
    }

}
