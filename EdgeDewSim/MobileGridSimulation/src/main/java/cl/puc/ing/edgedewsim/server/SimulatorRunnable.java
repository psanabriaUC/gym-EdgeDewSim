package cl.puc.ing.edgedewsim.server;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.JobStatsUtils;
import cl.puc.ing.edgedewsim.mobilegrid.network.NetworkModel;
import cl.puc.ing.edgedewsim.mobilegrid.network.WifiLink;
import cl.puc.ing.edgedewsim.mobilegrid.node.CloudNode;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.mobilegrid.node.Node;
import cl.puc.ing.edgedewsim.mobilegrid.node.SchedulerProxy;
import cl.puc.ing.edgedewsim.mobilegrid.persistence.IPersisterFactory;
import cl.puc.ing.edgedewsim.mobilegrid.persistence.dbentity.DeviceTuple;
import cl.puc.ing.edgedewsim.mobilegrid.persistence.dbentity.JobStatsTuple;
import cl.puc.ing.edgedewsim.persistence.mybatis.MybatisPersisterFactory;
import cl.puc.ing.edgedewsim.seas.reader.DeviceReader;
import cl.puc.ing.edgedewsim.seas.reader.SimReader;
import cl.puc.ing.edgedewsim.server.data.StatisticsData;
import cl.puc.ing.edgedewsim.simulator.Logger;
import cl.puc.ing.edgedewsim.simulator.Simulation;

