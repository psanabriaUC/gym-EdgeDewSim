package cl.puc.ing.edgedewsim.seas.proxy;


import cl.puc.ing.edgedewsim.mobilegrid.node.SchedulerProxy;

public class RemainingDataTransferringEvaluator implements
        DataAssignmentEvaluatorIF {

    @Override
    public double eval(DataAssignment da) {
        double remainingEnergy = SchedulerProxy.getProxyInstance(da.getDevice().getSimulation()).getJoulesBasedOnLastReportedSOC(da.getDevice());
        for (int job_index = 0; job_index < da.getAssignedJobs().size(); job_index++) {
            remainingEnergy -= da.getDevice().getEnergyWasteInTransferringData(da.getAssignedJobs().get(job_index).getInputSize());
            remainingEnergy -= da.getDevice().getEnergyWasteInTransferringData(da.getAssignedJobs().get(job_index).getOutputSize());
        }

        return remainingEnergy;
    }

}
