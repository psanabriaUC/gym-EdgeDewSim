package cl.puc.ing.edgedewsim.seas.proxy;

public class RSSIDataEvaluator extends RSSIEvaluator {

    @Override
    public double eval(DataAssignment da) {
        return (da.getMbToBeReceived() + da.getMbToBeSend()) * 1024 * getRSSIEnergyPercentageConsumption(da.getDevice());
    }
}
