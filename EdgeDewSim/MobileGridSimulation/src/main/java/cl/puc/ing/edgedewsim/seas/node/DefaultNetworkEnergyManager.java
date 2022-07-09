package cl.puc.ing.edgedewsim.seas.node;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.JobStatsUtils;
import cl.puc.ing.edgedewsim.mobilegrid.node.BatteryManager;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.mobilegrid.node.NetworkEnergyManager;
import cl.puc.ing.edgedewsim.mobilegrid.node.Node;
import cl.puc.ing.edgedewsim.simulator.Logger;

import java.util.HashMap;

/**
 * This class contains logic to reflect the energy consumption of a device caused by
 * its networking activity, i.e., caused by the sending and receiving of data through the network. The
 * energy consumption is taken as equal for sending and receiving operations but varies according to
 * device strength of signal. The strength of signal information was extracted from "Characterizing and
 * modeling the impact of wireless signal strength on smartphone battery drain"
 * All these energy consumptions are informed to the batteryManager that handles battery state updates.
 * Author: mhirsch
 */
public class DefaultNetworkEnergyManager implements NetworkEnergyManager {
    //Contains information about the cost (in Joules) of sending 1 kb through a wifi connection depending on the wifi signal
    //strength (RSSI) measured in dBm. These costs apply when the whole data to be send is 10 Kb or less.
    private static final HashMap<Short, Double> wifiRSSI_joulesPerKb_10kb = new HashMap<Short, Double>() {

        {
            put((short) -50, 0.00999d);
            put((short) -80, 0.010656d);
            put((short) -85, 0.01332d); //10 microAmph
            put((short) -90, 0.034632d); //(26 microAmp / 1000000) x 3600 sec x 3.7 volts = joules of sending 10KB
        }
    };
    //Contains information about the cost (in Joules) of receiving 1 kb through a wifi connection depending on the wifi signal
    //strength (RSSI) measured in dBm. These costs apply when the whole data to be send is 100 kb.
    private static final HashMap<Short, Double> wifiRSSI_joulesPerKb_100kb = new HashMap<Short, Double>() {

        {
            put((short) -50, 0.0018648d);
            put((short) -80, 0.0022644d);
            put((short) -85, 0.00333d);
            put((short) -90, 0.012654d);
        }
    };
    private final int TEN_KILOBYTES = 10;
    private final boolean TRANSFER_FROM_DEVICE = true;
    private final boolean TRANSFER_TO_DEVICE = false;
    private final short wifiRSSI;
    //private double joulesPerKbSent; //= 0.00558233189056585;this is the cost in joules of sending a kb through the network using TCP
    //private double batteryPercentageConsumedPerKbyte; // = 0.0000349244988148514; percentage of the battery capacity that represent each Kb sent through the network
    private boolean networkEnergyManagementEnable = true;
    //remove comment for testing purposes
    private double accGlobalJoules = 0;
    private Device device;
    private BatteryManager batteryManager;


    public DefaultNetworkEnergyManager(boolean enableNetworkEnergyManagement, short wifiSignalStrength) {
        this.networkEnergyManagementEnable = enableNetworkEnergyManagement;
        wifiRSSI = wifiSignalStrength;
    }

    public BatteryManager getBatteryManager() {
        return batteryManager;
    }

