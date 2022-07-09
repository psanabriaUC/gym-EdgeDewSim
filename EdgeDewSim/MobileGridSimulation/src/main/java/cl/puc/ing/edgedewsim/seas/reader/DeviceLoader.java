package cl.puc.ing.edgedewsim.seas.reader;

import cl.puc.ing.edgedewsim.mobilegrid.network.NetworkModel;
import cl.puc.ing.edgedewsim.mobilegrid.node.BatteryManager;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.seas.node.DefaultBatteryManager;
import cl.puc.ing.edgedewsim.seas.node.DefaultExecutionManager;
import cl.puc.ing.edgedewsim.seas.node.DefaultNetworkEnergyManager;
import cl.puc.ing.edgedewsim.seas.node.ProfileData;
import cl.puc.ing.edgedewsim.simulator.Event;
import cl.puc.ing.edgedewsim.simulator.Simulation;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Helper for reading and parsing all associated trace files for a given device.
 */
public class DeviceLoader extends Thread {
    private static final long ZERO_TIME = 0;

    public static ManagerFactory MANAGER_FACTORY = new DefaultManagerFactory();

    /**
     * The name of this device.
     */
    private final String nodeName;
    /**
     * The flops of the device's CPU.
     */
    private final long flops;

    @SuppressWarnings("unused")
    private final int maxActiveJobs;
    /**
     * Flag to enable or disable energy consumption simulation due to network related tasks (e.g. data transfers).
     */
    private final boolean networkEnergyManagerEnable;
    private final Simulation simulation;
    /**
     * The name of the file containing this device's base battery profile for energy consumption estimations.
     */
    private String batteryBaseFile = "";
    /**
     * The name of the file containing this device's battery profile for 100% CPU utilization and the screen turned off.
     * Used for energy consumption estimations.
     */
    private String batteryFullScreenOffFile = "";
    /**
     * The name of the file containing this device's battery profile for 100% CPU utilization and the screen turned on.
     * Used for energy consumption estimations.
     */
    private String batteryFullScreenOnFile = "";
    /**
     * The name of the file containing the device's cpu trace. It defines the <b>real</b> cpu percentage use when in
     * the relevant states for this simulation (i.e. when idle, cpu usage is actually around ~10%).
     */
    private String cpuFile;
    /**
     * The name of the file containing the device's user activity (when the screen is turned ON and OFF).
     */
    private String screenActivityFilePath;
    /**
     * The name of the file containing the device's network activity stemming from user activity and the app environment.
     */
    private String networkActivityFilePath;
    private ReentrantLock simLock;
    /**
     * Current battery state of charge as a value between 0 and 10.000.000, where 10.000.000 corresponds to 100%.
     */
    private int startCharge;
    /**
     * Estimated time the battery will last on a device, in milliseconds. Used by the original SEAS implementation.
     */
    private int startUptime;
    /**
     * Numerical identifier of the device.
     */
    private int deviceId;
    /**
     * Time of the simulation, in milliseconds, at which this device was added to the network.
     */
    private int startTime;
    /**
     * The time, in milliseconds, within the trace when the initial battery level happens.
     */
    private double firstSampleTime;
    /**
     * The battery capacity of the device is the value extract from the manufacturer information. It is calculated W x3600 sec
     */
    private long batteryCapacityInJoules;
    /**
     * Represent the signal strength value of a node (in dBm) with respect to the Access Point when its networking hardware is set in infrastructure mode
     */
    private short wifiSignalStrength;
    /**
     * Represents if the node is using a perpetual energy source
     */
    private boolean isInfinite;

    public DeviceLoader(String nodeName, long flops, int maxActiveJobs, boolean networkEnergyManagerEnable, short wifiSignalStrength, Simulation simulation) {
        this.nodeName = nodeName;
        this.flops = flops;
        this.maxActiveJobs = maxActiveJobs;
        this.networkEnergyManagerEnable = networkEnergyManagerEnable;
        this.setBatteryCapacityInJoules(Long.MAX_VALUE);
        this.setWifiSignalStrength(wifiSignalStrength);
        this.simulation = simulation;
    }

    public DeviceLoader(String nodeName, long flops, int maxActiveJobs, boolean networkEnergyManagerEnable, Simulation simulation) {
        this.nodeName = nodeName;
        this.flops = flops;
        this.maxActiveJobs = maxActiveJobs;
        this.networkEnergyManagerEnable = networkEnergyManagerEnable;
        this.setBatteryCapacityInJoules(Long.MAX_VALUE);
        this.simulation = simulation;
    }

