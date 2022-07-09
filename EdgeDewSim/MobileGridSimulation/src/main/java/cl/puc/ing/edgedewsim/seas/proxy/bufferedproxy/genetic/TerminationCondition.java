package cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy.genetic;

import java.util.ArrayList;

public interface TerminationCondition {

    boolean satisfiedCondition(ArrayList<Short[]> population);

    String getName();

}
