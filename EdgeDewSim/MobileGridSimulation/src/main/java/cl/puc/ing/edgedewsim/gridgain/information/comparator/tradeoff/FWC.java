package cl.puc.ing.edgedewsim.gridgain.information.comparator.tradeoff;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.JobStats;
import cl.puc.ing.edgedewsim.mobilegrid.jobs.JobStatsUtils;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.mobilegrid.node.SchedulerProxy;
import cl.puc.ing.edgedewsim.seas.proxy.DeviceComparator;

/**
 * The Future Work-aware Criterion (FWC) considers that the future computational power of a node
 * could be estimated by analyzing the computational power the node presented in the past. In
 * other words, it assumes that the throughput level achieved by a node in the past could be
 * maintained in the future as well.
 */
public class FWC extends DeviceComparator {

    @Override
    public double getValue(Device device) {
        double avgJob_N1 = getJobAvgTime(device);
        if (avgJob_N1 < Math.E) {
            avgJob_N1 = Math.E;
        }
        double nJobs = SchedulerProxy.getProxyInstance(device.getSimulation()).getIncomingJobs(device) + device.getNumberOfJobs() + 1;
        return SchedulerProxy.getProxyInstance(device.getSimulation()).getLastReportedSOC(device) / Math.log(avgJob_N1) / nJobs;
    }

    private double getJobAvgTime(Device device) {
        double result = 0;
        int cant = 0;
        double resutlNFinished = 0;
        int cantNFinished = 0;

        for (JobStats jobStats : JobStatsUtils.getInstance(device.getSimulation()).getJobStatsExecutedIn(device)) {
            //Si es igual puedo ver si termino
            double time = jobStats.getTotalExecutionTime();
            if (time > 0) {
                //si termino sumo al resultado y uno a la cantidad
                result += time;
                cant++;
            } else {//FIXME: tiny jobs may present a total execution time equals to zero. This case is being treat as the job do not finished
                time = jobStats.getStartExecutionTime();
                resutlNFinished += (device.getSimulation().getTime() - time);
                cantNFinished++;
            }

        }
        //si tengo trabajos terminados promedio eso, sino calculo
        //con el estimador del doble del promedio de tiempo de ejecución
        if (cant > 0) {
            result /= cant;
        } else if (cantNFinished > 0) {
            result = resutlNFinished / cantNFinished * 2;
        }
        //Retorno el resultado en segundos
        return result / 1000;
    }

}
