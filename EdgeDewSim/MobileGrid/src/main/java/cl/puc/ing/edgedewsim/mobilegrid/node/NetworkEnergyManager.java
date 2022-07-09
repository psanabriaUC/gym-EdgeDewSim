package cl.puc.ing.edgedewsim.mobilegrid.node;

/**
 * This interface describes methods that cause a decrement in the battery charge of a device that are particularly related to send or onMessageReceived data through the network of the mobile grid
 * Author: mhirsch
 */
public interface NetworkEnergyManager {

    /**
     * Called when a device sends data through the network. The true value is returned if
     * the device could received the message passed as argument
     */
    boolean onSendData(Node source, Node destination, long totalTransferSize, long bytesSent);

    /**
     * Called when a device receives data through the network. The true value is returned if
     * the device could received the message passed as argument
     */
    boolean onReceiveData(Node source, Node destination, long totalTransferSize, long bytesReceived);

    /**
     * Returns the wifi Received Signal Strength of the device
     */
    short getWifiRSSI();

    /**
     * this method returns the energy (in Joules) that the device is supposed to waste when sending the
     * amount of data indicated as argument. Data is expressed in bytes. The value returned is
     * independent from the available energy of the device
     */
    double getJoulesWastedWhenTransferData(double data);

    /**
     * NOTE: remove comment for testing purposes
     * This method has testing purposes and returns the percentage of energy of the device that was
     * consumed in networking activity
     */
    double getAccEnergyInTransferring();


    /**
     * return true if the network energy management is enabled, false otherwise
     */
    boolean isNetworkEnergyManagementEnable();

}
