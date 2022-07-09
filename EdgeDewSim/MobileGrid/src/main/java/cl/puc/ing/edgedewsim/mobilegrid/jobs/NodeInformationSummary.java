package cl.puc.ing.edgedewsim.mobilegrid.jobs;

public class NodeInformationSummary {

    protected int finishedAndTransferredJobs = 0;
    protected int incompleteJobs = 0;
    protected int stolenJobs = 0;
    private String name = "";
    private long mips = -1;
    private int notStartedJobs = 0;
    private double jobExecutionTime = 0;//expressed in milliseconds.
    private double idleTime = 0; //expressed in milliseconds.


    public NodeInformationSummary(String name, long mips) {
        this.name = name;
        this.mips = mips;
    }

    public NodeInformationSummary() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getMips() {
        return mips;
    }

    public void setMips(long mips) {
        this.mips = mips;
    }

    public int getFinishedAndTransferredJobs() {
        return finishedAndTransferredJobs;
    }

    public void setFinishedAndTransferredJobs(int finishedAndTransferredJobs) {
        this.finishedAndTransferredJobs = finishedAndTransferredJobs;
    }

    public int getIncompleteJobs() {
        return incompleteJobs;
    }

    public void setIncompleteJobs(int incompleteJobs) {
        this.incompleteJobs = incompleteJobs;
    }

    public int getNotStartedJobs() {
        return notStartedJobs;
    }

    public void setNotStartedJobs(int notStartedJobs) {
        this.notStartedJobs = notStartedJobs;
    }

    public double getJobExecutionTime() {
        return jobExecutionTime;
    }

    public void setJobExecutionTime(double jobExecutionTime) {
        this.jobExecutionTime = jobExecutionTime;
    }

    public double getIdleTime() {
        return idleTime;
    }

    public void setIdleTime(double idleTime) {
        this.idleTime = idleTime;
    }

    public int getStolenJobs() {
        return stolenJobs;
    }

    public void setStolenJobs(int stolenJobs) {
        this.stolenJobs = stolenJobs;
    }

    @Override
    public String toString() {
        String stringRep = "";
        stringRep += "Finished and transferred jobs: " + this.finishedAndTransferredJobs + "\n";
        stringRep += "Incomplete jobs: " + this.incompleteJobs + "\n";
        stringRep += "Not started jobs: " + this.notStartedJobs + "\n";
        stringRep += "Stolen jobs: " + this.stolenJobs + "\n";
        stringRep += "Job execution time (sec): " + this.jobExecutionTime / (double) 1000 + "\n";
        return stringRep;
    }

}
