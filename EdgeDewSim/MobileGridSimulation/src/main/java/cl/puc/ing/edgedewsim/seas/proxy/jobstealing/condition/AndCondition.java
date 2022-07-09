package cl.puc.ing.edgedewsim.seas.proxy.jobstealing.condition;

import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.seas.proxy.jobstealing.StealerProxy;

public class AndCondition extends CombinedCondition {

    @Override
    public boolean canSteal(Device stealer, Device victim, StealerProxy proxy) {
        for (StealingCondition condition : this.conditions)
            if (!condition.canSteal(stealer, victim, proxy)) return false;
        return true;
    }

}
