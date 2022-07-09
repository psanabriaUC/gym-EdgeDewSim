package cl.puc.ing.edgedewsim.mobilegrid.network;

public class UpdateMessage {

    public static int STATUS_MSG_SIZE_IN_BYTES = 2346; //http://stackoverflow.com/questions/5543326/what-is-the-total-length-of-pure-tcp-ack-sent-over-ethernet
    //16 bytes composed by: nodeId=4 bytes, %ofRemainingBattery=4 bytes, timeStamp=8bytes

    private String nodeId;
    private int percentageOfRemainingBattery;
    private long timeStamp;


    public UpdateMessage(String nodeId, int perOfRemainingBat, long timeStamp) {
        this.nodeId = nodeId;
        this.percentageOfRemainingBattery = perOfRemainingBat;
        this.timeStamp = timeStamp;
    }

    /**
     * Set the status message size in bytes
     */
    public static void setStatusMessageSize(int size) {
        STATUS_MSG_SIZE_IN_BYTES = size;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public int getPercentageOfRemainingBattery() {
        return percentageOfRemainingBattery;
    }

    public void setPercentageOfRemainingBattery(int percentageOfRemainingBattery) {
        this.percentageOfRemainingBattery = percentageOfRemainingBattery;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    /**
     * returns the data size (in bytes) of the status message
     */
    public int statusMessageSize() {
        return STATUS_MSG_SIZE_IN_BYTES;
    }
}
