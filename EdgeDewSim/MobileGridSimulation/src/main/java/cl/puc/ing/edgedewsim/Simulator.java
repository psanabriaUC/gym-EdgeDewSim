package cl.puc.ing.edgedewsim;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;
import cl.puc.ing.edgedewsim.mobilegrid.jobs.JobStatsUtils;
import cl.puc.ing.edgedewsim.mobilegrid.network.NetworkModel;
import cl.puc.ing.edgedewsim.mobilegrid.network.SimpleNetworkModel;
import cl.puc.ing.edgedewsim.mobilegrid.network.WifiLink;
import cl.puc.ing.edgedewsim.mobilegrid.node.*;
import cl.puc.ing.edgedewsim.mobilegrid.persistence.dbentity.DeviceTuple;
import cl.puc.ing.edgedewsim.mobilegrid.persistence.dbentity.JobStatsTuple;
import cl.puc.ing.edgedewsim.mobilegrid.persistence.IPersisterFactory;
import cl.puc.ing.edgedewsim.persistence.mybatis.MybatisPersisterFactory;
import cl.puc.ing.edgedewsim.seas.reader.DeviceReader;
import cl.puc.ing.edgedewsim.seas.reader.SimReader;
import cl.puc.ing.edgedewsim.simulator.Logger;
import cl.puc.ing.edgedewsim.simulator.Simulation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


public class Simulator {
    public static final Object o = new Object();