    /**
     * Builds a parser to load the information relevant to a particular node (device) in the network.
     * @param nodeName                      The name of the node.
     * @param flops                         The flops of the device's CPU.
     * @param maxActiveJobs                 The maximum amount of jobs the device can concurrently handle.
     * @param networkEnergyManagementEnable Flag to specify whether energy spent during network communication should be simulated.
     * @param startBatteryLevel             Initial battery level
     * @param batteryCapacityInJoules       Battery capacity in joules.
     * @param isInfinite                    Is Battery infinite? (Edge server)
     */
    public DeviceLoader(String nodeName, long flops, int maxActiveJobs,
                        boolean networkEnergyManagementEnable, int startBatteryLevel, long batteryCapacityInJoules,
                        Simulation simulation,
                        boolean isInfinite) {
        this.nodeName = nodeName;
        this.flops = flops;
        this.maxActiveJobs = maxActiveJobs;
        this.networkEnergyManagerEnable = networkEnergyManagementEnable;
        this.startCharge = startBatteryLevel * BatteryManager.PROFILE_ONE_PERCENT_REPRESENTATION;
        this.setBatteryCapacityInJoules(batteryCapacityInJoules);
        this.simulation = simulation;
        this.isInfinite = isInfinite;
    }

    /**
     * File format battery
     * time;charge*
     * File format cpu
     * time;cpu*
     * no blank lines
     */
    @Override
    public void run() {
        DefaultNetworkEnergyManager networkEnergyManager = MANAGER_FACTORY.createNetworkEnergyManager(networkEnergyManagerEnable, wifiSignalStrength);
        DefaultBatteryManager batteryManager;

        if (!isInfinite) {
            List<ProfileData> batteryBaseProfileData = !this.batteryBaseFile.isEmpty() && new File(this.batteryBaseFile).exists() ? this.readBattery(this.batteryBaseFile, false) : new ArrayList<>();
            List<ProfileData> batteryFullScreenOffProfileData = !this.batteryFullScreenOffFile.isEmpty() && new File(this.batteryFullScreenOffFile).exists() ? this.readBattery(this.batteryFullScreenOffFile, false) : new ArrayList<>();
            List<ProfileData> batteryFullScreenOnProfileData = !this.batteryFullScreenOnFile.isEmpty() && new File(this.batteryFullScreenOnFile).exists() ? this.readBattery(this.batteryFullScreenOnFile, false) : new ArrayList<>();

            batteryManager = MANAGER_FACTORY.createBatteryManager(3, startCharge, startUptime, batteryCapacityInJoules, false);

            for (ProfileData data : batteryBaseProfileData)
                batteryManager.addProfileData(0, data);

            for (ProfileData data : batteryFullScreenOffProfileData)
                batteryManager.addProfileData(1, data);

            // TODO: placeholder, remove once we have profiles for CPU 100% and screen on.
            for (ProfileData data : batteryFullScreenOnProfileData)
                batteryManager.addProfileData(2, data);
        } else {
            batteryManager = MANAGER_FACTORY.createBatteryManager(0, startCharge, startUptime, batteryCapacityInJoules, true);
        }

        DefaultExecutionManager executionManager = MANAGER_FACTORY.createExecutionManager();
        executionManager.setMips(this.flops);

        Device device = MANAGER_FACTORY.createDevice(this.nodeName, batteryManager, executionManager, networkEnergyManager, simulation, isInfinite);

        simLock.lock();
        NetworkModel.getModel(simulation).addNewNode(device);
        simulation.addEntity(device);
        this.deviceId = simulation.getEntity(this.nodeName).getId();
        simLock.unlock();

        // Configure dependencies between the device and its managers.
        batteryManager.setDevice(device);
        executionManager.setDevice(device);
        networkEnergyManager.setDevice(device);
        executionManager.setBatteryManager(batteryManager);
        networkEnergyManager.setBatteryManager(batteryManager);
        batteryManager.setSEASExecutionManager(executionManager);

        //TODO: Profile CPU Events for IoT devices
        if (!isInfinite)
            readCPUEvents(simulation);

        readScreenActivityEvents(simulation);
        readNetworkActivityEvents(simulation);

        Event event = Event.createEvent(Event.NO_SOURCE, this.startTime, this.deviceId, Device.EVENT_TYPE_DEVICE_START, null);

        this.simLock.lock();
        simulation.addEvent(event);
        this.simLock.unlock();
    }

