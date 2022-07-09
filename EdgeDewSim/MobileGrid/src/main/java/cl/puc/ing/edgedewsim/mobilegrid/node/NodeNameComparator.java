package cl.puc.ing.edgedewsim.mobilegrid.node;

import cl.puc.ing.edgedewsim.simulator.Entity;

import java.util.Comparator;

public class NodeNameComparator implements Comparator<Node> {
    @Override
    public int compare(Node arg0, Node arg1) {
        return ((Entity) arg0).getName().compareTo(((Entity) arg1).getName());
    }
}
