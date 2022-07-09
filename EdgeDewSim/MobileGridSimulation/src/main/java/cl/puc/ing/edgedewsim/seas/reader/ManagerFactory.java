package cl.puc.ing.edgedewsim.seas.reader;

import cl.puc.ing.edgedewsim.mobilegrid.node.BatteryManager;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.mobilegrid.node.ExecutionManager;
import cl.puc.ing.edgedewsim.mobilegrid.node.NetworkEnergyManager;
import cl.puc.ing.edgedewsim.seas.node.DefaultBatteryManager;
import cl.puc.ing.edgedewsim.seas.node.DefaultExecutionManager;
import cl.puc.ing.edgedewsim.seas.node.DefaultNetworkEnergyManager;
import cl.puc.ing.edgedewsim.simulator.Simulation;

public interface ManagerFactory {

    DefaultBatteryManager createBatteryManager(int prof, int charge, long estUptime, long batteryCapacityInJoules, boolean isInfinite);

    DefaultExecutionManager createExecutionManager();

    DefaultNetworkEnergyManager createNetworkEnergyManager(boolean enableNetworkExecutionManager, short wifiSignalString);

    Device createDevice(String name, BatteryManager bt, ExecutionManager em, NetworkEnergyManager nem, Simulation simulation, boolean isInfinite);

}
