package cl.puc.ing.edgedewsim.mobilegrid.network;

import cl.puc.ing.edgedewsim.mobilegrid.node.Node;
import cl.puc.ing.edgedewsim.simulator.Simulation;

public class NullLink extends Link {

    public NullLink(Simulation simulation) {
        super(0, 0, null, null, simulation);
    }

    @Override
    public boolean canSend(Node scr, Node dst) {
        return false;
    }


}
