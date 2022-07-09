package cl.puc.ing.edgedewsim.seas.proxy.jobstealing.condition;

import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.mobilegrid.node.SchedulerProxy;
import cl.puc.ing.edgedewsim.seas.proxy.jobstealing.StealerProxy;

public class BatteryDifferenceCondition implements StealingCondition {

    private double difference = 2;

    @Override
    public boolean canSteal(Device stealer, Device victim, StealerProxy proxy) {
        SchedulerProxy schedulerProxy = SchedulerProxy.getProxyInstance(proxy.getSimulation());
        return schedulerProxy.getLastReportedSOC(stealer) > difference * schedulerProxy.getLastReportedSOC(victim);
    }

    public void setDifference(String s) {
        this.difference = Double.parseDouble(s);
    }

}
