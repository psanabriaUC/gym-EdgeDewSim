package cl.puc.ing.edgedewsim.seas.reader;

import cl.puc.ing.edgedewsim.edge.node.DefaultInfiniteBatteryManager;
import cl.puc.ing.edgedewsim.mobilegrid.node.BatteryManager;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.mobilegrid.node.ExecutionManager;
import cl.puc.ing.edgedewsim.mobilegrid.node.NetworkEnergyManager;
import cl.puc.ing.edgedewsim.seas.node.DefaultBatteryManager;
import cl.puc.ing.edgedewsim.seas.node.DefaultExecutionManager;
import cl.puc.ing.edgedewsim.seas.node.DefaultFiniteBatteryManager;
import cl.puc.ing.edgedewsim.seas.node.DefaultNetworkEnergyManager;
import cl.puc.ing.edgedewsim.simulator.Simulation;

public class DefaultManagerFactory implements ManagerFactory {

    @Override
    public DefaultBatteryManager createBatteryManager(int prof, int charge, long estUptime, long batteryCapacityInJoules, boolean isInfinite) {
        if (isInfinite)
            return new DefaultInfiniteBatteryManager();
        else
            return new DefaultFiniteBatteryManager(prof, charge, estUptime, batteryCapacityInJoules);
    }

    @Override
    public DefaultExecutionManager createExecutionManager() {
        return new DefaultExecutionManager();
    }

    @Override
    public DefaultNetworkEnergyManager createNetworkEnergyManager(boolean enableNetworkExecutionManager, short wifiSignalString) {
        return new DefaultNetworkEnergyManager(enableNetworkExecutionManager, wifiSignalString);
    }

    @Override
    public Device createDevice(String name, BatteryManager bt, ExecutionManager em, NetworkEnergyManager nem, Simulation simulation, boolean isInfinite) {
        return new Device(name, bt, em, nem, simulation, !isInfinite);
    }


}
