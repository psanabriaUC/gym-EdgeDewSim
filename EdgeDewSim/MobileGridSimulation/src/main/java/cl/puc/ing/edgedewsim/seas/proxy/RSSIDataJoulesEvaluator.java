package cl.puc.ing.edgedewsim.seas.proxy;

import cl.puc.ing.edgedewsim.mobilegrid.node.Device;

public class RSSIDataJoulesEvaluator extends RSSIEvaluator {

    @Override
    public double eval(DataAssignment dataAssignment) {
        return (dataAssignment.getMbToBeReceived() + dataAssignment.getMbToBeSend()) * 1024 *
                getRSSIEnergyPercentageConsumption(dataAssignment.getDevice());
    }

    public double getValue(Double mbToTransfer, Device device) {
        return mbToTransfer * ((double) 1024) * getRSSIEnergyPercentageConsumption(device);
    }

}
