package cl.puc.ing.edgedewsim.mobilegrid.network;

import cl.puc.ing.edgedewsim.simulator.Simulation;

public class IdealBroadCastLink extends BroadCastLink {

    public IdealBroadCastLink(Simulation simulation) {
        super(0, 0, null, null, simulation);
    }

    @Override
    public long getTransmissionTime(long totalTransferSize, int size) {
        return 0;
    }

}
