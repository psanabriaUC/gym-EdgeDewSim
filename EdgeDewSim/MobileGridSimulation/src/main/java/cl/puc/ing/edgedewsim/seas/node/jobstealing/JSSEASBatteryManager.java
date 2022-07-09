package cl.puc.ing.edgedewsim.seas.node.jobstealing;

import cl.puc.ing.edgedewsim.seas.node.DefaultFiniteBatteryManager;

public class JSSEASBatteryManager extends DefaultFiniteBatteryManager {

    public JSSEASBatteryManager(int prof, int charge, long estUptime, long batteryCapacityInJoules) {
        super(prof, charge, estUptime, batteryCapacityInJoules);
    }

    @Override
    public void startWorking() {
        super.startWorking();
        //((StealerProxy)SchedulerProxy.getProxyInstance(simulation)).steal(this.getDevice());
    }


}