    /**
     * Parses the CPU trace file specified by {@link DeviceLoader#cpuFile}. This parameter must not be null.
     *
     * @param simulation The simulation to read
     */
    private void readCPUEvents(Simulation simulation) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(new File(this.cpuFile)));
            String line = reader.readLine();
            boolean samplesInclusionStarted = false;
            while (line != null) {
                if (line.trim().equals("")) {
                    line = reader.readLine();
                    break;
                }
                StringTokenizer st = new StringTokenizer(line, ";");
                st.nextToken();
                long time = Long.parseLong(st.nextToken());
                st.nextToken();
                double cpu = Double.parseDouble(st.nextToken());
                Event event = null;
                if(!samplesInclusionStarted) {
                    if (time >= firstSampleTime) {
                        //a fictitious event is created only if there is none cpu usage event associated for the firstSampleTime. It is
                        //to avoid inconsistencies in mobile device behavior. More specifically, to avoid starting a device with undefined
                        //cpu usage value.
                        if (time > firstSampleTime)
                            event = Event.createEvent(Event.NO_SOURCE, ZERO_TIME + this.startTime, this.deviceId, Device.EVENT_TYPE_CPU_UPDATE, cpu);
                        samplesInclusionStarted = true;
                    }
                }
                if (samplesInclusionStarted) {
                    event = Event.createEvent(Event.NO_SOURCE, getSynchronizedEventTime(time), this.deviceId, Device.EVENT_TYPE_CPU_UPDATE, cpu);
                }
                if (event != null) {
                    this.simLock.lock();
                    simulation.addEvent(event);
                    this.simLock.unlock();
                }

                line = reader.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Parses the battery trace file specified by <b>batteryTraceFile</b>. Must not be null.
     *
     * @param batteryTraceFile The name of the file containing the battery trace.
     * @param baseProfile If have a baseProfile
     * @return The list of battery profiling samples contained in the file.
     */
    private List<ProfileData> readBattery(String batteryTraceFile, boolean baseProfile) {
        List<ProfileData> profileData = new ArrayList<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(new File(batteryTraceFile)));
            String line = reader.readLine();
            int battery = 0;
            double time = 0;
            boolean samplesInclusionStarted = false; //this boolean was added to allow device join with a different battery level than the first appearing in trace
            while (line != null) {
                if (line.trim().equals("")) {
                    line = reader.readLine();
                } else {
                    String[] data = line.split(";");
                    if (line.startsWith("ADD_NODE;")) {
                        if (baseProfile)//allow only the baseline profile to set startTime
                            this.startTime = Integer.parseInt(data[1]);
                        line = reader.readLine();
                    } else {
                        int nbattery = 0;
                        double ntime = 0;
                        if (line.startsWith("LEFT_NODE;")) {
                            line = reader.readLine();
                            nbattery = 0;
                            ntime = Double.parseDouble(data[1]);
                        } else if (line.startsWith("NEW_BATTERY_STATE_NODE;")) {
                            nbattery = Integer.parseInt(data[3]);
                            ntime = Double.parseDouble(data[1]);
                        }

                        double slope = nbattery - battery;
                        slope = slope / (ntime - time);

                        /*
                         * Include the current sample of the trace providing that one of the two conditions are met:
                         *  1) the device start battery level is not specified at the constructor so the current sample is the start battery level
                         *  2) the current sample is equal or greater than the specified start battery level (valid only for discharging traces)
                         */
                        if (startCharge == 0 || startCharge == nbattery) {
                            samplesInclusionStarted = true;
                            if (baseProfile) {
                                startCharge = nbattery;
                                firstSampleTime = ntime;
                                startUptime = (int) ((ntime - time) * nbattery); //this line is only used and valid with original SEAS scheduler
                            }
                        }
                        // Ignores similar events
                        if (battery != nbattery) {
                            battery = nbattery;
                            time = ntime;
                            if (samplesInclusionStarted)
                                profileData.add(new ProfileData(nbattery, slope));
                        }
                        line = reader.readLine();
                    }
                }
            }
            if (!samplesInclusionStarted) {
                System.out.println("Initial battery level configured for device " + this.nodeName + " is not contained within the battery trace " + batteryTraceFile);
                throw new Exception("Initial battery level configured for device " + this.nodeName + " is not contained within the battery trace " + batteryTraceFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (profileData.get(profileData.size() - 1).getToCharge() != 0) {
            profileData.add(new ProfileData(0, profileData.get(profileData.size() - 1).getSlope()));
        }

        return profileData;
    }

    private void readScreenActivityEvents(Simulation simulation) {
        if (screenActivityFilePath != null) {
            File file = new File(screenActivityFilePath);
            if (file.exists()) {
                Scanner scanner = null;
                try {
                    scanner = new Scanner(file);
                    boolean samplesInclusionStarted = false;
                    while (scanner.hasNext()) {
                        String line = scanner.nextLine().trim();
                        if (line.indexOf("#") == 0 || line.length() == 0) continue;

                        Event event = null;
                        String[] values = line.split(";");
                        long time = Long.parseLong(values[0]);
                        String flag = values[1];
                        Boolean activity = flag.equals("ON");

                        if (!samplesInclusionStarted) {
                            if (time >= firstSampleTime) {

                                //a fictitious event is created only if there is no screen event associated for the firstSamplingTime. It is
                                //to avoid inconsistencies in mobile device behavior. More specifically, to avoid starting a device with no
                                //screen activity defined.
                                if (time > firstSampleTime)
                                    event = Event.createEvent(Event.NO_SOURCE, ZERO_TIME + this.startTime, this.deviceId, Device.EVENT_TYPE_SCREEN_ACTIVITY, activity);
                                samplesInclusionStarted = true;
                            }
                        }

                        if (samplesInclusionStarted)
                            event = Event.createEvent(Event.NO_SOURCE, getSynchronizedEventTime(time), this.deviceId, Device.EVENT_TYPE_SCREEN_ACTIVITY, activity);

                        if (event != null) {
                            this.simLock.lock();
                            simulation.addEvent(event);
                            this.simLock.unlock();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                } finally {
                    if (scanner != null) {
                        scanner.close();
                    }
                }
            }
        }
    }

    private long getSynchronizedEventTime(long time) throws Exception {
        time -= this.firstSampleTime; //synchronize the event time relative to when initial battery level happens
        if (time < ZERO_TIME)
            throw new Exception("Time inconsistency when synchronizing traces of device " + this.nodeName);
        time += this.startTime; //synchronize the time relative to when the simulation starts
        if (time < ZERO_TIME)
            throw new Exception("Out of range event time representation while loading traces of device " + this.nodeName);
        return time;
    }

    private void readNetworkActivityEvents(Simulation simulation) {
        if (networkActivityFilePath != null) {
            File file = new File(networkActivityFilePath);

            Scanner scanner = null;
            try {
                scanner = new Scanner(file);
                while (scanner.hasNext()) {
                    String line = scanner.nextLine().trim();
                    if (line.indexOf("#") == 0 || line.length() == 0) continue;

                    String[] values = line.split(";");
                    long time = Long.parseLong(values[0]);
                    int messageSize = Integer.parseInt(values[1]);
                    String flag = values[2];

                    Event.NetworkActivityEventData eventData = new Event.NetworkActivityEventData(messageSize, flag.equals("IN"));

                    Event event = Event.createEvent(Event.NO_SOURCE, time, this.deviceId, Device.EVENT_NETWORK_ACTIVITY, eventData);

                    this.simLock.lock();
                    simulation.addEvent(event);
                    this.simLock.unlock();

                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                System.exit(1);
            } finally {
                if (scanner != null) {
                    scanner.close();
                }
            }
        }
    }

    // Getters and setters.

    public String getBatteryFile() {
        return batteryBaseFile;
    }

    public void setBatteryFile(String batteryFile) throws FileNotFoundException {
        this.batteryBaseFile = batteryFile;
        if (!new File(this.batteryBaseFile).exists()) {
            throw new FileNotFoundException();
        }
    }

    public String getCPUFile() {
        return cpuFile;
    }

    public void setCPUFile(String cpuFile) throws FileNotFoundException {
        this.cpuFile = cpuFile;
        if (!new File(this.cpuFile).exists()) {
            throw new FileNotFoundException();
        }
    }

    public String getFullBatteryFile() {
        return batteryFullScreenOffFile;
    }

    public void setBatteryCpuFullScreenOffFile(String fullBatteryFile) throws FileNotFoundException {
        this.batteryFullScreenOffFile = fullBatteryFile;
        if (!new File(this.batteryFullScreenOffFile).exists()) {
            throw new FileNotFoundException();
        }
    }

    public void setBatteryCpuFullScreenOnFile(String batteryFullScreenOnFile) {
        this.batteryFullScreenOnFile = batteryFullScreenOnFile;
    }

    public void setScreenActivityFilePath(String screenActivityFile) {
        this.screenActivityFilePath = screenActivityFile;
    }

    public void setNetworkActivityFilePath(String networkActivityFilePath) {
        this.networkActivityFilePath = networkActivityFilePath;
    }

    public void setSimLock(ReentrantLock simLock) {
        this.simLock = simLock;
    }

    public void setWifiSignalStrength(short wifiSignalStrength) {
        this.wifiSignalStrength = wifiSignalStrength;
    }

    public long getBatteryCapacityInJoules() {
        return batteryCapacityInJoules;
    }

    public void setBatteryCapacityInJoules(long batteryCapacityInJoules) {
        this.batteryCapacityInJoules = batteryCapacityInJoules;
    }
}
