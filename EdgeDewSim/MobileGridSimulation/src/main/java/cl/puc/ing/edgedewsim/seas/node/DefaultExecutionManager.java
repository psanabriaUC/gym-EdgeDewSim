package cl.puc.ing.edgedewsim.seas.node;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;
import cl.puc.ing.edgedewsim.mobilegrid.jobs.JobStatsUtils;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.mobilegrid.node.ExecutionManager;
import cl.puc.ing.edgedewsim.simulator.Event;
import cl.puc.ing.edgedewsim.simulator.Logger;

import java.util.LinkedList;
import java.util.List;

/**
 * It represents the execution model. This is a simple model
 * that assigns the free CPU to one job. Basically, it calculates
 * how long it would take to execute the the job using the
 * following formular:
 * JobOps/(mips*(1-currentCPUUsage)
 * This model updates the job finish time for each new CPU usage event
 *
 * @author cuchillo
 */
public class DefaultExecutionManager implements ExecutionManager {

    private static final long NO_OPS = 0L;
    /**
     * List of assigned jobs pending to be executed.
     */
    private final List<Job> queuedJobs = new LinkedList<>();
    /**
     * The {@link Device} attached to this execution manager.
     */
    private Device device;
    /**
     * The processor speed in millions of instructions per second.
     */
    private long mips;
    /**
     * The fraction of the CPU currently in use by the end-user (and therefore unavailable for background operations),
     * as a value between 0 and 1, where 0 means the entire CPU is free for use, and 1 means it is completely hogged
     * by the user. Therefore, the actual speed of the processor can be estimated by:
     * {@link DefaultExecutionManager#mips} * (1 - cpu).
     */
    private double cpu;
    /**
     * The battery manager attached to this device.
     */
    private DefaultBatteryManager batteryManager;

    /**
     * Job currently being executed. Once assigned to this variable, the job should be removed from the
     * {@link DefaultExecutionManager#queuedJobs} list.
     */
    private Job executing;

    /**
     * The number of instructions executed by this device throughout the simulation. Used for logging purposes.
     */
    private long executedOps;

    /**
     * The simulation timestamp at which the {@link DefaultExecutionManager#lastEvent} was scheduled.
     */
    private long lastEventTime;

    /**
     * A reference to the {@link Device#EVENT_TYPE_FINISH_JOB} event that was scheduled by this class to simulate
     * the completion of the {@link Job} currently assigned to it.
     */
    private Event lastEvent;

    /**
     * The amount of {@link Job} completed by this device. Used for logging purposes.
     */
    private int finishedJob = 0;

    /**
     * Adds the job to the queue
     * checks if it can start to execute
     * notifies a possible change of profile to
     * the battery manager
     */
    @Override
    public void addJob(Job job) {
        this.queuedJobs.add(job);
        this.startExecute();
        if (isExecuting()) {
            this.batteryManager.onBeginExecutingJobs();
        } else {
            this.batteryManager.onStopExecutingJobs();
        }

    }

    @Override
    public int getQueuedJobs() {
        return this.queuedJobs.size();
    }

    @Override
    public Job removeJob(int index) {
        return this.queuedJobs.remove(index);
    }

    /**
     * Finishes the current Job
     * Tries to start a new one
     * Notifies to the battery manager a possible profile change
     */
    @Override
    public void onFinishJob(Job job) {
        long freeMips = this.getFreeMIPS();
        Logger.getInstance(device.getSimulation()).currentMIPS -= freeMips;
        Logger.getInstance(device.getSimulation()).writeMIPSLog("" + device.getSimulation().getTime());

        JobStatsUtils jobStatsUtils = JobStatsUtils.getInstance(device.getSimulation());

        this.finishedJob++;
        jobStatsUtils.success(job);
        Logger.getInstance(device.getSimulation()).logEntity(device, "The device finished the job", job,
                jobStatsUtils.timeToMinutes(jobStatsUtils.getJobStats(this.executing).getTotalExecutionTime()));
        this.executing = null;
        this.lastEvent = null;

        // Execute new job
        this.startExecute();
        if (isExecuting()) {
            // TODO: check if maybe we can eliminate this line.
            this.batteryManager.onBeginExecutingJobs();
        } else {
            this.batteryManager.onStopExecutingJobs();
            Logger.getInstance(device.getSimulation()).logEntity(device, "The device has become lazy");
        }

        /*Yisel Log*/
        Logger.getInstance(device.getSimulation()).logJob(job.getJobId(), device.getName(), device.getBatteryLevel(), job.getInputSize(), job.getOutputSize());

        Logger.getInstance(device.getSimulation()).writeLog("timestamps", "" + device.getSimulation().getTime());
    }

