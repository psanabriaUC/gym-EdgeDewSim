package cl.puc.ing.edgedewsim.seas.node.jobstealing;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;
import cl.puc.ing.edgedewsim.mobilegrid.node.SchedulerProxy;
import cl.puc.ing.edgedewsim.seas.node.DefaultExecutionManager;
import cl.puc.ing.edgedewsim.seas.proxy.jobstealing.StealerProxy;

public class JSSEASExecutionManager extends DefaultExecutionManager {

    @Override
    public void onFinishJob(Job job) {
        super.onFinishJob(job);
        if (this.getQueuedJobs() == 0 && !isExecuting()) {
            ((StealerProxy) SchedulerProxy.getProxyInstance(this.getDevice().getSimulation())).steal(this.getDevice());
        }
    }

}
