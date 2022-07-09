package cl.puc.ing.edgedewsim.mobilegrid.network;

import cl.puc.ing.edgedewsim.mobilegrid.node.Node;
import cl.puc.ing.edgedewsim.simulator.Simulation;

import java.util.Set;

public class BroadCastLink extends Link {

    public BroadCastLink(int delay, int bandwidth, Set<Node> source,
                         Set<Node> destinations, Simulation simulation) {
        super(delay, bandwidth, source, destinations, simulation);
    }

    @Override
    public Set<Node> getSources() {
        return NetworkModel.getModel(simulation).getNodes();
    }

    @Override
    public Set<Node> getDestinations() {
        return NetworkModel.getModel(simulation).getNodes();
    }

    @Override
    public boolean canSend(Node scr, Node dst) {
        return (scr.isOnline()) && (dst.isOnline());
    }
}