    /**
     * recalculates how much time it needs to finish the
     * current job
     */
    @Override
    public void onCPUEvent(double cpuUsage) {
        //If there is no job, there is nothing to do
        if (!this.isExecuting()) {
            this.cpu = cpuUsage;
            return;
        }
        //get the old free mips
        long freeMips = this.getFreeMIPS();
        Logger.getInstance(device.getSimulation()).currentMIPS -= freeMips;

        double freeMipms = this.getFreeMIPMS();
        //Calculates how many ops it has executed since the last event}
        //and updates the executed ops
        this.executedOps += (long) ((device.getSimulation().getTime() - this.lastEventTime) * freeMipms);
        if (this.executedOps > this.executing.getOps()) throw new IllegalStateException("It executed more ops (" +
                this.executedOps + ") than the size of the job (" + this.executing.getOps() + ")");
        double toExecute = this.executing.getOps() - this.executedOps;

        //Update process
        this.cpu = cpuUsage;
        freeMipms = this.getFreeMIPMS();
        freeMips = this.getFreeMIPS();

        Logger.getInstance(device.getSimulation()).currentMIPS += freeMips;
        Logger.getInstance(device.getSimulation()).writeMIPSLog("" + device.getSimulation().getTime());

        //Reestimate the remaining time using the new free mips
        double time = toExecute / freeMipms;
        time += device.getSimulation().getTime();
        //Updates the information in the simulator
        this.lastEventTime = device.getSimulation().getTime();
        device.getSimulation().removeEvent(this.lastEvent);
        this.lastEvent = Event.createEvent(Event.NO_SOURCE, (long) time, this.device.getId(),
                Device.EVENT_TYPE_FINISH_JOB, this.executing);
        device.getSimulation().addEvent(this.lastEvent);
    }

    /**
     * It is call when the device stop working
     * update the state of unfinished jobs to fail
     */
    @Override
    public void shutdown() {
        for (Job job : this.queuedJobs)
            JobStatsUtils.getInstance(device.getSimulation()).fail(job, NO_OPS);
        if (this.isExecuting()) {
            long freeMips = this.getFreeMIPS();
            Logger.getInstance(device.getSimulation()).currentMIPS -= freeMips;
            Logger.getInstance(device.getSimulation()).writeMIPSLog("" + device.getSimulation().getTime());

            JobStatsUtils.getInstance(device.getSimulation()).fail(this.executing, this.executedOps);
            device.getSimulation().removeEvent(this.lastEvent);
        }

        Logger.getInstance(device.getSimulation()).logEntity(device, "Device stopped. Failed jobs: " +
                (this.queuedJobs.size() + (this.isExecuting() ? 1 : 0)) + " finished jobs " + this.finishedJob);

        /*Yisel Log*/
        int failedJobs = this.queuedJobs.size() + (this.isExecuting() ? 1 : 0);
        Logger.getInstance(device.getSimulation()).logDevice(device.getName(),
                this.finishedJob + failedJobs,
                this.finishedJob,
                device.getCurrentTransfersCount(),
                device.getCurrentTotalTransferCount(),
                device.getWifiRSSI(),
                device.getEnergyPercentageWastedInNetworkActivity(),
                device.getInitialJoules(),
                device.getAccEnergyInTransferring());
    }

    // public int getActualCPUProfile() {
    // 	return this.isExecuting() ? 1 : 0;
    // }

    /**
     * Start to execute a new job
     */
    protected void startExecute() {
        //if there is no jobs in the queue or it is already running
        //there is nothing to do
        if (this.isExecuting() || this.queuedJobs.size() == 0) return;

        double freeMipms = this.getFreeMIPMS();
        long freeMips = this.getFreeMIPS();
        Logger.getInstance(device.getSimulation()).currentMIPS += freeMips;
        Logger.getInstance(device.getSimulation()).writeMIPSLog("" + device.getSimulation().getTime());

        //get the next job and update current information
        this.executing = this.queuedJobs.remove(0);
        Logger.getInstance(device.getSimulation()).logEntity(this.device, "The device start executing ", this.executing);
        JobStatsUtils.getInstance(device.getSimulation()).startExecute(this.executing);

        this.executedOps = 0;
        this.lastEventTime = device.getSimulation().getTime();
        //Calculate time to finish under current settings
        double time = ((double) this.executing.getOps()) / freeMipms;
        time += device.getSimulation().getTime();
        // updates the simulation
        this.lastEvent = Event.createEvent(Event.NO_SOURCE, (long) time, this.device.getId(),
                Device.EVENT_TYPE_FINISH_JOB, this.executing);
        device.getSimulation().addEvent(this.lastEvent);
    }

    // Getters and setters

    @Override
    public int getCurrentlyExecutingJobs() {
        return this.queuedJobs.size() + (this.isExecuting() ? 1 : 0);
    }

    @Override
    public double getCPUUsage() {
        return this.cpu;
    }

    public DefaultBatteryManager getBatteryManager() {
        return batteryManager;
    }

    public void setBatteryManager(DefaultBatteryManager batteryManager) {
        this.batteryManager = batteryManager;
    }

    protected boolean isExecuting() {
        return this.executing != null;
    }

    /**
     * Gets the free mips.
     */
    protected double getFreeMIPMS() {
        // mips are divided by 1000 to get the instructions per millisecond instead of per second
        double res = ((double) this.mips) / 1000d;
        return res * (1d - this.cpu);
    }

    protected long getFreeMIPS() {
        double res = this.mips * (1d - this.cpu);
        return (long) res;
    }

    @Override
    public long getMIPS() {
        return this.mips;
    }

    public void setMips(long mips) {
        this.mips = mips;
    }

    public Device getDevice() {
        return this.device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }
}
