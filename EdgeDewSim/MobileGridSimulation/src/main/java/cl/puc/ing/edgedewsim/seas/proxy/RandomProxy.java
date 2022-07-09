package cl.puc.ing.edgedewsim.seas.proxy;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;
import cl.puc.ing.edgedewsim.mobilegrid.jobs.JobStatsUtils;
import cl.puc.ing.edgedewsim.mobilegrid.node.Device;
import cl.puc.ing.edgedewsim.mobilegrid.node.SchedulerProxy;
import cl.puc.ing.edgedewsim.simulator.Event;
import cl.puc.ing.edgedewsim.simulator.Logger;
import cl.puc.ing.edgedewsim.simulator.Simulation;

import java.util.Random;

public class RandomProxy extends SchedulerProxy {
    private final Random random = new Random();
    protected int idSend = 0;

    public RandomProxy(String name, Simulation simulation) {
        super(name, simulation);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void processEvent(Event event) {
        if (EVENT_JOB_ARRIVE != event.getEventType()) throw new IllegalArgumentException("Unexpected event");
        Job job = (Job) event.getData();
        JobStatsUtils.getInstance(simulation).addJob(job, this);
        Logger.getInstance(simulation).logEntity(this, "Job arrived ", job.getJobId());

        String[] keys = this.devices.keySet().toArray(new String[this.devices.size()]);
        Device current = this.devices.get(keys[random.nextInt(keys.length)]);

        Logger.getInstance(simulation).logEntity(this, "Job assigned to ", job.getJobId(), current);
        queueJobTransferring(current, job);

    }

    @Override
    public boolean runsOnBattery() {
        //TODO: revisar esta respuesta
        return false;
    }

}
