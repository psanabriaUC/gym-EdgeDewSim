package cl.puc.ing.edgedewsim.mobilegrid.node;

public interface DeviceListener {

    /**
     * Listen for device disconnection
     *
     * @param e Disconnected node
     */
    void onDeviceFail(Node e);
}
