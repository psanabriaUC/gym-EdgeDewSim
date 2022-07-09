package cl.puc.ing.edgedewsim.edge.node;

import cl.puc.ing.edgedewsim.mobilegrid.node.BatteryManager;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.seas.node.DefaultBatteryManager;
import cl.puc.ing.edgedewsim.seas.node.DefaultExecutionManager;
import cl.puc.ing.edgedewsim.seas.node.ProfileData;
import cl.puc.ing.edgedewsim.simulator.Event;
import cl.puc.ing.edgedewsim.simulator.Logger;

public class DefaultInfiniteBatteryManager implements DefaultBatteryManager {
    private static final int MAX_CHARGE = BatteryManager.PROFILE_ONE_PERCENT_REPRESENTATION * 100;
    private Device device;
    private long startTime;
    private Event lastAddedEvent;
    private DefaultExecutionManager executionManager;
    private long lastMeasurement;
    private boolean runningJobs;

    public DefaultInfiniteBatteryManager() {
    }

    @Override
    public void onBeginExecutingJobs() {
    }

    @Override
    public void onStopExecutingJobs() {
    }

    @Override
    public void onNetworkEnergyConsumption(double decreasedBatteryPercentage) {

    }

    @Override
    public void onBatteryEvent(int level) {
        if (level <= 0) {
            this.lastMeasurement = device.getSimulation().getTime();
            this.device.onBatteryDepletion();
        }
    }

    @Override
    public void onUserActivityEvent(boolean flag) {

    }

    @Override
    public int getCurrentBattery() {
        return MAX_CHARGE;
    }

    @Override
    public long getBatteryCapacityInJoules() {
        return Integer.MAX_VALUE;
    }

    @Override
    public long getEstimatedUptime() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void startWorking() {
        this.lastMeasurement = device.getSimulation().getTime();
        this.startTime = device.getSimulation().getTime();
        this.lastAddedEvent = Event.createEvent(Event.NO_SOURCE, this.lastMeasurement, this.device.getId(),
                Device.EVENT_TYPE_BATTERY_UPDATE, MAX_CHARGE);
        device.getSimulation().addEvent(this.lastAddedEvent);
        Logger.getInstance(device.getSimulation()).logEntity(device, "Device started");
    }

    @Override
    public void shutdown() {
        device.getSimulation().removeEvent(this.lastAddedEvent);
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public int getInitialSOC() {
        return MAX_CHARGE;
    }

    @Override
    public double getCurrentSOC() {
        return MAX_CHARGE;
    }

    @Override
    public double getJoulesBasedOnLastReceivedSOC(int newBatteryLevel) {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public void addProfileData(int prof, ProfileData dat) {}

    @Override
    public DefaultExecutionManager getSEASExecutionManager() {
        return executionManager;
    }

    @Override
    public void setSEASExecutionManager(DefaultExecutionManager seasEM) {
        this.executionManager = seasEM;
    }

    @Override
    public Device getDevice() {
        return this.device;
    }

    @Override
    public void setDevice(Device device) {
        this.device = device;
    }
}
