package cl.puc.ing.edgedewsim.seas.proxy;

import cl.puc.ing.edgedewsim.seas.node.DefaultFiniteBatteryManager;

public class RSSIEnergyPercentageEvaluator extends RSSIEvaluator {

    @Override
    public double eval(DataAssignment dataAssignment) {
        return (double) dataAssignment.getDevice().getWifiRSSI() + (double) (dataAssignment.getDevice().getBatteryLevel() /
                DefaultFiniteBatteryManager.PROFILE_ONE_PERCENT_REPRESENTATION) -
                ((dataAssignment.getMbToBeReceived() + dataAssignment.getMbToBeSend()) * 1024 * getRSSIEnergyPercentageConsumption(dataAssignment.getDevice()));
    }
}
