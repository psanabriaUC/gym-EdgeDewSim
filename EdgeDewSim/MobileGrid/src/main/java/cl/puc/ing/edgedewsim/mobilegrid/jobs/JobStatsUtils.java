package cl.puc.ing.edgedewsim.mobilegrid.jobs;

import cl.puc.ing.edgedewsim.mobilegrid.network.UpdateMessage;
import cl.puc.ing.edgedewsim.mobilegrid.node.BatteryManager;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.mobilegrid.node.Node;
import cl.puc.ing.edgedewsim.mobilegrid.node.SchedulerProxy;
import cl.puc.ing.edgedewsim.mobilegrid.persistence.dbentity.DeviceTuple;
import cl.puc.ing.edgedewsim.mobilegrid.persistence.dbentity.JobStatsTuple;
import cl.puc.ing.edgedewsim.mobilegrid.persistence.IDevicePersister;
import cl.puc.ing.edgedewsim.mobilegrid.persistence.IPersisterFactory;
import cl.puc.ing.edgedewsim.mobilegrid.persistence.SQLSession;
import cl.puc.ing.edgedewsim.simulator.Entity;
import cl.puc.ing.edgedewsim.simulator.Logger;
import cl.puc.ing.edgedewsim.simulator.Simulation;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JobStatsUtils {

    private final static ConcurrentHashMap<UUID, JobStatsUtils> jobStatsUtilsHashMap = new ConcurrentHashMap<>();
    private static final Object lock = new Object();
    private final Map<Node, List<JobStats>> executed = new HashMap<>();
    private final Map<Node, NetStats> netStatsPerNode = new HashMap<>();
    private final Simulation simulation;
    public IPersisterFactory persistFactory;
    public int sim_id = -1;
    public long devicesIdleTime = 0;
    public double totalJobInputData = 0;
    public double totalJobOutputData = 0;
    private long timeStampLastJob = 0;
    private Map<Job, JobStats> stats = new HashMap<>();
    private HashMap<Node, NodeInformationSummary> nodesInformation = null;

    public JobStatsUtils(Simulation simulation) {
        this.simulation = simulation;
    }

    public static JobStatsUtils getInstance(Simulation simulation) {
        UUID uuid = simulation.getId();

        if (jobStatsUtilsHashMap.get(uuid) == null) {
            synchronized (lock) {
                if (jobStatsUtilsHashMap.get(uuid) == null) {
                    jobStatsUtilsHashMap.put(uuid, new JobStatsUtils(simulation));
                }
            }
        }

        return jobStatsUtilsHashMap.get(uuid);
    }

    public static void removeInstance(Simulation simulation) {
        UUID uuid = simulation.getId();
        synchronized (lock) {
            if (jobStatsUtilsHashMap.get(uuid) != null) {
                jobStatsUtilsHashMap.remove(uuid);
            }
        }
    }

    public void reset() {
        stats = new HashMap<>();
        executed.clear();
        netStatsPerNode.clear();
        devicesIdleTime = 0;
        totalJobInputData = 0;
        totalJobOutputData = 0;
        timeStampLastJob = 0;
        nodesInformation = null;
    }

    /**
     * Call when add a new job, if the job is already in the stats it does nothing
     */
    public void addJob(Job job, Node node) {
        if (stats.containsKey(job)) {
            return;
        }

        long time = simulation.getTime();
        JobStats stat = new JobStatsTuple(job.getJobId(), sim_id, time, node);
        stats.put(job, stat);
    }


    /**
     * Set Last Transfer time
     */
    public void changeLastTransferTime(Job job, long time, long startTime) {
        if (!stats.get(job).isSuccess())
            stats.get(job).setLastTransferTime(time, startTime);
        else
            stats.get(job).setLastResultTransferTime(time);
    }

    /**
     * Call when a job is transferred from a node to another
     */
    public void transfer(Job job, Node node, long time, long startTime) {
        stats.get(job).addTransfers(node, time, startTime);
    }

    public void setJobTransferCompleted(Job job, Node node) {
        stats.get(job).setJobTransferCompleted(node);
    }


    /**
     * Call when a device was selected for executing the job
     */
    public void setJobAssigned(Job job) {
        stats.get(job).setAssigned();
    }

    /**
     * Call when a job results are transferred from a node to another
     */
    public void transferResults(Job job, Node node, long time) {
        stats.get(job).addResultsTransfers(node, time);
    }

    /**
     * Call when a job starts to be executed
     */
    public void startExecute(Job job) {
        JobStats stat = stats.get(job);
        stat.setStartExecutionTime(simulation.getTime());
        Node n = stat.getTransfers().get(stat.getTransfers().size() - 1);
        List<JobStats> l = executed.computeIfAbsent(n, k -> new ArrayList<>());
        l.add(stat);
    }

    /**
     * Call when a job finished successfully
     */
    public void success(Job job) {
        JobStats jobStats = stats.get(job);
        jobStats.setFinishTime(simulation.getTime());
        jobStats.setExecutedMips(job.getOps());
        jobStats.setSuccess(true);
        jobStats.setFromEdge(job.isFromEdge());
        timeStampLastJob = simulation.getTime();
    }

    /**
     * Call when a job results was successfully transferred back to the proxy or origin node
     */
    public void successTransferBack(Job job) {
        stats.get(job).successTransferredBack();
    }

    /*
    public void transferBackInitiated(Job job) {
     if(stats.get(job).getTotalExecutionTime()==0) return;
     stats.get(job).transferredBackInitiated();

     }*/

    /**
     * Call when a job failed to finish
     */
    public void fail(Job job, long executedMips) {
        JobStats jobStats = stats.get(job);
        jobStats.setFinishTime(simulation.getTime());
        jobStats.setExecutedMips(executedMips);
        jobStats.setSuccess(false);
    }


    /**
     * Get the total time in the network
     */
    public long getTotalTransferTime() {
        long t = 0;
        for (Job job : stats.keySet())
            t += stats.get(job).getTotalTransferTime();
        return t;
    }

    /**
     * get the total time of jobs waiting
     */
    public long getTotalTime() {
        long t = 0;
        for (Job job : stats.keySet())
            t += stats.get(job).getTotalTime();
        return t;
    }

    /**
     * Gets the total time of jobs executing
     */
    public long getTotalExecutionTime() {
        long t = 0;
        for (Job job : stats.keySet())
            t += stats.get(job).getTotalExecutionTime();
        return t;
    }

    public float getAverageOPS() {
        float opsTotal = 0.0f;
        long n = 0;

        for (Job job : stats.keySet()) {
            JobStats stat = stats.get(job);

            if (stat.getFinishTime() != -1) {
                if (stat.isSuccess())
                    opsTotal += job.getOps() / (float)(stat.getFinishTime() - stat.getStartTime());
                ++n;
            }
        }
        return opsTotal / (n > 0 ? n : 1);
    }

    public float getAverageSuccessfulOPS() {
        float opsTotal = 0.0f;
        long n = 0;

        for (Job job : stats.keySet()) {
            JobStats stat = stats.get(job);

            if (stat.isSuccess()) {
                opsTotal += job.getOps() / (float) (stat.getFinishTime() - stat.getStartTime());
                ++n;
            }
        }
        return opsTotal / (n > 0 ? n : 1);
    }

    public float getTotalSuccessfulOPS() {
        float opsTotal = 0.0f;

        for (Job job : stats.keySet()) {
            JobStats stat = stats.get(job);

            if (stat.isSuccess()) {
                opsTotal += job.getOps() / (float) (stat.getFinishTime() - stat.getStartTime());
            }
        }
        return opsTotal;
    }

    /**
     * Gets the total execution time of jobs that successfully finished executing
     */
    public long getEffectiveExecutionTime() {
        long t = 0;
        for (Job job : stats.keySet())
            if (stats.get(job).isSuccess())
                t += stats.get(job).getTotalExecutionTime();
        return t;
    }

    public long getTimeStampLastJob() {
        return timeStampLastJob;
    }

    public void printJobStatesSummary() {
        this.printJobStatesSummary(System.out);
    }

    public void printJobStatesSummary(PrintStream output) {

        int total = 0;
        int completed = 0;
        int finishedButNotCompleted = 0;
        int startedButNotFinished = 0;
        int notScheduled = 0;
        int queued = 0;
        int scheduledButInterruptedTransfer = 0;
        int rejected = 0;
        int completedOnEdge = 0;
        double totalGIP = 0;

        for (Job job : stats.keySet()) {
            JobStats js = stats.get(job);
            total++;
            totalGIP += job.getOps() / (double) (1000000000);
            if (js.isRejected())
                rejected++;
            else {
                if (!js.isAssigned())
                    notScheduled++;
                else {
                    if (js.wasReceivedByAWorkerNode()) {
                        if (js.isCompleted()) {
                            completed++;
                            if (js.isFromEdge())
                                ++completedOnEdge;
                        } else {
                            if (js.executedSuccessButNotTransferredBack()) {
                                finishedButNotCompleted++;
                            } else {
                                if (!js.statedToExecute())
                                    queued++;
                                else
                                    startedButNotFinished++;
                            }
                        }
                    } else//These are jobs whose input could not be transferred to
                        //the device that were assigned to. Such transfer may or
                        //may not start.
                        scheduledButInterruptedTransfer++;
                }

            }


        }
        output.println("Total arrived jobs:" + total);
        output.println("Rejected:" + rejected);
        output.println("Scheduled but transfer interrupted:" + scheduledButInterruptedTransfer);
        output.println("Completed jobs:" + completed);
        if (completedOnEdge > 0) {
            output.println("Completed jobs on edge: " + completedOnEdge);
        }
        output.println("FinishedButNotCompleted jobs:" + finishedButNotCompleted);
        output.println("StartedButNotFinished jobs:" + startedButNotFinished);
        output.println("Queued jobs:" + queued);
        output.println("Not scheduled jobs:" + notScheduled);
        output.println("Checksum of jobs:" + (rejected + scheduledButInterruptedTransfer + completed + finishedButNotCompleted + startedButNotFinished + queued + notScheduled));

        /*Yisel Log*/
        double sentDataGB = getTotalTransferredData(true) / 1024;
        double receivedDataGB = getTotalTransferredData(false) / 1024;
        double percentEnergySendingData = getPercentageOfEnergyInSendingData();
        double percentEnergyReceivingData = getPercentageOfEnergyInReceivingData();
        double totalExecutedGIP = getTotalExecutedGIP();
        double totalDataToTransfer = getAggregatedJobsData(true) + getAggregatedJobsData(false);
        //Logger.logExperiment(total, total-notScheduled-rejected, finishedButNotCompleted + completed, completed, sentDataGB,receivedDataGB, percentEnergySendingData, percentEnergyReceivingData, gips,totalExecutedGIP);
        Logger.getInstance(simulation).logExperiment2(total,
                notScheduled + rejected,
                scheduledButInterruptedTransfer,
                queued,
                startedButNotFinished,
                finishedButNotCompleted,
                completed,
                sentDataGB,
                receivedDataGB,
                totalDataToTransfer,
                percentEnergySendingData,
                percentEnergyReceivingData,
                totalGIP,
                totalExecutedGIP);

        for (Job job : stats.keySet()) {
            JobStats js = stats.get(job);
            Logger.getInstance(simulation).logJobDetails(job.getJobId(), js.rejected, js.success, js.successTransferBack, js.startTime, js.getStartExecutionTime(), js.getFinishTime(), js.getQueueTime(), js.getTotalResultsTransferTime(), js.getTotalTransferTime());
        }
	
		/*double fitness =  (sentDataGB+receivedDataGB)/(totalDataToTransfer) + completed*1.0/total;
		output.println("Fitness:"+ fitness);*/

    }

    /**
     * Get the number of jobs that were successfully executed
     */
    public int getCompletedJobs() {
        int t = 0;
        for (Job job : stats.keySet())
            if (stats.get(job).isCompleted())
                t++;
        return t;
    }

    public long getCompletedJobsOps() {
        long t = 0;
        for (Job job : stats.keySet()) {
            if (stats.get(job).isCompleted())
                t += job.getOps();
        }

        return t;
    }

    public int getCompletedJobsInEdge() {
        int t = 0;
        for (Job job : stats.keySet())
            if (stats.get(job).isCompleted() && stats.get(job).isFromEdge())
                t++;
        return t;
    }

    public int getNotCompletedJobs() {
        int t = 0;

        for (Job job : stats.keySet()) {
            JobStats js = stats.get(job);

            if (js.isRejected() || !js.isAssigned())
                ++t;
            else if (js.wasReceivedByAWorkerNode()) {
                if (!js.isCompleted() && (js.executedSuccessButNotTransferredBack() || js.statedToExecute())) {
                    ++t;
                }
            } else
                ++t;
        }

        return t;
    }

    /**
     * Gets the total time of jobs that successfully finished executing
     */
    public long getEffectiveTotalTime() {
        long t = 0;
        for (Job job : stats.keySet())
            if (stats.get(job).isSuccess())
                t += stats.get(job).getTotalTime();
        return t;
    }

    /**
     * Gets the total queue time of jobs that successfully finished executing
     */
    public long getEffectiveQueueTime() {
        long t = 0;
        for (Job job : stats.keySet())
            if (stats.get(job).isSuccess())
                t += stats.get(job).getQueueTime();
        return t;
    }

    /**
     * Gets the total queue time of all the jobs
     */
    public long getTotalQueueTime() {
        long t = 0;
        for (Job job : stats.keySet())
            t += stats.get(job).getQueueTime();
        return t;
    }

    /**
     * Get the number of jobs that started to execute
     */
    public int getNumberOfJobsStarted() {
        int t = 0;
        for (Job job : stats.keySet())
            if (stats.get(job).statedToExecute())
                t++;
        return t;
    }

    /**
     * Get the number of jobs
     */
    public int getSize() {
        return stats.size();
    }

    /**
     * Get the number of jobs that were successfully executed
     */
    public int getSuccessfullyExecutedJobs() {
        int t = 0;
        for (Job job : stats.keySet())
            if (stats.get(job).isSuccess())
                t++;
        return t;
    }

    /**
     * Get registered Jobs
     */
    public Iterator<Job> getJob() {
        return stats.keySet().iterator();
    }

    /**
     * Return the stats for a Job
     */
    public JobStats getJobStats(Job j) {
        return stats.get(j);
    }

    public String timeToMinutes(long millis) {
        StringBuilder sb = new StringBuilder();
        double aux = Math.floor(millis / 60000d);
        sb.append((long) aux);
        sb.append(":");
        aux = (millis - aux * 60000) / 1000d;
        sb.append(aux);
        return sb.toString();
    }

    public String timeToHours(long millis) {
        StringBuilder sb = new StringBuilder();
        int h = (int) Math.floor(millis / (60 * 60 * 1000d));
        int m = (int) Math.floor(millis / (60 * 1000d));
        int s = (int) Math.floor(millis / 1000d);
        int ms = (int) (millis - s * 1000);
        s = s - m * 60;
        m = m - h * 60;
        sb.append(h);
        sb.append(':');
        sb.append(m);
        sb.append(':');
        sb.append(s);
        sb.append('.');
        sb.append(ms);
        return sb.toString();
    }

    public long getTotalResultsTransferTime() {
        long t = 0;
        for (Job job : stats.keySet())
            t += stats.get(job).getTotalResultsTransferTime();
        return t;
    }

    public int cantJobTransfers() {
        int t = 0;
        for (Job job : stats.keySet())
            t += stats.get(job).getTotalTransfers();
        return t;
    }

    public int cantJobSuccessButNotTransferred() {
        int t = 0;
        for (Job job : stats.keySet())
            t += stats.get(job).executedSuccessButNotTransferredBack() ? 1 : 0;
        return t;
    }

    public int cantJobResultTransfers() {
        int t = 0;
        for (Job job : stats.keySet())
            t += stats.get(job).getTotalResultTransfers();
        return t;
    }

    public Set<Job> getJobs() {
        return stats.keySet();
    }

    public List<JobStats> getJobStats() {
        List<JobStats> jobs = new ArrayList<>(stats.size());
        for (Job j : stats.keySet())
            jobs.add(stats.get(j));
        return jobs;
    }

    public List<JobStats> getJobStatsExecutedIn(Node n) {
        return executed.computeIfAbsent(n, k -> new ArrayList<>());
    }

    /**
     * this method allows to register an amount of energy wasted in sending data for a given node. The energy in sending data spent by the node increases with every call to this method.
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean registerSendingDataEnergy(Node node, double joulesInSendingData, double megabytesOfSentData) {
        NetStats energyCosts = netStatsPerNode.get(node);
        if (energyCosts != null) {
            energyCosts.addJoulesInSendingData(joulesInSendingData);
            energyCosts.addMegabytesSent(megabytesOfSentData);
        } else {
            double maxJoulesOfDevice = Double.MAX_VALUE;

            if (node.runsOnBattery()) {
                Device d = ((Device) node);
                maxJoulesOfDevice = (((double) d.getInitialSOC() / (double) BatteryManager.PROFILE_ONE_PERCENT_REPRESENTATION) * (double) d.getTotalBatteryCapacityInJoules()) / (double) 100;
            }
            energyCosts = new NetStats(maxJoulesOfDevice, joulesInSendingData, 0.0d, megabytesOfSentData, 0.0d);
            netStatsPerNode.put(node, energyCosts);
        }
        return energyCosts.isMaximumAvailableJoulesExceeded();
    }

    /**
     * this method allows to register an amount of energy wasted and amount of data Received for a given node.
     * The energy and received data is added to the previously registered values of the corresponding node.
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean registerReceivingDataEnergy(Node node, double joulesInReceivingData, double megabytesOfReceivedData) {
        NetStats energyCosts = netStatsPerNode.get(node);
        if (energyCosts != null) {
            energyCosts.addMegabytesReceived(megabytesOfReceivedData);
            energyCosts.addJoulesInReceivingData(joulesInReceivingData);
        } else {
            double maxJoulesOfDevice = Double.MAX_VALUE;

            if (node.runsOnBattery()) {
                Device d = ((Device) node);
                maxJoulesOfDevice = (((double) d.getInitialSOC() / (double) BatteryManager.PROFILE_ONE_PERCENT_REPRESENTATION) * (double) d.getTotalBatteryCapacityInJoules()) / (double) 100;
            }

            energyCosts = new NetStats(maxJoulesOfDevice, 0.0d, joulesInReceivingData, 0.0d, megabytesOfReceivedData);
            netStatsPerNode.put(node, energyCosts);
        }
        return energyCosts.isMaximumAvailableJoulesExceeded();

    }

    public double getTotalEnergyInDataTransmission() {
        double totalEnergyInDataTransmission = 0;
        for (Node device : netStatsPerNode.keySet()) {
            if (device != null && device.runsOnBattery()) {
                NetStats energyCost = netStatsPerNode.get(device);
                totalEnergyInDataTransmission += energyCost.getAccJoulesInReceivingData();
                totalEnergyInDataTransmission += energyCost.getAccJoulesInSendingData();
            }
        }
        return totalEnergyInDataTransmission;
    }

    public double getPercentageOfEnergyInDataTransmission() {
        return (getTotalEnergyInDataTransmission() * (double) 100) / getGridBatteryPoweredEnergy();
    }

    public void registerUpdateMessage(Node scr, UpdateMessage data) {
        NetStats netStats = netStatsPerNode.get(scr);
        if (netStats != null) {
            netStats.setUpdateMsgCount(netStats.getUpdateMsgCount() + 1);
        } else {
            netStats = new NetStats();
            netStats.setUpdateMsgCount(netStats.getUpdateMsgCount() + 1);
            netStatsPerNode.put(scr, netStats);
        }
    }

    /**
     * returns the total executed job instructions expressed in billion of instructions (GIP)
     */
    public double getTotalExecutedGIP() {
        List<JobStats> jobStats = getJobStats();
        double executedInstructions = 0.0d;
        for (JobStats jobStat : jobStats) {
            if (!jobStat.isRejected() || !jobStat.isSuccess())
                executedInstructions += ((double) jobStat.getExecutedMips()) / (double) (1000000000);
        }
        return executedInstructions;
    }

    public int getTotalUpdateMsgSentByNodes() {
        int totalUpdateMsg = 0;
        for (Node node : netStatsPerNode.keySet()) {
            if (node.runsOnBattery()) {
                totalUpdateMsg += netStatsPerNode.get(node).getUpdateMsgCount();
            }
        }
        return totalUpdateMsg;
    }

    public int getTotalUpdateMsgReceivedByProxy() {
        NetStats proxyStats = netStatsPerNode.get(SchedulerProxy.getProxyInstance(simulation));
        return proxyStats != null ? proxyStats.getUpdateMsgCount() : 0;
    }

    public double getGridBatteryPoweredEnergy() {
        double gridTotalEnergy = 0;
        for (Node device : netStatsPerNode.keySet()) {
            if (device != null && device.runsOnBattery()) {
                Device dev = (Device) device;
                gridTotalEnergy += (((dev.getInitialSOC() / (double) BatteryManager.PROFILE_ONE_PERCENT_REPRESENTATION) * dev.getTotalBatteryCapacityInJoules()) / (double) 100);
            }
        }
        return gridTotalEnergy;
    }

    public double getPercentageOfEnergyInSendingData() {
        return (getTotalEnergyInSendingData() * (double) 100) / getGridBatteryPoweredEnergy();
    }

    public double getTotalEnergyInSendingData() {
        double totalEnergyInDataTransmission = 0;
        for (Node device : netStatsPerNode.keySet()) {
            if (device != null && device.runsOnBattery()) {
                NetStats energyCost = netStatsPerNode.get(device);
                totalEnergyInDataTransmission += energyCost.getAccJoulesInSendingData();
            }
        }
        return totalEnergyInDataTransmission;
    }

    public double getPercentageOfEnergyInReceivingData() {
        return (getTotalEnergyInReceivingData() * (double) 100) / getGridBatteryPoweredEnergy();
    }

    public double getTotalEnergyInReceivingData() {
        double totalEnergyInDataTransmission = 0;
        for (Node device : netStatsPerNode.keySet()) {
            if (device != null && device.runsOnBattery()) {
                NetStats energyCost = netStatsPerNode.get(device);
                totalEnergyInDataTransmission += energyCost.getAccJoulesInReceivingData();
            }
        }
        return totalEnergyInDataTransmission;
    }

    public double getTotalTransferredData(boolean sent) {
        double totalData = 0;
        for (Node device : netStatsPerNode.keySet()) {
            if (device != null && device.runsOnBattery()) {
                NetStats energyCost = netStatsPerNode.get(device);
                if (sent)
                    totalData += energyCost.getAccMegabytesSent();
                else
                    totalData += energyCost.getAccMegabytesReceived();
            }
        }
        return totalData;
    }

    public double getPercentOfTransferredData() {
        double sentData = getTotalTransferredData(true) / 1024;
        double receivedData = getTotalTransferredData(false) / 1024;
        double totalData = getAggregatedJobsData(true) + getAggregatedJobsData(false);
        return (sentData + receivedData) * 100 / totalData;
    }

    /**
     * this method returns the data input represented in gb by all jobs that arrived to the grid.
     * With dataInput in true the aggregated job data input is returned while when it is in false
     * the aggregated job data output is returned.
     */
    public double getAggregatedJobsData(boolean dataInput) {
        double totalJobsData = 0d;
        for (Job job : stats.keySet()) {
            totalJobsData += dataInput ? job.getInputSize() : job.getOutputSize();
        }
        return totalJobsData / (1024 * 1024 * 1024);
    }

    private void generateNodeInformationSummary() {
        if (nodesInformation == null)
            nodesInformation = new HashMap<>();

        Collection<JobStats> jobs = stats.values();
        for (JobStats jobStats : jobs) {
            List<Node> transfers = jobStats.getTransfers();
            Node lastNode = transfers.get(transfers.size() - 1);
            if (((Entity) lastNode).getName().compareTo("PROXY") != 0) {
                NodeInformationSummary lastNodeInfo = getNodeInfoSummary(lastNode);

                if (jobStats.isCompleted())
                    lastNodeInfo.setFinishedAndTransferredJobs(lastNodeInfo.getFinishedAndTransferredJobs() + 1);
                if (!jobStats.isCompleted()) lastNodeInfo.setIncompleteJobs(lastNodeInfo.getIncompleteJobs() + 1);
                if (!jobStats.statedToExecute()) lastNodeInfo.setNotStartedJobs(lastNodeInfo.getNotStartedJobs() + 1);
                lastNodeInfo.setJobExecutionTime(lastNodeInfo.getJobExecutionTime() + jobStats.getTotalExecutionTime());

                if (transfers.size() == 3)//if the last node stole the job to the first
                    lastNodeInfo.setStolenJobs(lastNodeInfo.getStolenJobs() + 1);
                else {
                    if (transfers.size() > 3) {
                        for (int i = 2; i < transfers.size(); i++) {//i starts in 2 because the transfer between the proxy and the first node does not count as a steal
                            NodeInformationSummary nodeInfo = getNodeInfoSummary(transfers.get(i));
                            nodeInfo.setStolenJobs(nodeInfo.getStolenJobs() + 1);
                        }
                    }
                }

            }
        }
    }

    public void printNodeInformationSummary(PrintStream output) {
        if (nodesInformation == null)
            generateNodeInformationSummary();
        output.println("--------------Node information summaries-----------------");
        Set<Node> nodes = nodesInformation.keySet();
        for (Node node : nodes) {
            NodeInformationSummary nodeInfo = nodesInformation.get(node);
            output.println("/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*/*");
            output.println("Node type: " + ((Device) node).getMIPS());
            output.print(nodeInfo.toString());
        }
        output.println("------------------------------------------");
    }

    private NodeInformationSummary getNodeInfoSummary(Node node) {
        if (!nodesInformation.containsKey(node))
            nodesInformation.put(node, new NodeInformationSummary(((Device) node).getName(), ((Device) node).getMIPS()));
        return nodesInformation.get(node);
    }


    public void printNodeInformationSummaryByNodeMips(PrintStream output) {
        if (nodesInformation == null)
            generateNodeInformationSummary();

        HashMap<Long, NodesGroupInformationSummary> groupsInfo = new HashMap<>();

        for (NodeInformationSummary nodeInfo : nodesInformation.values()) {
            if (!groupsInfo.containsKey(nodeInfo.getMips())) {
                groupsInfo.put(nodeInfo.getMips(), new NodesGroupInformationSummary(String.valueOf(nodeInfo.getMips())));
            }

            NodesGroupInformationSummary nodesGroupInfo = groupsInfo.get(nodeInfo.getMips());
            nodesGroupInfo.addFinishedTransferredJobs(nodeInfo.getFinishedAndTransferredJobs());
            nodesGroupInfo.addStolenJobs(nodeInfo.getStolenJobs());
            nodesGroupInfo.addIncompleteJobs(nodeInfo.getIncompleteJobs());
            nodesGroupInfo.addNodes(1);
        }

        output.println("--------------Node information groups summaries-----------------");
        Set<Long> nodes = groupsInfo.keySet();
        for (Long nodeGroupMips : nodes) {
            NodesGroupInformationSummary nodesGroupInfo = groupsInfo.get(nodeGroupMips);
            output.println("************************************");
            output.println("Node mips: " + nodeGroupMips);
            output.print(nodesGroupInfo.toString());
        }
        output.println("----------------------------------------------------------------");

    }

    public void printNodeInformationSummaryByNodeMips() {
        printNodeInformationSummaryByNodeMips(System.out);
    }

    public void storeInDB() {
        IDevicePersister devicePersist = persistFactory.getDevicePersister();
        SQLSession session = devicePersist.openSQLSession();

        devicePersist.insertInMemoryDeviceTuples(session);
        session.commit();

        Collection<JobStats> jobs = stats.values();
        for (JobStats job : jobs) {
            JobStatsTuple jobStats = (JobStatsTuple) job;
            jobStats.persist(session);
        }
        session.commit();
        session.close();
    }


    public void deviceJoinTopology(Device device, long startTime) {
        DeviceTuple dt = persistFactory.getDevicePersister().getDevice(device.getName());
        if (dt != null)
            dt.setJoin_topology_time(startTime);
    }


    public void deviceLeftTopology(Device device, long leftTime) {
        DeviceTuple dt = persistFactory.getDevicePersister().getDevice(device.getName());
        if (dt != null)
            dt.setLeft_topology_time(leftTime);
    }

    public int getSim_id() {
        return sim_id;
    }

    public void setSim_id(int sim_id) {
        this.sim_id = sim_id;
    }

    public void incIdleTime(long l) {
        devicesIdleTime += l;
    }

    /**
     * Remove comment for testing purposes
     */
    public void printNodesPercentageOfEnergyWasteInNetworkActivity(PrintStream output) {
        for (Node device : netStatsPerNode.keySet()) {
            if (device != null && device.runsOnBattery()) {
                Device dev = (Device) device;
                output.println("Device " + dev.getName() + " " + dev.getEnergyPercentageWastedInNetworkActivity());
            }
        }
    }

    public void printNodesPercentageOfEnergyWasteInNetworkActivity() {
        this.printJobStatesSummary(System.out);
    }

    public int getTotalStealing() {
        if (nodesInformation == null)
            generateNodeInformationSummary();
        int stealing = 0;

        for (NodeInformationSummary nodeInfo : nodesInformation.values()) {
            stealing += nodeInfo.getStolenJobs();
        }
        return stealing;
    }

    /**
     * The method is invoked when a job is left out of the scheduling because
     * there are not devices able to handle the job.
     */
    public void rejectJob(Job job, long rejectTime) {
        JobStats js = stats.get(job);
        js.setRejected(true);
        js.setExecutedMips(0);
        js.setFinishTime(rejectTime);
    }


}
