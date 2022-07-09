package cl.puc.ing.edgedewsim.seas.proxy.jobstealing;

import cl.puc.ing.edgedewsim.gridgain.spi.loadbalacing.energyaware.GridEnergyAwareLoadBalancing;
import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;
import cl.puc.ing.edgedewsim.mobilegrid.network.Message;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.seas.node.jobstealing.JSMessage;
import cl.puc.ing.edgedewsim.seas.proxy.jobstealing.condition.NoJobsCondition;
import cl.puc.ing.edgedewsim.seas.proxy.jobstealing.condition.StealingCondition;
import cl.puc.ing.edgedewsim.simulator.Logger;
import cl.puc.ing.edgedewsim.simulator.Simulation;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scheduler that implements a job stealing policy.
 * <p>
 * TODO: add network energy consumption emulation support
 */
public class StealerProxy extends GridEnergyAwareLoadBalancing {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(0);

    private StealingPolicy policy;
    private StealingStrategy strategy;
    private StealingCondition condition = new NoJobsCondition();

    public StealerProxy(String name, Simulation simulation) {
        super(name, simulation);
    }

    public void steal(Device stealer) {
        Device victim = this.strategy.getVictim(this, stealer);
        if (victim == null || !this.condition.canSteal(stealer, victim, this))
            return; //TODO: ANALIZAR SI ESTA LINEA INVOLUCRA CONSUMO DE RED O EL CHEQUEO SE REALIZA CON LAS ESTRUCTURAS INTERNAS DE LOS NODOS

        int numberOfJobsToSteal = this.policy.jobsToSteal(stealer, victim);
        Logger.getInstance(simulation).logEntity(stealer, "The device stole " + numberOfJobsToSteal + " jobs from " + victim);

        // The proxy sends a steal request to the victim node
        JSMessage jsMessage = new JSMessage(JSMessage.STEAL_REQUEST_TYPE);
        queueMessageTransfer(victim, jsMessage, Message.STEAL_MSG_SIZE);

        for (int i = 0; i < numberOfJobsToSteal && victim.isOnline() && stealer.isOnline(); i++) {
            Job job = victim.removeJob(0);
            Logger.getInstance(simulation).logEntity(victim, "Sending stolen job (id) to stealer (st)", job, stealer);

            queueJobTransferring(stealer, job);
            onJobStolen(stealer, victim, job);

            /*
			// long time = NetworkModel.getModel().send(victim, stealer, NEXT_ID.incrementAndGet(), job.getInputSize(), job);

			if (time != 0) { //means that there was a connection problem between the victim and the stealer
				long currentSimTime = Simulation.getTime();			
				JobStatsUtils.transfer(job, stealer, time-currentSimTime,currentSimTime);
			} else {
                Logger.logEntity(victim, "Fail to send job (id) to stealer (st): broken link.", job, stealer);
            }
            */
        }
    }

    protected void onJobStolen(Device stealer, Device victim, Job job) {
    }

    public StealingPolicy getPolicy() {
        return this.policy;
    }

    public void setPolicy(StealingPolicy policy) {
        this.policy = policy;
        Logger.getInstance(simulation).logEntity(this, "Using StealingPolicy", policy.getClass().getName());
    }

    public StealingStrategy getStrategy() {
        return this.strategy;
    }

    public void setStrategy(StealingStrategy strategy) {
        this.strategy = strategy;
        Logger.getInstance(simulation).logEntity(this, "Using StealingStrategy", strategy.getClass().getName());
    }

    public void setCondition(StealingCondition pol) {
        this.condition = pol;
        Logger.getInstance(simulation).logEntity(this, "Using StealingCondition", pol.getClass().getName());
    }

}