import java.io.*;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class SimulatorRunnable implements Runnable {
    private final Socket client;

    public SimulatorRunnable(Socket client) {
        this.client = client;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void run() {
        Simulation simulation = new Simulation();
        DataInputStream input;
        DataOutputStream output;

        try {
            System.out.printf("Connection started with %s\n", client.getInetAddress());
            input = new DataInputStream(new BufferedInputStream(client.getInputStream(), Simulation.BUFFER_SIZE));
            output = new DataOutputStream(new BufferedOutputStream(client.getOutputStream(), Simulation.BUFFER_SIZE));

            System.out.println("Obtained input");
            simulation.setInput(input);
            simulation.setOutput(output);
            int size = input.readInt();
            byte[] fileBytes = new byte[size];
            int read = input.read(fileBytes);
            String file = new String(fileBytes, 0, read);

            System.out.println("File to use: " + file);
            int command;
            boolean printLogs = false;

            SimReader simReader = new SimReader(simulation);

            setPersist(simulation);
            System.out.printf("Simulation %s started\n", simulation.getId().toString());

            do {
                simulation.resetEvents();
                simReader.read(file, false);
                JobStatsUtils.getInstance(simulation).reset();
                JobStatsUtils.getInstance(simulation).setSim_id(SimReader.getSim_id());

                String cnfPath = file;
                String[] cnfPathArr = cnfPath.split("/");
                cnfPath = cnfPathArr[cnfPathArr.length - 1];
                cnfPathArr = cnfPath.split("-");
                cnfPath = cnfPathArr[0];
                Logger.getInstance(simulation).ENABLE = printLogs;
                Logger.getInstance(simulation).EXPERIMENT = cnfPath;
                Logger.getInstance(simulation).VERBOSE = false;
                Logger.getInstance(simulation).setOutput(client.getOutputStream());

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

                output.writeLong(simulation.getId().getLeastSignificantBits());
                output.writeLong(simulation.getId().getMostSignificantBits());

                simulation.runSimulation();

//                if (printLogs)
//                    printResults(simulation, output);
                output.writeInt(Simulation.END_COMMAND);
                StatisticsData statisticsData = new StatisticsData();

                statisticsData.setJobs(JobStatsUtils.getInstance(simulation).getSize());
                statisticsData.setCompletedJobs(JobStatsUtils.getInstance(simulation).getCompletedJobs());
                statisticsData.setTime(JobStatsUtils.getInstance(simulation).getTimeStampLastJob());
                statisticsData.setCompletedOps(JobStatsUtils.getInstance(simulation).getCompletedJobsOps());
                statisticsData.setTotalSuccessfulOps(JobStatsUtils.getInstance(simulation).getTotalSuccessfulOPS());
                statisticsData.printData(output);
                output.flush();
                command = input.readInt();
                simulation.fullReset();
                NetworkModel.resetModel(simulation);
                CloudNode.resetInstance(simulation);
                SchedulerProxy.resetProxy(simulation);
                System.out.printf("Iteration for simulation %s ended\n", simulation.getId().toString());
                if (command == Simulation.RESET_COMMAND)
                    System.out.println("Hai! Reset received, beginning again");
            } while (command == Simulation.RESET_COMMAND);
            System.out.printf("Simulation %s ended\n", simulation.getId().toString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                JobStatsUtils.getInstance(simulation).reset();
                JobStatsUtils.removeInstance(simulation);
                NetworkModel.removeModel(simulation);
                CloudNode.removeInstance(simulation);
                SchedulerProxy.removeProxy(simulation);
                Logger.removeInstance(simulation);
                simulation.close();
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void printResults(Simulation simulation, PrintStream output) {
        JobStatsUtils.getInstance(simulation).printNodeInformationSummaryByNodeMips(output);
        output.print("Total simulated time: ");
        output.println(simulation.getTime());
        output.println(simulation.getTime());
        output.print("Jobs simulated: ");
        output.println(JobStatsUtils.getInstance(simulation).getSize());
        output.print("Jobs successfully executed: ");
        output.println(JobStatsUtils.getInstance(simulation).getSuccessfullyExecutedJobs());
        output.print("Last successful task time: ");
        output.println(JobStatsUtils.getInstance(simulation).getTimeStampLastJob());
        output.println(JobStatsUtils.getInstance(simulation).timeToHours(JobStatsUtils.getInstance(simulation).getTimeStampLastJob()));
        output.print("Successfully execution time: ");
        output.println(JobStatsUtils.getInstance(simulation).getEffectiveExecutionTime());
        output.println(JobStatsUtils.getInstance(simulation).timeToHours(JobStatsUtils.getInstance(simulation).getEffectiveExecutionTime()));
        output.print("Successfully execution time per effective job: ");
        int execJobs = JobStatsUtils.getInstance(simulation).getSuccessfullyExecutedJobs();
        if (execJobs > 0) {
            output.println(JobStatsUtils.getInstance(simulation).getEffectiveExecutionTime() / execJobs);
            output.println(JobStatsUtils.getInstance(simulation).timeToHours(JobStatsUtils.getInstance(simulation).getEffectiveExecutionTime() / execJobs));
        } else {
            output.println(JobStatsUtils.getInstance(simulation).timeToHours(execJobs));
            output.println(JobStatsUtils.getInstance(simulation).timeToHours(execJobs));
        }


        output.print("Executed job waiting time: ");
        output.println(JobStatsUtils.getInstance(simulation).getEffectiveQueueTime());
        output.println(JobStatsUtils.getInstance(simulation).timeToHours(JobStatsUtils.getInstance(simulation).getEffectiveQueueTime()));
        output.print("Executed job waiting time per effective: ");
        if (execJobs > 0) {
            output.println(JobStatsUtils.getInstance(simulation).getEffectiveQueueTime() / JobStatsUtils.getInstance(simulation).getSuccessfullyExecutedJobs());
            output.println(JobStatsUtils.getInstance(simulation).timeToHours(JobStatsUtils.getInstance(simulation).getEffectiveQueueTime() / JobStatsUtils.getInstance(simulation).getSuccessfullyExecutedJobs()));
        } else {
            output.println(execJobs);
            output.println(JobStatsUtils.getInstance(simulation).timeToHours(execJobs));
        }

        output.println("*****************************");
        output.print("Total queue time: ");
        output.println(JobStatsUtils.getInstance(simulation).getTotalQueueTime());
        output.println(JobStatsUtils.getInstance(simulation).timeToHours(JobStatsUtils.getInstance(simulation).getTotalQueueTime()));
        long avgQueueTimePerJob = JobStatsUtils.getInstance(simulation).getSize() == 0 ? 0 : JobStatsUtils.getInstance(simulation).getTotalQueueTime() / JobStatsUtils.getInstance(simulation).getSize();
        output.println("Average queue time per job: " + avgQueueTimePerJob);

        output.println(JobStatsUtils.getInstance(simulation).timeToHours(avgQueueTimePerJob));
        output.print("Total execution time: ");
        output.println(JobStatsUtils.getInstance(simulation).getTotalExecutionTime());
        output.println(JobStatsUtils.getInstance(simulation).timeToHours(JobStatsUtils.getInstance(simulation).getTotalExecutionTime()));

        long avgExecTimePerJob = JobStatsUtils.getInstance(simulation).getSize() == 0 ? 0 : JobStatsUtils.getInstance(simulation).getTotalExecutionTime() / JobStatsUtils.getInstance(simulation).getSize();
        output.println("Average execution time per job: " + avgExecTimePerJob);
        output.println(JobStatsUtils.getInstance(simulation).timeToHours(avgExecTimePerJob));

        output.println("*****************************");
        int failed = JobStatsUtils.getInstance(simulation).getSize() - JobStatsUtils.getInstance(simulation).getSuccessfullyExecutedJobs();
        long wastedWaited = JobStatsUtils.getInstance(simulation).getTotalQueueTime() - JobStatsUtils.getInstance(simulation).getEffectiveQueueTime();
        long wastedExecution = JobStatsUtils.getInstance(simulation).getTotalExecutionTime() - JobStatsUtils.getInstance(simulation).getEffectiveExecutionTime();
        output.print("Failed jobs: ");
        output.println(failed);
        output.print("Wasted queue time: ");
        output.println(wastedWaited);
        output.println(JobStatsUtils.getInstance(simulation).timeToHours(wastedWaited));
        output.print("Average wasted queue time per failed job: ");
        long wastedDivFailed = failed != 0 ? wastedWaited / failed : 0;
        output.println(wastedDivFailed);
        String wasteDivFailedInHours = failed != 0 ? JobStatsUtils.getInstance(simulation).timeToHours(wastedWaited / failed) : "0";
        output.println(wasteDivFailedInHours);
        output.print("Wasted execution time: ");
        output.println(wastedExecution);
        output.println(JobStatsUtils.getInstance(simulation).timeToHours(wastedExecution));
        output.print("Average wasted execution time per failed job: ");
        wastedDivFailed = failed != 0 ? wastedExecution / failed : 0;
        output.println(wastedDivFailed);
        wasteDivFailedInHours = failed != 0 ? JobStatsUtils.getInstance(simulation).timeToHours(wastedExecution / failed) : "0";
        output.println(wasteDivFailedInHours);

        output.println("*****************************");
        output.print("Total transfers: ");
        output.println(JobStatsUtils.getInstance(simulation).cantJobTransfers());
        output.print("Total stealings: ");
        output.println(JobStatsUtils.getInstance(simulation).getTotalStealing());
        output.print("Total transfer time: ");
        output.println(JobStatsUtils.getInstance(simulation).getTotalTransferTime());
        output.println(JobStatsUtils.getInstance(simulation).timeToHours(JobStatsUtils.getInstance(simulation).getTotalTransferTime()));
        long jobsAverageTransferTime = JobStatsUtils.getInstance(simulation).getSize() == 0 ? 0 : JobStatsUtils.getInstance(simulation).getTotalTransferTime() / JobStatsUtils.getInstance(simulation).getSize();
        output.println("Total transfer time per job: " + jobsAverageTransferTime);
        output.println(JobStatsUtils.getInstance(simulation).timeToHours(jobsAverageTransferTime));
        output.println("*****************************");
        output.print("Total results transfers: ");
        output.println(JobStatsUtils.getInstance(simulation).cantJobResultTransfers());
        output.print("Total result transfer time: ");
        output.println(JobStatsUtils.getInstance(simulation).getTotalResultsTransferTime());
        output.println(JobStatsUtils.getInstance(simulation).timeToHours(JobStatsUtils.getInstance(simulation).getTotalResultsTransferTime()));
        output.print("Total result transfer time per job: ");
        if (execJobs > 0) {
            output.println(JobStatsUtils.getInstance(simulation).getTotalResultsTransferTime() / JobStatsUtils.getInstance(simulation).getSuccessfullyExecutedJobs());
            output.println(JobStatsUtils.getInstance(simulation).timeToHours(JobStatsUtils.getInstance(simulation).getTotalResultsTransferTime() / JobStatsUtils.getInstance(simulation).getSuccessfullyExecutedJobs()));
        } else {
            output.println(execJobs);
            output.println(JobStatsUtils.getInstance(simulation).timeToHours(execJobs));
        }

        output.println("*****************************");
        output.println("Net stats summary");
        output.println("-------------------");
        output.println("Total Percentage of energy consumed in data transmisions: " + JobStatsUtils.getInstance(simulation).getPercentageOfEnergyInDataTransmission());
        output.println("Percentage sending:" + JobStatsUtils.getInstance(simulation).getPercentageOfEnergyInSendingData());
        output.println("Percentage receiving:" + JobStatsUtils.getInstance(simulation).getPercentageOfEnergyInReceivingData());
        output.print("Total update messages received by the proxy:");
        output.println(JobStatsUtils.getInstance(simulation).getTotalUpdateMsgReceivedByProxy());
        output.print("Total update messages sent by nodes:");
        output.println(JobStatsUtils.getInstance(simulation).getTotalUpdateMsgSentByNodes());
        output.print("Amount of sent data (in Gb):");
        output.println(JobStatsUtils.getInstance(simulation).getTotalTransferredData(true) / 1024);
        output.print("Amount of received data (in Gb):");
        output.println(JobStatsUtils.getInstance(simulation).getTotalTransferredData(false) / 1024);
        output.print("Total job data input (in Gb):");
        output.println(JobStatsUtils.getInstance(simulation).getAggregatedJobsData(true));
        output.print("Total job data output (in Gb):");
        output.println(JobStatsUtils.getInstance(simulation).getAggregatedJobsData(false));
        output.print("Percent of transfered data:");
        output.println(JobStatsUtils.getInstance(simulation).getPercentOfTransferredData());
        output.println("*****************************");
        JobStatsUtils.getInstance(simulation).printNodesPercentageOfEnergyWasteInNetworkActivity(output);
        output.println("Jobs states summary");
        output.println("-------------------");
        JobStatsUtils.getInstance(simulation).printJobStatesSummary(output);
        output.print("Percentage of completed jobs:");
        output.println(((((Integer) (JobStatsUtils.getInstance(simulation).getCompletedJobs() * 100)).floatValue())) / ((Integer) JobStatsUtils.getInstance(simulation).getSize()).floatValue());
        if (JobStatsUtils.getInstance(simulation).getCompletedJobsInEdge() > 0) {
            output.print("Percentage of completed jobs in edge:");
            output.println(((((Integer) (JobStatsUtils.getInstance(simulation).getCompletedJobsInEdge() * 100)).floatValue())) / ((Integer) JobStatsUtils.getInstance(simulation).getSize()).floatValue());
        }
        output.print("Nodes idle Time:");
        output.println(JobStatsUtils.getInstance(simulation).timeToHours(JobStatsUtils.getInstance(simulation).devicesIdleTime));
        output.print("Total executed ops (in GIPs):");
        output.println(JobStatsUtils.getInstance(simulation).getTotalExecutedGIP());
    }

    private void setPersist(Simulation simulation) {
        IPersisterFactory persistenceFactory = new MybatisPersisterFactory();
        DeviceReader.setPersisterFactory(persistenceFactory);
        JobStatsTuple.setIPersisterFactory(persistenceFactory);
        JobStatsUtils.getInstance(simulation).persistFactory = persistenceFactory;
        DeviceTuple.setIPersisterFactory(persistenceFactory);
        SimReader.setPersisterFactory(persistenceFactory);
    }
}
