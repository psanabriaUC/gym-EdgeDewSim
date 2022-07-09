package cl.puc.ing.edgedewsim.mobilegrid.node;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;

public interface ExecutionManager {
    /**
     * Call when a new jobs arrives
     *
     * @param job Job to add
     */
    void addJob(Job job);

    /**
     * get the number of jobs enqueue
     *
     * @return nothing
     */
    int getQueuedJobs();

    /**
     * removes a job
     *
     * @param index index of job to remove
     */
    Job removeJob(int index);

    /**
     * Call when a job is finished
     *
     * @param job to mark as finished
     */
    void onFinishJob(Job job);

    /**
     * Call when a CPU event arrives
     *
     * @param cpuUsage current CPU Usage
     */
    void onCPUEvent(double cpuUsage);

    /**
     * Get number of jobs currently executing
     *
     * @return number of jobs
     */
    int getCurrentlyExecutingJobs();

    /**
     * Get cpu mips
     *
     * @return CPU MIPS
     */
    long getMIPS();

    /**
     * Get current cpu usage
     *
     * @return current CPU Usage
     */
    double getCPUUsage();

    /**
     * Call when the device shutdown
     */
    void shutdown();
}
