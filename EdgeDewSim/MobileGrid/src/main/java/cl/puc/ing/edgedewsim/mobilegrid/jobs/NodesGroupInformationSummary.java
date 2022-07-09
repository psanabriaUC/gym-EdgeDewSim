package cl.puc.ing.edgedewsim.mobilegrid.jobs;

import java.util.ArrayList;
import java.util.List;

public class NodesGroupInformationSummary {
    private String groupName = "";
    private int nodesQuantity = 0;
    private int sumOfJobsFinishedAndTransferred = 0;
    private int sumOfIncompleteJobs = 0;
    private int sumOfStolenJobs = 0;
    private final List<Integer> jobsFinishedAndTransferredList = new ArrayList<>();
    private final List<Integer> incompleteJobsList = new ArrayList<>();
    private final List<Integer> stolenJobsList = new ArrayList<>();

    public NodesGroupInformationSummary(String name) {
        groupName = name;
    }

    public NodesGroupInformationSummary() {
    }

    public int getNodesQuantity() {
        return nodesQuantity;
    }

    public float getAvgIncompleteJobs() {
        return (float) this.sumOfIncompleteJobs / (float) nodesQuantity;
    }

    public float getAvgJobsFinishedAndTransferred() {
        return (float) this.sumOfJobsFinishedAndTransferred / (float) nodesQuantity;
    }

    public float getAvgStolenJobs() {
        return (float) this.sumOfStolenJobs / (float) nodesQuantity;
    }

    public void addFinishedTransferredJobs(int finishedJobs) {
        jobsFinishedAndTransferredList.add(finishedJobs);
        this.sumOfJobsFinishedAndTransferred += finishedJobs;
    }

    public void addIncompleteJobs(int incompleteJobs) {
        incompleteJobsList.add(incompleteJobs);
        this.sumOfIncompleteJobs += incompleteJobs;
    }

    public void addStolenJobs(int stolenJobs) {
        stolenJobsList.add(stolenJobs);
        this.sumOfStolenJobs += stolenJobs;
    }

    @Override
    public String toString() {
        String stringRep = "";
        stringRep += groupName + " Finished and transferred jobs -> total: " + sumOfJobsFinishedAndTransferred + " avg: " + this.getAvgJobsFinishedAndTransferred() + " std: " + getStdJobsFinishedAndTransferred() + "\n";
        stringRep += groupName + " Stolen jobs -> total: " + sumOfStolenJobs + " avg: " + this.getAvgStolenJobs() + " std: " + getStdStolenJobs() + "\n";
        stringRep += groupName + " Incomplete jobs -> total:" + sumOfIncompleteJobs + " avg: " + this.getAvgIncompleteJobs() + " std: " + getStdIncompleteJobs() + "\n";
        return stringRep;
    }

    public float getStdStolenJobs() {
        return getStd(getAvgStolenJobs(), stolenJobsList);
    }

    public float getStdIncompleteJobs() {
        return getStd(getAvgIncompleteJobs(), incompleteJobsList);
    }

    public float getStdJobsFinishedAndTransferred() {
        return getStd(getAvgJobsFinishedAndTransferred(), jobsFinishedAndTransferredList);
    }

    private float getStd(float valuesAvg, List<Integer> values) {
        if (valuesAvg == 0) return 0.0f;

        if (values.size() == this.nodesQuantity) {
            double innerSum = 0;
            for (Integer value : jobsFinishedAndTransferredList) {
                innerSum += Math.pow(valuesAvg - value, 2);
            }
            return (float) Math.sqrt(innerSum / (double) (this.nodesQuantity - 1));
        } else {
            return Float.NaN;
        }
    }

    public void addNodes(int i) {
        nodesQuantity += i;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

}
