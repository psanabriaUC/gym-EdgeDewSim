package cl.puc.ing.edgedewsim.mobilegrid.jobs;

/**
 * This class allow to store the energy, measured in Joules, of all transfers a node does during its lifetime.
 * Values are summarized in two variables: Joules wasted in sending data and Joules wasted in receiving data through a
 * network
 */
public class NetStats {
    private double accJoulesInReceivingData;
    private double accJoulesInSendingData;
    private double accMegabytesSent;
    private double accMegabytesReceived;
    private double maxAvailableJoules;
    private int updateMsgCount;

    public NetStats() {
        accJoulesInReceivingData = 0.0;
        accJoulesInSendingData = 0.0;
        setUpdateMsgCount(0);
        this.accMegabytesSent = 0.0;
        this.accMegabytesReceived = 0.0;
    }

    public NetStats(double maxJoules, double accJoulesInSendingData, double accJoulesInReceivingData, double accMegabytesSent, double accMegabytesReceived) {
        this.maxAvailableJoules = maxJoules;
        this.accJoulesInReceivingData = accJoulesInReceivingData;
        this.accJoulesInSendingData = accJoulesInSendingData;
        setUpdateMsgCount(0);
        this.accMegabytesSent = accMegabytesSent;
        this.accMegabytesReceived = accMegabytesReceived;
    }

    public double getAccJoulesInReceivingData() {
        return accJoulesInReceivingData;
    }

    public void addJoulesInReceivingData(double joulesInReceivingData) {
        this.accJoulesInReceivingData += joulesInReceivingData;
    }

    public double getAccJoulesInSendingData() {
        return accJoulesInSendingData;
    }

    public void addJoulesInSendingData(double joulesInSendingData) {
        this.accJoulesInSendingData += joulesInSendingData;
    }

    public int getUpdateMsgCount() {
        return updateMsgCount;
    }

    public void setUpdateMsgCount(int updateMsgCount) {
        this.updateMsgCount = updateMsgCount;
    }

    public double getAccMegabytesSent() {
        return accMegabytesSent;
    }

    public double getAccMegabytesReceived() {
        return accMegabytesReceived;
    }

    public void addMegabytesSent(double dataInMegabytes) {
        this.accMegabytesSent += dataInMegabytes;
    }

    public void addMegabytesReceived(double dataInMegabytes) {
        this.accMegabytesReceived += dataInMegabytes;
    }

    public boolean isMaximumAvailableJoulesExceeded() {
        return accJoulesInReceivingData + accJoulesInSendingData > maxAvailableJoules;

    }
}
