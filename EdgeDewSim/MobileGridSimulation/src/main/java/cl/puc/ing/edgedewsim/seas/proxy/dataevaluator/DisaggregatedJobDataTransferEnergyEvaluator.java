package cl.puc.ing.edgedewsim.seas.proxy.dataevaluator;

import cl.puc.ing.edgedewsim.mobilegrid.node.SchedulerProxy;
import cl.puc.ing.edgedewsim.seas.node.DefaultFiniteBatteryManager;
import cl.puc.ing.edgedewsim.seas.proxy.DataAssignment;
import cl.puc.ing.edgedewsim.seas.proxy.DataAssignmentEvaluatorIF;

public class DisaggregatedJobDataTransferEnergyEvaluator implements
        DataAssignmentEvaluatorIF {

    private int maxAvailable = 100;

    /**
     * maxAvailable is the maximum battery percentage that could be used for transferring data. The value should be indicated with a positive integer
     * within the range of [0-100]
     */
    public DisaggregatedJobDataTransferEnergyEvaluator(int maxAvailable) {
        this.maxAvailable = maxAvailable >= 0 && maxAvailable <= 100 ? maxAvailable : 100;
    }

    @Override
    public double eval(DataAssignment da) {
        //double costPerKb = getCostInJoulesPerKbOfData(da.getDevice());
        int completelyTransferedJobs = 0;
        double totalDataTransfered = 0.0d;
        double energyRequired = 0;
        double energyWasted = 0;
        double missedJobs = 0.0d;
        double nodeAvailableEnergy = ((double) ((SchedulerProxy.getProxyInstance(da.getDevice().getSimulation()).getLastReportedSOC(da.getDevice()) /
                DefaultFiniteBatteryManager.PROFILE_ONE_PERCENT_REPRESENTATION) * da.getDevice().getTotalBatteryCapacityInJoules())) / (double) (100);
        double nodeMaxAvailableForTransferring = ((double) maxAvailable * nodeAvailableEnergy) / (double) 100;
        for (int job_index = 0; job_index < da.getAssignedJobs().size(); job_index++) {
            energyRequired += da.getDevice().getEnergyWasteInTransferringData(da.getAssignedJobs().get(job_index).getInputSize());
            energyRequired += da.getDevice().getEnergyWasteInTransferringData(da.getAssignedJobs().get(job_index).getOutputSize());
            if (energyRequired <= nodeMaxAvailableForTransferring) {
                completelyTransferedJobs++;
                //data transfer is indicated in  Megabytes //kilobytes
                totalDataTransfered += (da.getAssignedJobs().get(job_index).getInputSize() + da.getAssignedJobs().get(job_index).getOutputSize()) / (double) (1024 * 1024);
                energyWasted = energyRequired;
            } else
                missedJobs++;
        }
        da.setDeviceEnergyWasted(energyWasted);
        da.setAffordableDataTranfered(totalDataTransfered);
        da.setAffordableJobCompletelyTransfered(completelyTransferedJobs);

        return missedJobs;
    }

    /**return the mean amount of kilobytes of data that the device is able to transfer with its current
     * energy and wifi signal*/
	/*public double getDeviceDataCapacity(Device d){
		//TODO: return a mean value
		double nodeAvailableEnergy = ((double)(d.getLastBatteryLevelUpdate() / DefaultBatteryManager.PROFILE_ONE_PERCENT_REPRESENTATION) * d.getTotalBatteryCapacityInJoules()) / (double)100;
		return nodeAvailableEnergy / getCostInJoulesPerKbOfData(d);
	}*/

}
