package cl.puc.ing.edgedewsim.seas.proxy.dataevaluator;

import cl.puc.ing.edgedewsim.mobilegrid.node.SchedulerProxy;
import cl.puc.ing.edgedewsim.seas.proxy.DataAssignment;
import cl.puc.ing.edgedewsim.seas.proxy.DataAssignmentEvaluatorIF;

/**
 * this evaluator weights a data assignment based on how much data and jobs can be completely transfered
 * with the permitted usable energy set in the constructor. Data (input + output) and jobs that fall out of the
 * permitted usable energy penalize the amount of data and jobs that fall into that energy limit
 */
public class OverflowPenaltyDataEvaluator implements DataAssignmentEvaluatorIF {

    /**
     * maxAvailable is the maximum battery percentage that could be used for transferring data. The value should be indicated with a positive integer
     * within the range of [0-100]
     */
    private final double maxAvailPercentageEnergy;

    public OverflowPenaltyDataEvaluator(int maximumAllowed) {
        this.maxAvailPercentageEnergy = maximumAllowed >= 0 && maximumAllowed <= 100 ? maximumAllowed : 100;
    }

    @Override
    public double eval(DataAssignment da) {

        double nodeAvailableEnergy = SchedulerProxy.getProxyInstance(da.getDevice().getSimulation()).getJoulesBasedOnLastReportedSOC(da.getDevice());
        double nodeMaxAvailableEnergy = (maxAvailPercentageEnergy * nodeAvailableEnergy) / 100;
        int completelyTransferedJobs = 0;
        double totalDataTransfered = 0.0d;
        double energyWastedWhileReceiving = 0.0d;
        double energyWastedWhileSending = 0.0d;
        double totalEnergyWasted = 0.0d;
        int lastAffordableJob = 0;

        for (int job_index = 0; job_index < da.getAssignedJobs().size(); job_index++) {
            double jobInputData = da.getAssignedJobs().get(job_index).getInputSize();
            double jobOutputData = da.getAssignedJobs().get(job_index).getOutputSize();
            energyWastedWhileReceiving += da.getDevice().getEnergyWasteInTransferringData(jobInputData);
            energyWastedWhileSending += da.getDevice().getEnergyWasteInTransferringData(jobOutputData);

            if (energyWastedWhileReceiving + energyWastedWhileSending <= nodeMaxAvailableEnergy) {
                completelyTransferedJobs++;
                lastAffordableJob = job_index;
                totalDataTransfered += ((jobInputData + jobOutputData) / (double) (1024 * 1024)); //data transfer is indicated in Megabytes
                totalEnergyWasted = energyWastedWhileReceiving + energyWastedWhileSending;
            } else {
                //Assuming that all jobs data inputs are received by the node before it starts to execute jobs, and jobs are executed in order, and
                //jobs data output is sent as the node finish with each job, the following logic penalizes jobs that prevent other jobs from finished
                //due to the energy originally planned for transferring data outputs is consumed by data input of the next job in the queue that is
                //being traversed in this loop.
                if (energyWastedWhileReceiving <= nodeMaxAvailableEnergy) {
                    double overflow = (energyWastedWhileReceiving + energyWastedWhileSending - jobOutputData) - nodeAvailableEnergy;

                    while (overflow > 0 && lastAffordableJob >= 0) {
                        double lastAffordableOutput = da.getAssignedJobs().get(lastAffordableJob).getOutputSize();
                        overflow -= da.getDevice().getEnergyWasteInTransferringData(lastAffordableOutput);
                        totalDataTransfered -= ((lastAffordableOutput + (double) da.getAssignedJobs().get(lastAffordableJob).getInputSize()) / (double) (1024 * 1024));
                        lastAffordableJob--;
                    }
                    completelyTransferedJobs = lastAffordableJob + 1;
                } else {//the penalty increases while energy needed for receiving jobs increases
                    if (lastAffordableJob >= 0) {
                        lastAffordableJob = -1;
                        totalDataTransfered = 0.0d;
                        completelyTransferedJobs = 0;
                    } else {
                        totalDataTransfered -= ((jobInputData + jobOutputData) / (double) (1024 * 1024));
                        completelyTransferedJobs--;
                    }
                }
            }
        }
        da.setDeviceEnergyWasted(totalEnergyWasted);
        da.setAffordableDataTranfered(totalDataTransfered);
        da.setAffordableJobCompletelyTransfered(completelyTransferedJobs);
        return energyWastedWhileReceiving + energyWastedWhileSending;

    }

}
