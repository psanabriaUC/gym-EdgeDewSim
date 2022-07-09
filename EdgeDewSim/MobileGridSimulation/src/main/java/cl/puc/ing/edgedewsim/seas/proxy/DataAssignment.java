package cl.puc.ing.edgedewsim.seas.proxy;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;

import java.util.ArrayList;

public class DataAssignment {

    public static DataAssignmentEvaluatorIF evaluator = new RemainingDataTransferringEvaluator();

    private Device device;
    /**
     * Total Megabytes representing the data the device should send (sum of job outputs)
     */
    private double mbToBeSend;
    /**
     * Total Megabytes representing the data the device should onMessageReceived (sum of job inputs)
     */
    private double mbToBeReceived;
    /**
     * Amount of assigned jobs that the device is able to transfer completely, i.e., input + output, with its available energy
     */
    private int affordableJobCompletelyTransfered = 0;
    /**
     * Amount of data transfered (expressed in kilobytes) that the device is able to transfer completely, i.e., input + output, with its available energy
     */
    private double affordableDataTranfered = 0.0d;
    /**
     * this is the energy (in joules) the device wastes in sending and receiving the affordableDataTranfered
     */
    private double deviceEnergyWasted;

    /**
     * jobs that were scheduled to the device
     */
    private ArrayList<Job> assignedJobs;

    public DataAssignment(Device dev) {
        this.device = dev;
        assignedJobs = new ArrayList<Job>();
        mbToBeReceived = 0.0;
        mbToBeSend = 0.0;
    }


    public void scheduleJob(Job j) {
        assignedJobs.add(j);
        //the sent and received data is accounted from the device stand point not from the proxy stand point
        mbToBeReceived += ((double) j.getInputSize() / 1024d) / 1024d;
        mbToBeSend += ((double) j.getOutputSize() / 1024d) / 1024d;
    }

    public double getMbToBeSend() {
        return mbToBeSend;
    }

    public void setMbToBeSend(double mbToBeSend) {
        this.mbToBeSend = mbToBeSend;
    }

    public double getMbToBeReceived() {
        return mbToBeReceived;
    }

    public void setMbToBeReceived(double mbToBeReceived) {
        this.mbToBeReceived = mbToBeReceived;
    }


    public Device getDevice() {
        return device;
    }


    public void setDevice(Device device) {
        this.device = device;
    }

    public ArrayList<Job> getAssignedJobs() {
        return assignedJobs;
    }

    @Override
    public DataAssignment clone() {
        DataAssignment dataAssignmentClone = new DataAssignment(this.device);
        dataAssignmentClone.assignedJobs = this.assignedJobs;
        dataAssignmentClone.mbToBeReceived = this.mbToBeReceived;
        dataAssignmentClone.mbToBeSend = this.mbToBeSend;
        return dataAssignmentClone;
    }

    public int getAffordableJobCompletelyTransfered() {
        return affordableJobCompletelyTransfered;
    }

    public void setAffordableJobCompletelyTransfered(
            int affordableJobCompletelyTransfered) {
        this.affordableJobCompletelyTransfered = affordableJobCompletelyTransfered;
    }

    public double getAffordableDataTranfered() {
        return affordableDataTranfered;
    }

    public void setAffordableDataTranfered(double affordableDataTranfered) {
        this.affordableDataTranfered = affordableDataTranfered;
    }

    public double getDeviceEnergyWasted() {
        return deviceEnergyWasted;
    }

    public void setDeviceEnergyWasted(double energyWasted) {
        this.deviceEnergyWasted = energyWasted;

    }

    public void scheduleJobs(ArrayList<Job> assignedJobs2) {
        for (Job job : assignedJobs2) {
            this.scheduleJob(job);
        }
    }


    public double eval() {
        return evaluator.eval(this);
    }

}
