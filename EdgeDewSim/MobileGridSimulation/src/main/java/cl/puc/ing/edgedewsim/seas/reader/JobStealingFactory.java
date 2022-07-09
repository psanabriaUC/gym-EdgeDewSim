package cl.puc.ing.edgedewsim.seas.reader;

import cl.puc.ing.edgedewsim.edge.node.DefaultInfiniteBatteryManager;
import cl.puc.ing.edgedewsim.mobilegrid.node.BatteryManager;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.mobilegrid.node.ExecutionManager;
import cl.puc.ing.edgedewsim.mobilegrid.node.NetworkEnergyManager;
import cl.puc.ing.edgedewsim.seas.node.DefaultBatteryManager;
import cl.puc.ing.edgedewsim.seas.node.DefaultExecutionManager;
import cl.puc.ing.edgedewsim.seas.node.DefaultNetworkEnergyManager;
import cl.puc.ing.edgedewsim.seas.node.jobstealing.JSDevice;
import cl.puc.ing.edgedewsim.seas.node.jobstealing.JSSEASBatteryManager;
import cl.puc.ing.edgedewsim.seas.node.jobstealing.JSSEASExecutionManager;
import cl.puc.ing.edgedewsim.simulator.Simulation;

public class JobStealingFactory implements ManagerFactory {

    @Override
    public DefaultBatteryManager createBatteryManager(int prof, int charge,
                                                      long estUptime, long batteryCapacityInJoules, boolean isInfinite) {
        if (isInfinite)
            return new DefaultInfiniteBatteryManager();
        else
            return new JSSEASBatteryManager(prof, charge, estUptime, batteryCapacityInJoules);
    }

    @Override
    public DefaultExecutionManager createExecutionManager() {
        return new JSSEASExecutionManager();
    }

    @Override
    public DefaultNetworkEnergyManager createNetworkEnergyManager(
            boolean enableNetworkExecutionManager, short wifiSignalStrength) {
        return new DefaultNetworkEnergyManager(enableNetworkExecutionManager, wifiSignalStrength);
    }

    @Override
    public Device createDevice(String name, BatteryManager bt, ExecutionManager em, NetworkEnergyManager nem, Simulation simulation, boolean isInfinite) {
        return new JSDevice(name, bt, em, nem, simulation, !isInfinite);
    }

}