    public void setBatteryManager(BatteryManager batteryManager) {
        this.batteryManager = batteryManager;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    @Override
    public boolean onSendData(Node source, Node destination, long totalTransferSize, long bytesSent) {
        return registerEnergyWaste(source, destination, totalTransferSize, bytesSent, TRANSFER_FROM_DEVICE);
    }

    @Override
    public boolean onReceiveData(Node source, Node destination, long totalTransferSize, long bytesReceived) {
        return registerEnergyWaste(source, destination, totalTransferSize, bytesReceived, TRANSFER_TO_DEVICE);
    }

    /**
     * This method accounts the energy wasted of sending or receiving the message passed as arguments. If
     * transfer could be performed with the available energy then the invocation returns true, otherwise
     * returns false. If networkEnergyManagementEnable flag is false, then the invocation always returns true.
     */
    private boolean registerEnergyWaste(Node source, Node destination, double totalTransferSize, long bytesSent, boolean transferringType) {
        boolean completedTransfer = true;

        if (networkEnergyManagementEnable) {
            double dataSizeInKb = bytesSent / 1024.0;
            double joulesPerKbSent = (totalTransferSize / 1024.0d) <= TEN_KILOBYTES ? wifiRSSI_joulesPerKb_10kb.get(wifiRSSI) : wifiRSSI_joulesPerKb_100kb.get(wifiRSSI);
            double joulesNeedForTransferData = dataSizeInKb * joulesPerKbSent;
			
			/*if((device.isReceiving() && transferingType) || (device.isSending() && !transferingType))
				joulesNeedForTransferData =  joulesNeedForTransferData / 2;*/

            //infer the available joules of the node from its current battery percentage
            //  100% ------------------------totalJoules
            //  SOC%-------------------------availableJoules
            double availableJoules = ((batteryManager.getCurrentSOC() * (double) batteryManager.getBatteryCapacityInJoules()) /
                    ((double) 100 * (double) BatteryManager.PROFILE_ONE_PERCENT_REPRESENTATION));
            Logger.getInstance(device.getSimulation()).logEnergy(device, "registerEnergyWaste", "availableJoules=" + availableJoules, "joulesNeedForTransferData=" + joulesNeedForTransferData);

            // 1) calculate the percentage of battery represented by the cost of transfer the KBs of data through the network
            // totalJoules ------------------100%
            // joulesNeedForTransferData-----batteryPercentage%
            // NOTE: in the above calculus a transformation of the batteryPercentage% value to the representation used by the battery manager is included
            double batteryPercentageNeeded = ((joulesNeedForTransferData * (double) 100) / (double) batteryManager.getBatteryCapacityInJoules()) *
                    (double) BatteryManager.PROFILE_ONE_PERCENT_REPRESENTATION;

            if (Double.isInfinite(joulesNeedForTransferData) ||
                    Double.isNaN(joulesNeedForTransferData) ||
                    joulesNeedForTransferData > availableJoules) {

                joulesNeedForTransferData = availableJoules;
                batteryPercentageNeeded = batteryManager.getCurrentSOC();
                dataSizeInKb = availableJoules / joulesPerKbSent;
                completedTransfer = false;
            }

            batteryManager.onNetworkEnergyConsumption(batteryPercentageNeeded);
            if (transferringType == TRANSFER_FROM_DEVICE)
                JobStatsUtils.getInstance(device.getSimulation()).registerSendingDataEnergy(source, joulesNeedForTransferData, dataSizeInKb / (double) 1024);
            else
                JobStatsUtils.getInstance(device.getSimulation()).registerReceivingDataEnergy(destination, joulesNeedForTransferData, dataSizeInKb / (double) 1024);

            accGlobalJoules += joulesNeedForTransferData;
        }
        return completedTransfer;
    }

    @Override
    public boolean isNetworkEnergyManagementEnable() {
        return networkEnergyManagementEnable;
    }


    @Override
    public short getWifiRSSI() {
        return wifiRSSI;
    }

    @Override
    /**data is expressed in bytes*/
    public double getJoulesWastedWhenTransferData(double data) {
        double joulesPerKbSent = data <= 10240 ? wifiRSSI_joulesPerKb_10kb.get(wifiRSSI) : wifiRSSI_joulesPerKb_100kb.get(wifiRSSI);
        return (data / (double) 1024) * joulesPerKbSent;
    }

    //remove comment for testing purposes
    @Override
    public double getAccEnergyInTransferring() {
        return accGlobalJoules;
    }

}