    public static void main(String[] args) {
        Simulation simulation = new Simulation();

        if (args.length == 3) {

            System.err.println("Waiting for 10 sec");
            synchronized (o) {
                try {
                    o.wait(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.err.println("Executing");
        }
        setPersisters(simulation);
        Thread.setDefaultUncaughtExceptionHandler((arg0, arg1) -> {
            arg1.printStackTrace();
            System.exit(1);
        });

        //uncomment for debugging
        /*OutputStream debugFile = null;
         try {
         debugFile = new FileOutputStream("DebugLog.log");
         } catch (FileNotFoundException e) {
         e.printStackTrace();
         }
         Logger.setDebugOutputStream(debugFile);
         */

        //((SimpleNetworkModel)NetworkModel.getModel()).setDefaultLink(new IdealBroadCastLink());


        boolean storeInDB = false;
        if (args.length == 2) storeInDB = Boolean.parseBoolean(args[1]);
        SimReader simReader = new SimReader(simulation);

        simReader.read(args[0], storeInDB);
        JobStatsUtils.getInstance(simulation).setSim_id(SimReader.getSim_id());

        String cnfPath = args[0];
        String[] cnfPathArr = cnfPath.split("/");
        cnfPath = cnfPathArr[cnfPathArr.length - 1];
        cnfPathArr = cnfPath.split("-");
        cnfPath = cnfPathArr[0];
        Logger.getInstance(simulation).EXPERIMENT = cnfPath;
        Logger.getInstance(simulation).VERBOSE = false;
        Logger.getInstance(simulation).LOG_FILES = true;

        CloudNode cloudNode = CloudNode.getCloudNode(simulation);
        Set<Node> cloudNodeSet = new HashSet<>();
        cloudNodeSet.add(cloudNode);

        for (Node node : NetworkModel.getModel(simulation).getNodes()) {
            if (node instanceof Device) {
                short rssi = ((Device) node).getWifiRSSI();
                Set<Node> nodeSet = new HashSet<>();
                nodeSet.add(node);

                Set<Node> proxySet = new HashSet<>();
                proxySet.add(SchedulerProxy.getProxyInstance(simulation));

                WifiLink wl1 = new WifiLink(rssi, nodeSet, proxySet, simulation);
                WifiLink wl2 = new WifiLink(rssi, proxySet, nodeSet, simulation);
                WifiLink wl3 = new WifiLink(rssi, nodeSet, cloudNodeSet, simulation);


                NetworkModel.getModel(simulation).addNewLink(wl1);
                NetworkModel.getModel(simulation).addNewLink(wl2);
                NetworkModel.getModel(simulation).addNewLink(wl3);
            }
        }

        simulation.runSimulation();


        if (storeInDB) {
            JobStatsUtils.getInstance(simulation).storeInDB();
        }

        //Logger.flushDebugInfo();

        JobStatsUtils.getInstance(simulation).printNodeInformationSummaryByNodeMips();
        System.out.print("Total simulated time: ");
        System.out.println(simulation.getTime());
        System.out.println(simulation.getTime());
        System.out.print("Jobs simulated: ");
        System.out.println(JobStatsUtils.getInstance(simulation).getSize());
        System.out.print("Jobs successfully executed: ");
        System.out.println(JobStatsUtils.getInstance(simulation).getSuccessfullyExecutedJobs());
        System.out.print("Last successful task time: ");
        System.out.println(JobStatsUtils.getInstance(simulation).getTimeStampLastJob());
        System.out.println(JobStatsUtils.getInstance(simulation).timeToHours(JobStatsUtils.getInstance(simulation).getTimeStampLastJob()));
        System.out.print("Successfully execution time: ");
        System.out.println(JobStatsUtils.getInstance(simulation).getEffectiveExecutionTime());
        System.out.println(JobStatsUtils.getInstance(simulation).timeToHours(JobStatsUtils.getInstance(simulation).getEffectiveExecutionTime()));
        System.out.print("Successfully execution time per effective job: ");
        int execJobs = JobStatsUtils.getInstance(simulation).getSuccessfullyExecutedJobs();
        if (execJobs > 0) {
            System.out.println(JobStatsUtils.getInstance(simulation).getEffectiveExecutionTime() / execJobs);
            System.out.println(JobStatsUtils.getInstance(simulation).timeToHours(JobStatsUtils.getInstance(simulation).getEffectiveExecutionTime() / execJobs));
        } else {
            System.out.println(JobStatsUtils.getInstance(simulation).timeToHours(execJobs));
            System.out.println(JobStatsUtils.getInstance(simulation).timeToHours(execJobs));
        }


        System.out.print("Executed job waiting time: ");
        System.out.println(JobStatsUtils.getInstance(simulation).getEffectiveQueueTime());
        System.out.println(JobStatsUtils.getInstance(simulation).timeToHours(JobStatsUtils.getInstance(simulation).getEffectiveQueueTime()));
        System.out.print("Executed job waiting time per effective: ");
        if (execJobs > 0) {
            System.out.println(JobStatsUtils.getInstance(simulation).getEffectiveQueueTime() / JobStatsUtils.getInstance(simulation).getSuccessfullyExecutedJobs());
            System.out.println(JobStatsUtils.getInstance(simulation).timeToHours(JobStatsUtils.getInstance(simulation).getEffectiveQueueTime() / JobStatsUtils.getInstance(simulation).getSuccessfullyExecutedJobs()));
        } else {
            System.out.println(execJobs);
            System.out.println(JobStatsUtils.getInstance(simulation).timeToHours(execJobs));
        }

        System.out.println("*****************************");
        System.out.print("Total queue time: ");
        System.out.println(JobStatsUtils.getInstance(simulation).getTotalQueueTime());
        System.out.println(JobStatsUtils.getInstance(simulation).timeToHours(JobStatsUtils.getInstance(simulation).getTotalQueueTime()));
        long avgQueueTimePerJob = JobStatsUtils.getInstance(simulation).getSize() == 0 ? 0 : JobStatsUtils.getInstance(simulation).getTotalQueueTime() / JobStatsUtils.getInstance(simulation).getSize();
        System.out.println("Average queue time per job: " + avgQueueTimePerJob);

        System.out.println(JobStatsUtils.getInstance(simulation).timeToHours(avgQueueTimePerJob));
        System.out.print("Total execution time: ");
        System.out.println(JobStatsUtils.getInstance(simulation).getTotalExecutionTime());
        System.out.println(JobStatsUtils.getInstance(simulation).timeToHours(JobStatsUtils.getInstance(simulation).getTotalExecutionTime()));

        long avgExecTimePerJob = JobStatsUtils.getInstance(simulation).getSize() == 0 ? 0 : JobStatsUtils.getInstance(simulation).getTotalExecutionTime() / JobStatsUtils.getInstance(simulation).getSize();
        System.out.println("Average execution time per job: " + avgExecTimePerJob);
        System.out.println(JobStatsUtils.getInstance(simulation).timeToHours(avgExecTimePerJob));

        System.out.println("*****************************");
        int failed = JobStatsUtils.getInstance(simulation).getSize() - JobStatsUtils.getInstance(simulation).getSuccessfullyExecutedJobs();
        long wastedWaited = JobStatsUtils.getInstance(simulation).getTotalQueueTime() - JobStatsUtils.getInstance(simulation).getEffectiveQueueTime();
        long wastedExecution = JobStatsUtils.getInstance(simulation).getTotalExecutionTime() - JobStatsUtils.getInstance(simulation).getEffectiveExecutionTime();
        System.out.print("Failed jobs: ");
        System.out.println(failed);
        System.out.print("Wasted queue time: ");
        System.out.println(wastedWaited);
        System.out.println(JobStatsUtils.getInstance(simulation).timeToHours(wastedWaited));
        System.out.print("Average wasted queue time per failed job: ");
        long wastedDivFailed = failed != 0 ? wastedWaited / failed : 0;
        System.out.println(wastedDivFailed);
        String wasteDivFailedInHours = failed != 0 ? JobStatsUtils.getInstance(simulation).timeToHours(wastedWaited / failed) : "0";
        System.out.println(wasteDivFailedInHours);
        System.out.print("Wasted execution time: ");
        System.out.println(wastedExecution);
        System.out.println(JobStatsUtils.getInstance(simulation).timeToHours(wastedExecution));
        System.out.print("Average wasted execution time per failed job: ");
        wastedDivFailed = failed != 0 ? wastedExecution / failed : 0;
        System.out.println(wastedDivFailed);
        wasteDivFailedInHours = failed != 0 ? JobStatsUtils.getInstance(simulation).timeToHours(wastedExecution / failed) : "0";
        System.out.println(wasteDivFailedInHours);

        System.out.println("*****************************");
        System.out.print("Total transfers: ");
        System.out.println(JobStatsUtils.getInstance(simulation).cantJobTransfers());
        System.out.print("Total stealings: ");
        System.out.println(JobStatsUtils.getInstance(simulation).getTotalStealing());
        System.out.print("Total transfer time: ");
        System.out.println(JobStatsUtils.getInstance(simulation).getTotalTransferTime());
        System.out.println(JobStatsUtils.getInstance(simulation).timeToHours(JobStatsUtils.getInstance(simulation).getTotalTransferTime()));
        long jobsAverageTransferTime = JobStatsUtils.getInstance(simulation).getSize() == 0 ? 0 : JobStatsUtils.getInstance(simulation).getTotalTransferTime() / JobStatsUtils.getInstance(simulation).getSize();
        System.out.println("Total transfer time per job: " + jobsAverageTransferTime);
        System.out.println(JobStatsUtils.getInstance(simulation).timeToHours(jobsAverageTransferTime));
        System.out.println("*****************************");
        System.out.print("Total results transfers: ");
        System.out.println(JobStatsUtils.getInstance(simulation).cantJobResultTransfers());
        System.out.print("Total result transfer time: ");
        System.out.println(JobStatsUtils.getInstance(simulation).getTotalResultsTransferTime());
        System.out.println(JobStatsUtils.getInstance(simulation).timeToHours(JobStatsUtils.getInstance(simulation).getTotalResultsTransferTime()));
        System.out.print("Total result transfer time per job: ");
        if (execJobs > 0) {
            System.out.println(JobStatsUtils.getInstance(simulation).getTotalResultsTransferTime() / JobStatsUtils.getInstance(simulation).getSuccessfullyExecutedJobs());
            System.out.println(JobStatsUtils.getInstance(simulation).timeToHours(JobStatsUtils.getInstance(simulation).getTotalResultsTransferTime() / JobStatsUtils.getInstance(simulation).getSuccessfullyExecutedJobs()));
        } else {
            System.out.println(execJobs);
            System.out.println(JobStatsUtils.getInstance(simulation).timeToHours(execJobs));
        }

        System.out.println("*****************************");
        System.out.println("Net stats summary");
        System.out.println("-------------------");
        System.out.println("Total Percentage of energy consumed in data transmisions: " + JobStatsUtils.getInstance(simulation).getPercentageOfEnergyInDataTransmission());
        System.out.println("Percentage sending:" + JobStatsUtils.getInstance(simulation).getPercentageOfEnergyInSendingData());
        System.out.println("Percentage receiving:" + JobStatsUtils.getInstance(simulation).getPercentageOfEnergyInReceivingData());
        System.out.print("Total update messages received by the proxy:");
        System.out.println(JobStatsUtils.getInstance(simulation).getTotalUpdateMsgReceivedByProxy());
        System.out.print("Total update messages sent by nodes:");
        System.out.println(JobStatsUtils.getInstance(simulation).getTotalUpdateMsgSentByNodes());
        System.out.print("Amount of sent data (in Gb):");
        System.out.println(JobStatsUtils.getInstance(simulation).getTotalTransferredData(true) / 1024);
        System.out.print("Amount of received data (in Gb):");
        System.out.println(JobStatsUtils.getInstance(simulation).getTotalTransferredData(false) / 1024);
        System.out.print("Total job data input (in Gb):");
        System.out.println(JobStatsUtils.getInstance(simulation).getAggregatedJobsData(true));
        System.out.print("Total job data output (in Gb):");
        System.out.println(JobStatsUtils.getInstance(simulation).getAggregatedJobsData(false));
        System.out.print("Percent of transfered data:");
        System.out.println(JobStatsUtils.getInstance(simulation).getPercentOfTransferredData());
        System.out.println("*****************************");
        JobStatsUtils.getInstance(simulation).printNodesPercentageOfEnergyWasteInNetworkActivity();
        System.out.println("Jobs states summary");
        System.out.println("-------------------");
        JobStatsUtils.getInstance(simulation).printJobStatesSummary();
        System.out.print("Percentage of completed jobs:");
        System.out.println(((((Integer) (JobStatsUtils.getInstance(simulation).getCompletedJobs() * 100)).floatValue())) / ((Integer) JobStatsUtils.getInstance(simulation).getSize()).floatValue());
        if (JobStatsUtils.getInstance(simulation).getCompletedJobsInEdge() > 0) {
            System.out.print("Percentage of completed jobs in edge:");
            System.out.println(((((Integer) (JobStatsUtils.getInstance(simulation).getCompletedJobsInEdge() * 100)).floatValue())) / ((Integer) JobStatsUtils.getInstance(simulation).getSize()).floatValue());
        }
        System.out.print("Nodes iddle Time:");
        System.out.println(JobStatsUtils.getInstance(simulation).timeToHours(JobStatsUtils.getInstance(simulation).devicesIdleTime));
        System.out.print("Total executed ops (in GIPs):");
        System.out.println(JobStatsUtils.getInstance(simulation).getTotalExecutedGIP());
        //System.out.println("*****************************");
        //System.out.println(((SimpleGASchedulerProxy)SchedulerProxy.getProxyInstance(simulation)).printGeneticRoundsInfo());

        //ValidateExperiment();
        System.out.println("Total time:");
        System.out.println(JobStatsUtils.getInstance(simulation).getTimeStampLastJob());
    }

    private static void setPersisters(Simulation simulation) {
        IPersisterFactory persistenceFactory = new MybatisPersisterFactory();
        DeviceReader.setPersisterFactory(persistenceFactory);
        JobStatsTuple.setIPersisterFactory(persistenceFactory);
        JobStatsUtils.getInstance(simulation).persistFactory = persistenceFactory;
        DeviceTuple.setIPersisterFactory(persistenceFactory);
        SimReader.setPersisterFactory(persistenceFactory);

    }

    private static void validateExperiment(Simulation simulation) {
        //Validate: Devices Initial energy is enough for Jobs Assignments
        java.util.HashMap<Device, Double> device_assignEnergy = new java.util.HashMap<>();
        java.util.HashMap<Device, Long> device_assignTime = new java.util.HashMap<>();
        Collection<TransferInfo<?>> transfers = SchedulerProxy.getProxyInstance(simulation).getTransfersCompleted();
        transfers.addAll(SchedulerProxy.getProxyInstance(simulation).getTransfersPending());

        boolean valid_assigment = true;
        for (TransferInfo<?> tInfo : transfers) {
            // FIXME: unsafe
            Device device = (Device) tInfo.getDestination();
            Job job = (Job) tInfo.getData();

            double energy = device_assignEnergy.containsKey(device) ? device_assignEnergy.get(device) : 0;
            int inputData = ((Job) tInfo.getData()).getInputSize();
            int outputData = ((Job) tInfo.getData()).getOutputSize();
            energy += device.getEnergyWasteInTransferringData(job.getInputSize());
            energy += device.getEnergyWasteInTransferringData(job.getOutputSize());
            device_assignEnergy.put(device, energy);

            long time = device_assignTime.containsKey(device) ? device_assignTime.get(device) : 0;

            int data = job.getInputSize();
            long subMessagesCount = (long) Math.ceil(inputData / (double) Device.MESSAGES_BUFFER_SIZE);
            long lastMessageSize = (long) inputData - (subMessagesCount - 1) * Device.MESSAGES_BUFFER_SIZE;
            time += (subMessagesCount - 1) * NetworkModel.getModel(simulation).getTransmissionTime(SchedulerProxy.getProxyInstance(simulation), device, inputData, Device.MESSAGES_BUFFER_SIZE);
            time += NetworkModel.getModel(simulation).getTransmissionTime(SchedulerProxy.getProxyInstance(simulation), device, inputData, (int) lastMessageSize);

            data = job.getOutputSize();
            subMessagesCount = (long) Math.ceil(outputData / (double) Device.MESSAGES_BUFFER_SIZE);
            lastMessageSize = (long) outputData - (subMessagesCount - 1) * Device.MESSAGES_BUFFER_SIZE;
            time += (subMessagesCount - 1) * NetworkModel.getModel(simulation).getTransmissionTime(device, SchedulerProxy.getProxyInstance(simulation), outputData, Device.MESSAGES_BUFFER_SIZE);
            time += NetworkModel.getModel(simulation).getTransmissionTime(device, SchedulerProxy.getProxyInstance(simulation), outputData, (int) lastMessageSize);

            device_assignTime.put(device, time);

            if (device.getInitialJoules() < energy) {
                valid_assigment = false;
            }
        }

        //Validate: Devices AccEnergyInTransfering corresponds to assignments expected energy waste
        boolean valid_energy_simulation = true;
        double[] energyDiffs = new double[100];

        //Validate: Devices, time between first input and last output correspond with sum of expected times of all msg
        boolean valid_time_simulation = true;
        double[] timeDiffs = new double[100];

        int i = 0;
        for (Device device : device_assignEnergy.keySet()) {
            double assign_energy = device_assignEnergy.get(device);
            double waste_energy = device.getAccEnergyInTransferring();
            double ack_energy = device.getFinishedJobTransfersCompleted().size() * device.getEnergyWasteInTransferringData(NetworkModel.getModel(simulation).getAckMessageSizeInBytes());
            if (Math.abs(assign_energy - waste_energy) > 0.01) {    //+ ack_energy
                valid_energy_simulation = false;
            }

            energyDiffs[i] = assign_energy - waste_energy;

            long expectedTransferingTime = device_assignTime.get(device);

            long lastTime = ((SimpleNetworkModel) NetworkModel.getModel(simulation)).lastTransferringTimes.get(device);
            long firstTime = ((SimpleNetworkModel) NetworkModel.getModel(simulation)).firstTransferringTimes.get(device);
            long transferingTime = lastTime - firstTime;

            if (Math.abs(expectedTransferingTime - transferingTime) > 0.01) {
                valid_time_simulation = false;
            }

            timeDiffs[i] = (expectedTransferingTime - transferingTime) / 1000.0;
            i++;
        }

        System.out.println("---------------------------Validations-------------------------");
        System.out.println((valid_assigment ? "Valid" : "Invalid") + " jobs assigments based on initial devices energy");
        System.out.println((valid_energy_simulation ? "Valid" : "Invalid") + " simulation energy discount based on assigments");
        System.out.println((valid_time_simulation ? "Valid" : "Invalid") + " simulation time transfers based on assigments");

    }
}
