package cl.puc.ing.edgedewsim.server.proxy;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;
import cl.puc.ing.edgedewsim.mobilegrid.jobs.JobStatsUtils;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.mobilegrid.node.SchedulerProxy;
import cl.puc.ing.edgedewsim.server.data.DeviceData;
import cl.puc.ing.edgedewsim.server.data.JobData;
import cl.puc.ing.edgedewsim.server.data.StatisticsData;
import cl.puc.ing.edgedewsim.simulator.Event;
import cl.puc.ing.edgedewsim.simulator.Logger;
import cl.puc.ing.edgedewsim.simulator.Simulation;

import java.io.*;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class RemoteLoadBalancing extends SchedulerProxy {
    private final DataOutputStream output;
    private final DataInputStream input;
    private final HashMap<String, Long> assignedJobs = new HashMap<>();
    private final HashMap<String, DeviceData> deviceStats = new HashMap<>();
    private final HashMap<Long, String> deviceIdMap = new HashMap<>();
    private final AtomicLong autoIncrement = new AtomicLong(101);
    private boolean firstJob = true;

    public RemoteLoadBalancing(String name, Simulation simulation) {
        super(name, simulation);
        output = simulation.getOutput();
        input = simulation.getInput();
    }

    @Override
    public void processEvent(Event event) {
        if (EVENT_END_JOB_ARRIVAL_BURST == event.getEventType())
            return;
        if (event.getEventType() != EVENT_JOB_ARRIVE)
            throw new IllegalArgumentException("Unexpected event");
        Job job = event.getData();
        JobStatsUtils.getInstance(simulation).addJob(job, this);
        Logger.getInstance(simulation).logEntity(this, "Job arrived to edge proxy", Objects.requireNonNull(job).getJobId());
        assignJob(job);
    }

    private void assignJob(Job job) {
        if (this.devices.isEmpty()) {
            JobStatsUtils.getInstance(simulation).rejectJob(job, simulation.getTime());
            Logger.getInstance(simulation).logEntity(this, "Job rejected = " + job.getJobId() + " at " + simulation.getTime() +
                    " simulation time");
        } else {
            try {
                output.writeInt(Simulation.NEXT_COMMAND);
                output.writeInt(this.devices.size());

                for (Device device : this.devices.values()) {
                    DeviceData deviceData = deviceStats.get(device.getName());

                    if (deviceData == null) {
                        deviceData = new DeviceData(autoIncrement.getAndIncrement());

                        deviceData.setName(device.getName());
                        deviceData.setMips(device.getMIPS());
                        deviceStats.put(device.getName(), deviceData);
                        deviceIdMap.put(deviceData.getId(), device.getName());
                    }

                    deviceData.setUptime(getLastReportedSOC(device));
                    deviceData.setNJobs(getIncomingJobs(device) + device.getNumberOfJobs() + 1);
                    deviceData.setRemainingBattery(device.getBatteryLevel());
                    deviceData.setHasBattery(device.runsOnBattery());
                    deviceData.setCpuUsage(device.getCPUUsage());
                    deviceData.setAssignedJobs(assignedJobs.get(device.getName()));

                    if (firstJob)
                        deviceData.printFullData(output);
                    else
                        deviceData.printData(output);
                    output.flush();
                }

                if (firstJob) {
                    firstJob = false;
                }

                JobData jobData = new JobData();

                jobData.setOps(job.getOps());
                jobData.setInputSize(job.getInputSize());
                jobData.setOutputSize(job.getOutputSize());
                jobData.printData(output);

                StatisticsData statisticsData = new StatisticsData();

                statisticsData.setJobs(JobStatsUtils.getInstance(simulation).getSize());
                statisticsData.setCompletedJobs(JobStatsUtils.getInstance(simulation).getCompletedJobs());
                statisticsData.setCompletedOps(JobStatsUtils.getInstance(simulation).getCompletedJobsOps());
                statisticsData.setTime(simulation.getTime());
                statisticsData.setTotalSuccessfulOps(JobStatsUtils.getInstance(simulation).getTotalSuccessfulOPS());

                statisticsData.printData(output);
                output.flush();
                int nextCommand = input.readInt();

                if (nextCommand == Simulation.RESET_COMMAND) {
                    System.out.println("Hai! Reset received while scheduling");
                    simulation.fullReset();
                } else {
                    long selectedId = input.readLong();
                    Device selectedDevice = devices.get(deviceIdMap.get(selectedId));

                    if (selectedDevice != null) {
                        assignedJobs.put(selectedDevice.getName(), assignedJobs.get(selectedDevice.getName()) + job.getOps());
                        queueJobTransferring(selectedDevice, job);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                simulation.fullReset();
            }

        }
    }

    @Override
    public void addDevice(Device device) {
        super.addDevice(device);

        assignedJobs.put(device.getName(), 0L);
        device.setOnFinishJobListener((device1, job) -> {
            long ops = assignedJobs.get(device1.getName()) - job.getOps();
            if (ops < 0)
                throw new IllegalStateException("Negative remaining OPS");
            assignedJobs.put(device1.getName(), ops);
        });
    }

    @Override
    public boolean runsOnBattery() {
        return false;
    }

    @Override
    public void reset() {
        super.reset();
        assignedJobs.clear();
        autoIncrement.set(0);
    }
}
