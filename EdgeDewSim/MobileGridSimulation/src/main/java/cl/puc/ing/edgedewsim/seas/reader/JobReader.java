package cl.puc.ing.edgedewsim.seas.reader;

import cl.puc.ing.edgedewsim.mobilegrid.jobs.Job;
import cl.puc.ing.edgedewsim.mobilegrid.node.SchedulerProxy;
import cl.puc.ing.edgedewsim.simulator.Event;
import cl.puc.ing.edgedewsim.simulator.Simulation;

import java.io.BufferedReader;
import java.util.StringTokenizer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Helper class for reading and parsing a list of {@link Job}s contained in a configuration file. Examples input files can be found in
 * sim_input/jobs/*.
 * <p>
 * This class provides its own Runnable implementation so it can be run in parallel.
 */
public class JobReader extends Thread {

    private final ReentrantLock simLock;
    private final BufferedReader conf;
    private final Simulation simulation;
    private final boolean networkMeasurementEnable;

    public JobReader(ReentrantLock simLock, BufferedReader conf, Simulation simulation, boolean networkEnableFlag) {
        super();
        this.simLock = simLock;
        this.conf = conf;
        this.networkMeasurementEnable = networkEnableFlag;
        this.simulation = simulation;
    }

    /**
     * Jobs FileFormat
     * ops;time;inputSize;outputSize*
     */
    @Override
    public void run() {
        try {
            this.simLock.lock();
            int schedulerProxyId = simulation.getEntity("PROXY").getId();
            this.simLock.unlock();

            String line = this.conf.readLine();
            boolean lackingJobParameter = false;

            // Expected format for each line in the configuration file:
            // [jobId];[# of CPU cycles required to complete];[arrival time](;[input size];[output size])
            while (line != null) {
                line = line.trim();
                if (!(line.equals("") || line.startsWith("#"))) {
                    Event event;
                    if (line.contains("END_JOB_BURST")) {
                        StringTokenizer ts = new StringTokenizer(line, ";");
                        //arrival time +1 is to assure the event location within the events queue is placed just after the last arrived job of the burst
                        long arrivalTime = Long.parseLong(ts.nextToken());
                        event = Event.createEvent(Event.NO_SOURCE, arrivalTime, schedulerProxyId, SchedulerProxy.EVENT_END_JOB_ARRIVAL_BURST, null);
                    } else {//means that the line is a job descriptor
                        StringTokenizer ts = new StringTokenizer(line, ";");
                        // Currently the user provided job id (defined in the configuration file) is ignored for internal job identification. Instead it is automatically assigned by the simulation engine.
                        int userProvidedJobId = Integer.parseInt(ts.nextToken());
                        long ops = Long.parseLong(ts.nextToken());
                        long arrivalTime = Long.parseLong(ts.nextToken());

                        int inputSize = 0;
                        int outputSize = 0;
                        if (ts.hasMoreTokens()) {
                            inputSize = Integer.parseInt(ts.nextToken());
                            outputSize = Integer.parseInt(ts.nextToken());
                        } else if (networkMeasurementEnable) {
                            lackingJobParameter = true;
                        }

                        // For each job, we create an encompassing event and send it to the scheduling proxy chosen for this simulation.
                        // The scheduling proxy will then re-send the events for processing to the appropriate devices according to its policy.
                        Job job = new Job(userProvidedJobId, ops, schedulerProxyId, inputSize, outputSize, simulation);
                        event = Event.createEvent(Event.NO_SOURCE, arrivalTime, schedulerProxyId, SchedulerProxy.EVENT_JOB_ARRIVE, job);
                    }
                    this.simLock.lock();
                    simulation.addEvent(event);
                    this.simLock.unlock();
                }

                line = this.conf.readLine();
            }
            if (lackingJobParameter) {
                System.out.println("[WARN] At least one job has no input size or output size defined");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
