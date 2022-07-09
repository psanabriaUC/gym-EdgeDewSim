package cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.seas.proxy.DataAssignment;
import cl.puc.ing.edgedewsim.seas.proxy.RSSIDataEvaluator;
import cl.puc.ing.edgedewsim.simulator.Simulation;

import java.util.HashMap;

public class HungarianBasedProxy extends BufferedSchedulerProxy {

    private final boolean usePreviousAssignmentsInformation = true;

    public HungarianBasedProxy(String name, Simulation simulation) {
        super(name, "1", simulation);
        DataAssignment.evaluator = new RSSIDataEvaluator();
        bufferSize = devices.size();
    }


    @Override
    protected void queueJob(Job job) {
        bufferedJobs.add(job);
    }

    @Override
    protected void assignBufferedJobs() {
        // build the cost matrix
        // Columns of the costsMatrix represent the nodes and Rows represent the jobs.
        // Each cell contains the cost (in energy percentage) of transfer the
        // (input+output) data of the job to a node. In a proxy-based infrastructure
        // it is assumed that the proxy sees all the nodes, so, all cell should be
        // filled with a value different from infinito. Infinito values will be used
        // to represent unreachable nodes.
        HashMap<Integer, Device> mapOfDevices = new HashMap<Integer, Device>();
        double[][] costsMatrix = new double[devices.size()][devices.size()];
        //fill the matrix with the energetic cost of sending each job to each device

        double costij;

        for (int jobIndex = 0; jobIndex < bufferedJobs.size(); jobIndex++) {
            Job job = bufferedJobs.get(jobIndex);
            int deviceIndex = 0;
            for (Device device : devices.values()) {
                DataAssignment deviceCurrentAssignment = deviceToAssignmentsMap.get(device);

                if (deviceCurrentAssignment == null) {
                    deviceCurrentAssignment = new DataAssignment(null);
                    deviceCurrentAssignment.setDevice(device);
                }
                DataAssignment hypotheticalDeviceAssignment = deviceCurrentAssignment.clone();
                double inputData = (double) (job.getInputSize()) / 1024 / 1024;
                double outputData = (double) (job.getOutputSize()) / 1024 / 1024;
                if (usePreviousAssignmentsInformation) {
                    inputData += hypotheticalDeviceAssignment.getMbToBeReceived();
                    outputData += hypotheticalDeviceAssignment.getMbToBeSend();
                }
                hypotheticalDeviceAssignment.setMbToBeReceived(inputData);
                hypotheticalDeviceAssignment.setMbToBeSend(outputData);
                costij = DataAssignment.evaluator.eval(hypotheticalDeviceAssignment);
                costsMatrix[jobIndex][deviceIndex] = costij;
                mapOfDevices.put(deviceIndex, device);
                deviceIndex++;
            }
        }

        //invoke Hungarian algorithm
        int[][] assignments = HungarianAlgorithm.hgAlgorithm(costsMatrix, "min");

        /////////////////////////////////////////////////////////////////////////
        //map jobs to nodes
        for (int i = 0; i < assignments.length; i++) {
            queueJobTransferring(mapOfDevices.get(i), bufferedJobs.get(assignments[i][1]));
        }
    }

}
