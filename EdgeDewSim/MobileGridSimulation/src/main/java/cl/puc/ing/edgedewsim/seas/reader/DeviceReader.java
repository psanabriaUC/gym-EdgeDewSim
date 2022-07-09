package cl.puc.ing.edgedewsim.seas.reader;

import cl.puc.ing.edgedewsim.mobilegrid.persistence.dbentity.DeviceTuple;
import cl.puc.ing.edgedewsim.mobilegrid.persistence.IDevicePersister;
import cl.puc.ing.edgedewsim.mobilegrid.persistence.IPersisterFactory;
import cl.puc.ing.edgedewsim.simulator.Simulation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Helper class for reading and parsing a node list configuration file. Examples input files can be found in
 * sim_input/nodes/*
 */
public class DeviceReader {

    public static IPersisterFactory persisterFactory = null;

    /**
     * Reader for reading text files.
     */
    private final BufferedReader reader;

    /**
     * Hash map of devices loaded from the configuration file. Each line in said file represents one device.
     * A device's name is used as a search key to enable O(1) access times.
     */
    private final Map<String, DeviceLoader> devices;

    /**
     * Current line being read by the reader.
     */
    private String line;

    /**
     * Builds a node list configuration file parser.
     *
     * @param file                          The node list configuration file.
     * @param networkEnergyManagementEnable Flag to specify whether or not to simulate energy consumption during network transfers.
     * @throws IOException if there is a problem reading the file.
     */
    public DeviceReader(String file, boolean networkEnergyManagementEnable, Simulation simulation) throws IOException {
        this.reader = new BufferedReader(new FileReader(file));
        this.devices = new HashMap<>();
        this.nextLine();
        boolean batteryCapacityUndefined = false;
        IDevicePersister devicePersister = persisterFactory.getDevicePersister();
        int sim_id = SimReader.getSim_id();
        // Expected format for each line in the configuration file:
        // [user_provided_id];[device_flops];[max_concurrent_active_jobs_supported];[initial_batt_level|Inf](;[battery_capacity_in_joules])
        while (this.line != null) {
            StringTokenizer st = new StringTokenizer(line, ";");
            String nodeId = st.nextToken();
            long flops = Long.parseLong(st.nextToken());
            int maxActiveJobs = Integer.parseInt(st.nextToken());
            String token = st.nextToken();
            int initialBatteryLevel = 100;
            boolean isInfinite = false;
            long batteryCapacityInJoules = Long.MAX_VALUE;

            if (token.toLowerCase().equals("inf")) {
                isInfinite = true;
            } else {
                initialBatteryLevel = Integer.parseInt(token);
                if (st.hasMoreTokens()) {
                    batteryCapacityInJoules = Long.parseLong(st.nextToken());
                } else {
                    batteryCapacityUndefined = true;
                }
            }

            DeviceLoader loader = new DeviceLoader(nodeId, flops, maxActiveJobs, networkEnergyManagementEnable,
                    initialBatteryLevel, batteryCapacityInJoules, simulation, isInfinite);
            DeviceTuple tuple = new DeviceTuple(nodeId, flops, batteryCapacityInJoules, sim_id);
            devicePersister.saveDeviceIntoMemory(nodeId, tuple);

            this.devices.put(nodeId, loader);
            this.nextLine();
        }

        if (batteryCapacityUndefined) {
            System.out.println("[WARN] At least one node has no battery capacity defined and the value of Long.MAX_VALUE is being used instead");
        }
        this.reader.close();
    }

    public static IPersisterFactory getPersisterFactory() {
        return persisterFactory;
    }

    public static void setPersisterFactory(IPersisterFactory persisterFactory) {
        DeviceReader.persisterFactory = persisterFactory;
    }

    /**
     * Gets the map of nodes read.
     *
     * @return The map of nodes.
     */
    public Map<String, DeviceLoader> getDevices() {
        return this.devices;
    }

    /**
     * Reads the next line in the file. Empty lines, or lines starting with # (for denoting comments) will be skipped.
     * Lines will be automatically trimmed of leading and trailing whitespaces.
     *
     * @throws IOException if there is a problem reading the file.
     */
    private void nextLine() throws IOException {
        this.line = this.reader.readLine();
        if (line == null) return;
        this.line = this.line.trim();
        while (line.startsWith("#") || line.equals("")) {
            this.line = this.reader.readLine().trim();
        }
    }

}
