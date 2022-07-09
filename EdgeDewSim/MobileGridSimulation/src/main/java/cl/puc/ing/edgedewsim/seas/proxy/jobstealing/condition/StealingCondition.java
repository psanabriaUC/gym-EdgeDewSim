package cl.puc.ing.edgedewsim.seas.proxy.jobstealing.condition;

import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.seas.proxy.jobstealing.StealerProxy;

public interface StealingCondition {

    boolean canSteal(Device stealer, Device victim, StealerProxy proxy);
}
