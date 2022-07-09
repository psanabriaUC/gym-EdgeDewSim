package cl.puc.ing.edgedewsim.seas.node;

import cl.puc.ing.edgedewsim.mobilegrid.node.BatteryManager;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;

public interface DefaultBatteryManager extends BatteryManager {
    void addProfileData(int prof, ProfileData dat);

    DefaultExecutionManager getSEASExecutionManager();

    void setSEASExecutionManager(DefaultExecutionManager seasEM);

    Device getDevice();

    void setDevice(Device device);
}
